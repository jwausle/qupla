package org.iota.qupla.test.utils

import java.io.Closeable
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.regex.PatternSyntaxException

/**
 * Record all System.out and System.err output and provide convenience assertions in the messages.
 *
 * **Attention** - Don't forget to clean things up by calling [Console.close] at the end
 * of each Console usage.
 */
class Console : Closeable {

    /**
     * @return the data file.
     */
    var file: Path? = null
        private set
    private val sideChannel: AtomicReference<PrintStream>
    private val reroutedStdOutPrint: PrintStream
    private val reroutedStdErrPrint: PrintStream
    private val reroutedStdOut: TeeOutputStream
    private val reroutedStdErr: TeeOutputStream

    /**
     * @return All data that has been logged to the console.
     */
    val content: String
        get() {
            flush()
            try {
                return String(Files.readAllBytes(file!!))
            } catch (e: IOException) {
                throw RuntimeException("Cannot read log file '$file'", e)
            }

        }

    /**
     *
     *
     * Construct a new instance to collect stdout and stderr output for later analysis. stdout and stderr will be rerouted when this constructor returns. Output
     * will still be printed to console but is also recorded.
     *
     * Don't forget to clean things up by calling [Console.close] at the end of each Console usage.
     */
    init {
        this.file = createTmpFile()
        this.sideChannel = AtomicReference(createConsoleStream(file))

        reroutedStdOut = TeeOutputStream(ORIGINAL_STDOUT, sideChannel)
        reroutedStdErr = TeeOutputStream(ORIGINAL_STDERR, sideChannel)
        reroutedStdOutPrint = PrintStream(reroutedStdOut)
        reroutedStdErrPrint = PrintStream(reroutedStdErr)

        System.setOut(reroutedStdOutPrint)
        System.setErr(reroutedStdErrPrint)
    }

    private fun createTmpFile(): Path {
        try {
            return Files.createTempFile("slm", ".console")
        } catch (e: Exception) {
            throw RuntimeException("Console initalization fail with " + e.javaClass.simpleName + " because - " + e.message, e)
        }

    }

    private fun deleteTmpFile() {
        if (file != null) {
            try {
                Files.deleteIfExists(file!!)
            } catch (e: IOException) {
                ORIGINAL_STDERR.println("WARN - Possible resource leak: Could not delete temporary test file '" + file!!.toString() + "' - "
                        + e.javaClass.simpleName + ": " + e.message)
            } finally {
                file = null
                sideChannel.get().close()
            }
        }
    }

    /**
     * Set the System.err and System.out back to standard and release all resources. This object becomes unusable when this method returns.
     */
    override fun close() {
        sideChannel.get().close()
        reroutedStdOutPrint.close()
        reroutedStdErrPrint.close()
        deleteTmpFile()
        System.setErr(ORIGINAL_STDERR)
        System.setOut(ORIGINAL_STDOUT)
        println("------------------------------------------------------------------")
    }

    private fun flush() {
        sideChannel.get().flush()
        reroutedStdOutPrint.flush()
        reroutedStdErrPrint.flush()
    }

    /**
     * Discard all recorded log messages.
     */
    @Synchronized
    fun clear() {
        flush()
        sideChannel.get().close()
        deleteTmpFile()
        file = createTmpFile()
        sideChannel.set(createConsoleStream(file))
    }

    /**
     *
     *
     * Like [.anyLineMatches] but does not use a regex but a pure String comparison.
     *
     * The line matching uses a "contains" logic, rather than an "equals" logic to make matching a bit more fuzzy.
     *
     *
     *
     * **Example:**
     *
     * <pre>
     * &#64;Test
     * public void test() {
     * System.out.println("nomatch");
     * Assert.assertTrue(console.anyLineContains("match")); // passes
     * }
    </pre> *
     *
     *
     * @param line - Match lines with this.
     * @return `true` if at least one line contains `line`. Otherwise `false`.
     */
    fun anyLineContains(line: String): Boolean {
        return internalContains(line, 0) != null
    }

    private fun internalContains(line: String, offset: Int): Int? {
        var contents: List<String> = ArrayList()
        try {
            contents = Files.readAllLines(file!!)
        } catch (e: IOException) {
            println(e.message)
        }

        for (i in offset until contents.size) {
            val content = contents[i]
            if (content.contains(line)) {
                return i
            }
        }

        return null
    }

    /**
     * Check if any line matches the provided `regex`.
     *
     * @param regex - Match lines with this regular expression.
     * @return `true` if at least one line matches `regex`. Otherwise `false`.
     * @throws PatternSyntaxException - if the regular expression's syntax is invalid.
     */
    fun anyLineMatches(regex: String): Boolean {
        try {
            return Files.readAllLines(file!!).stream().anyMatch { line -> line.matches(regex.toRegex()) }
        } catch (e: IOException) {
            println(e.message)
            return false
        }

    }

    /**
     * Check if any line starts with `prefix`.
     *
     * @param prefix - String to match line beginnings.
     * @return `true` if at least one line starts with `prefix`. Otherwise `false`.
     */
    fun anyLineStartsWith(prefix: String): Boolean {
        try {
            return Files.readAllLines(file!!).stream().anyMatch { line -> line.startsWith(prefix) }
        } catch (e: IOException) {
            println(e.message)
            return false
        }

    }

    /**
     * Check if any line ends with `suffix`.
     *
     * @param suffix - String to match line endings.
     * @return `true` if at least one line ends with `suffix`. Otherwise `false`.
     */
    fun anyLineEndsWith(suffix: String): Boolean {
        try {
            return Files.readAllLines(file!!).stream().anyMatch { line -> line.endsWith(suffix) }
        } catch (e: IOException) {
            println(e.message)
            return false
        }

    }

    private fun createConsoleStream(tmpFile: Path?): PrintStream {
        var err: PrintStream?
        try {
            err = PrintStream(FileOutputStream(tmpFile!!.toFile()))
        } catch (e: Exception) {
            throw RuntimeException("Could not create side channel - " + e.javaClass.simpleName + ": " + e.message, e)
        }

        return err
    }

    /**
     * Copied from [https://stackoverflow.com/questions/7987395/how-to-write-data-to-two-java-io-outputstream-objects-at-once](https://stackoverflow.com/questions/7987395/how-to-write-data-to-two-java-io-outputstream-objects-at-once)
     */
    private inner class TeeOutputStream constructor(private val out: OutputStream, private val teeSideChannel: AtomicReference<PrintStream>) : OutputStream() {
        private val callbacks = ArrayList<Runnable>()

        @Throws(IOException::class)
        override fun write(b: Int) {
            out.write(b)
            teeSideChannel.get().write(b)
            runCallbacks()
        }

        @Throws(IOException::class)
        override fun write(b: ByteArray) {
            out.write(b)
            teeSideChannel.get().write(b)
            runCallbacks()
        }

        @Throws(IOException::class)
        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            teeSideChannel.get().write(b, off, len)
            runCallbacks()
        }

        @Throws(IOException::class)
        override fun flush() {
            out.flush()
            teeSideChannel.get().flush()
        }

        @Throws(IOException::class)
        override fun close() {
            callbacks.clear()
            teeSideChannel.get().close()
        }

        private fun registerCallback(callback: Runnable) {
            callbacks.add(callback)
        }

        @Throws(IOException::class)
        private fun runCallbacks() {
            out.flush()
            teeSideChannel.get().flush()
            callbacks.forEach(Consumer<Runnable> { it.run() })
        }
    }

    companion object {
        private val ORIGINAL_STDOUT = System.out
        private val ORIGINAL_STDERR = System.err
    }
}
