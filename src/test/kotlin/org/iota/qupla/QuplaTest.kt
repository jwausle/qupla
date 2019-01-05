package org.iota.qupla

import org.iota.qupla.test.utils.Console
import org.junit.After
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Unit test of Qupla QUPLA_CMDLINE.
 */
class QuplaTest {
    private val QUPLA_MAIN = "org.iota.qupla.Qupla"
    private val QUPLA_CLASSPATH = System.getProperty("java.class.path")
    private val QUPLA_CMDLINE = arrayOf("java", "-classpath", "${QUPLA_CLASSPATH}", QUPLA_MAIN, "Examples", "fibonacci(10)")
    private val QUAPLA_WORKING_DIR = System.getProperty("user.dir") + "/src/main/resources"
    private val QUPLA_EXEC_TIMEOUT = 10

    private val console: Console = Console()

    @After
    fun afterTest() {
        console.close()
    }

    @Test
    fun runFibonacciTest() {
        val qupla = execCmdline(QUPLA_CMDLINE)
        qupla.waitFor(QUPLA_EXEC_TIMEOUT.toLong(), TimeUnit.SECONDS)

        val messageIfFail = """
            |> Missing line 'Eval: fibonacci(10)' in
            |
            |>>>>>>>>>>>>>>>>>>>> console >>>>>>>>>>>>>>>>>>>
            |${console.content}
            |<<<<<<<<<<<<<<<<<<<< console <<<<<<<<<<<<<<<<<<<
            |
            |$ cd ${QUAPLA_WORKING_DIR}
            |$ ${QUPLA_CMDLINE.joinToString(" ")}""".trimMargin()

        Assert.assertTrue(messageIfFail, console.anyLineContains("Eval: fibonacci(10)"))
        Assert.assertTrue(messageIfFail, console.anyLineContains("  ==> (55) 100-10000000000000000000000000000000000000000000000000000000000000000000000000000"))
    }

    /*
     ****************************************************************
     * Private test helper
     ****************************************************************
     */
    private fun execCmdline(cmdline: Array<String>): Process {
        return ProcessBuilder(cmdline.toMutableList())
                .directory(File(QUAPLA_WORKING_DIR))
                .redirectError(console.file!!.toFile())
                .redirectOutput(console.file!!.toFile())
                .start()
    }
}
