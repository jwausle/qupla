package org.iota.qupla;

import org.iota.qupla.test.utils.Console;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Unit test of Qupla cmdline.
 */
public class QuplaTest {
    private static final String QUPLA_CLASSPATH = System.getProperty("user.dir") + "/build/classes/java/main";
    private static final String QUAPLA_WORKING_DIR = System.getProperty("user.dir") + "/src/main/resources/";
    private static final String QUPLA_MAIN = "org.iota.qupla.Qupla";
    private static final int QUPLA_EXEC_TIMEOUT = 10;

    private Console console;

    @Before
    public void beforeTest() {
        console = new Console();
    }

    @After
    public void afterTest() {
        console.close();
    }

    @Test
    public void runFibonacciTest() throws IOException, InterruptedException {
        Process qupla = execCmdline("Examples nextOne(10)");
        qupla.waitFor(QUPLA_EXEC_TIMEOUT, TimeUnit.SECONDS);

        Assert.assertTrue("Cannot find 'Eval: fibonacci(10)' in \n" + console.getContent(), console.anyLineContains("Eval: nextOne(10)"));
        Assert.assertTrue("Cannot find '  ==> (55)...' in \n" + console.getContent(), console.anyLineContains("  ==> fib2: (55) 100-10000000000000000000000"));
    }

    /*
     ****************************************************************
     * Private test helper
     ****************************************************************
     */
    private Process execCmdline(String quplaCmdline) throws IOException {
        final List<String> command = Stream.concat(
                Stream.of("java", "-classpath", QUPLA_CLASSPATH, QUPLA_MAIN),
                Stream.of(quplaCmdline.split(" ")))
                .collect(Collectors.toList());

        return new ProcessBuilder(command.toArray(new String[0]))
                .directory(new File(QUAPLA_WORKING_DIR))
                .redirectError(console.getFile().toFile())
                .redirectOutput(console.getFile().toFile())
                .start();
    }
}
