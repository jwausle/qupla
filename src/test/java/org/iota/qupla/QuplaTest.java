package org.iota.qupla;

import org.iota.qupla.qupla.helper.ModuleLoaderTest;
import org.iota.qupla.test.utils.Console;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Unit loadAllQuplaModulesTest of Qupla cmdline.
 */
public class QuplaTest {
    private static final String QUPLA_CLASSPATH = System.getProperty("java.class.path");
    private static final String QUPLA_WORKING_DIR = System.getProperty("user.dir");
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
        Process qupla = execCmdline("Examples nextOne(10)", Optional.of("-Dmodulepath=src/main/resources")).start();
        qupla.waitFor(QUPLA_EXEC_TIMEOUT, TimeUnit.SECONDS);

        Assert.assertTrue("Cannot find 'Eval: nextOne(10)' in \n" + console.getContent(), console.anyLineContains("Eval: nextOne(10)"));
        Assert.assertTrue("Cannot find '  ==> fib2: (55) 100-10000000000000000000000' in \n" + console.getContent(), console.anyLineContains("  ==> fib2: (55) 100-10000000000000000000000"));
    }

    /*
     ****************************************************************
     * Private loadAllQuplaModulesTest helper
     ****************************************************************
     */
    private ProcessBuilder execCmdline(String quplaCmdline, Optional<String> modulepath) throws IOException {
        final List<String> command = new LinkedList<>();
        command.addAll(Arrays.asList("java", "-classpath", QUPLA_CLASSPATH));
        modulepath.ifPresent(mp -> command.add(mp));
        command.add(QUPLA_MAIN);
        command.addAll(Arrays.asList(quplaCmdline.split(" ")));

        System.out.println(command.stream().collect(Collectors.joining(" ")));

        return new ProcessBuilder(command.toArray(new String[0]))
                .directory(new File(QUPLA_WORKING_DIR))
                .redirectError(console.getFile().toFile())
                .redirectOutput(console.getFile().toFile());
    }
}
