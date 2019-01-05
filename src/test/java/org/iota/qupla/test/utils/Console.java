package org.iota.qupla.test.utils;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.PatternSyntaxException;

/**
 * Record all System.out and System.err output and provide convenience assertions in the messages.
 *
 * <b>Attention</b> - Don't forget to clean things up by calling {@link Console#close()} at the end
 * of each Console usage.
 */
public final class Console implements Closeable {
    private static final PrintStream ORIGINAL_STDOUT = System.out;
    private static final PrintStream ORIGINAL_STDERR = System.err;

    private Path tmpFile;
    private final AtomicReference<PrintStream> sideChannel;
    private final PrintStream reroutedStdOutPrint;
    private final PrintStream reroutedStdErrPrint;
    private final TeeOutputStream reroutedStdOut;
    private final TeeOutputStream reroutedStdErr;

    /**
     * <p>
     * Construct a new instance to collect stdout and stderr output for later analysis. stdout and stderr will be rerouted when this constructor returns. Output
     * will still be printed to console but is also recorded.
     * </p>
     * Don't forget to clean things up by calling {@link Console#close()} at the end of each Console usage.
     */
    public Console() {
        this.tmpFile = createTmpFile();
        this.sideChannel = new AtomicReference<>(createConsoleStream(tmpFile));

        reroutedStdOut = new TeeOutputStream(ORIGINAL_STDOUT, sideChannel);
        reroutedStdErr = new TeeOutputStream(ORIGINAL_STDERR, sideChannel);
        reroutedStdOutPrint = new PrintStream(reroutedStdOut);
        reroutedStdErrPrint = new PrintStream(reroutedStdErr);

        System.setOut(reroutedStdOutPrint);
        System.setErr(reroutedStdErrPrint);
    }

    private Path createTmpFile() {
        try {
            return Files.createTempFile("slm", ".console");
        } catch (Exception e) {
            throw new RuntimeException("Console initalization fail with " + e.getClass().getSimpleName() + " because - " + e.getMessage(), e);
        }
    }

    private void deleteTmpFile() {
        if (tmpFile != null) {
            try {
                Files.deleteIfExists(tmpFile);
            } catch (IOException e) {
                ORIGINAL_STDERR.println("WARN - Possible resource leak: Could not delete temporary test file '" + tmpFile.toString() + "' - "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            } finally {
                tmpFile = null;
                sideChannel.get().close();
            }
        }
    }

    /**
     * Set the System.err and System.out back to standard and release all resources. This object becomes unusable when this method returns.
     */
    public void close() {
        sideChannel.get().close();
        reroutedStdOutPrint.close();
        reroutedStdErrPrint.close();
        deleteTmpFile();
        System.setErr(ORIGINAL_STDERR);
        System.setOut(ORIGINAL_STDOUT);
        System.out.println("------------------------------------------------------------------");
    }

    /**
     * @return All data that has been logged to the console.
     */
    public String getContent() {
        flush();
        try {
            return new String(Files.readAllBytes(tmpFile));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read log file '" + tmpFile + "'", e);
        }
    }

    /**
     * @return the data file.
     */
    public Path getFile(){
        return tmpFile;
    }

    private void flush() {
        sideChannel.get().flush();
        reroutedStdOutPrint.flush();
        reroutedStdErrPrint.flush();
    }

    /**
     * Discard all recorded log messages.
     */
    public synchronized void clear() {
        flush();
        sideChannel.get().close();
        deleteTmpFile();
        tmpFile = createTmpFile();
        sideChannel.set(createConsoleStream(tmpFile));
    }

    /**
     * <p>
     * Like {@link #anyLineMatches(String)} but does not use a regex but a pure String comparison.
     * </p>
     * The line matching uses a "contains" logic, rather than an "equals" logic to make matching a bit more fuzzy.
     * </p>
     * <p>
     * <b>Example:</b>
     *
     * <pre>
     * &#64;Test
     * public void test() {
     *     System.out.println("nomatch");
     *     Assert.assertTrue(console.anyLineContains("match")); // passes
     * }
     * </pre>
     * </p>
     *
     * @param line - Match lines with this.
     * @return {@code true} if at least one line contains {@code line}. Otherwise {@code false}.
     */
    public boolean anyLineContains(String line) {
        return internalContains(line, 0) != null;
    }

    private Integer internalContains(String line, int offset) {
        List<String> contents = new ArrayList<>();
        try {
            contents = Files.readAllLines(tmpFile);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        for (int i = offset; i < contents.size(); i++) {
            String content = contents.get(i);
            if (content.contains(line)) {
                return i;
            }
        }

        return null;
    }

    /**
     * Check if any line matches the provided {@code regex}.
     *
     * @param regex - Match lines with this regular expression.
     * @return {@code true} if at least one line matches {@code regex}. Otherwise {@code false}.
     * @throws PatternSyntaxException - if the regular expression's syntax is invalid.
     */
    public boolean anyLineMatches(String regex) {
        try {
            return Files.readAllLines(tmpFile).stream().anyMatch(line -> line.matches(regex));
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * Check if any line starts with {@code prefix}.
     *
     * @param prefix - String to match line beginnings.
     * @return {@code true} if at least one line starts with {@code prefix}. Otherwise {@code false}.
     */
    public boolean anyLineStartsWith(String prefix) {
        try {
            return Files.readAllLines(tmpFile).stream().anyMatch(line -> line.startsWith(prefix));
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * Check if any line ends with {@code suffix}.
     *
     * @param suffix - String to match line endings.
     * @return {@code true} if at least one line ends with {@code suffix}. Otherwise {@code false}.
     */
    public boolean anyLineEndsWith(String suffix) {
        try {
            return Files.readAllLines(tmpFile).stream().anyMatch(line -> line.endsWith(suffix));
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private PrintStream createConsoleStream(Path tmpFile) {
        PrintStream err = null;
        try {
            err = new PrintStream(new FileOutputStream(tmpFile.toFile()));
        } catch (Exception e) {
            throw new RuntimeException("Could not create side channel - " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
        return err;
    }

    /**
     * Copied from <a href= "https://stackoverflow.com/questions/7987395/how-to-write-data-to-two-java-io-outputstream-objects-at-once">https://stackoverflow.com/questions/7987395/how-to-write-data-to-two-java-io-outputstream-objects-at-once</a>
     */
    private final class TeeOutputStream extends OutputStream {

        private final OutputStream out;
        private final AtomicReference<PrintStream> teeSideChannel;
        private final List<Runnable> callbacks = new ArrayList<>();

        private TeeOutputStream(OutputStream out, AtomicReference<PrintStream> sideChannel) {
            this.out = out;
            this.teeSideChannel = sideChannel;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            teeSideChannel.get().write(b);
            runCallbacks();
        }

        @Override
        public void write(byte[] b) throws IOException {
            out.write(b);
            teeSideChannel.get().write(b);
            runCallbacks();
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            teeSideChannel.get().write(b, off, len);
            runCallbacks();
        }

        @Override
        public void flush() throws IOException {
            out.flush();
            teeSideChannel.get().flush();
        }

        @Override
        public void close() throws IOException {
            callbacks.clear();
            teeSideChannel.get().close();
        }

        private void registerCallback(Runnable callback) {
            callbacks.add(callback);
        }

        private void runCallbacks() throws IOException {
            out.flush();
            teeSideChannel.get().flush();
            callbacks.forEach(Runnable::run);
        }
    }
}
