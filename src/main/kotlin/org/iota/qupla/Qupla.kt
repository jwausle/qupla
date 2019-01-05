package org.iota.qupla

import org.iota.qupla.abra.context.AbraEvalContext
import org.iota.qupla.dispatcher.Dispatcher
import org.iota.qupla.exception.CodeException
import org.iota.qupla.exception.ExitException
import org.iota.qupla.helper.TritConverter
import org.iota.qupla.helper.TritVector
import org.iota.qupla.qupla.context.QuplaEvalContext
import org.iota.qupla.qupla.context.QuplaPrintContext
import org.iota.qupla.qupla.context.QuplaToAbraContext
import org.iota.qupla.qupla.context.QuplaToVerilogContext
import org.iota.qupla.qupla.expression.FuncExpr
import org.iota.qupla.qupla.expression.IntegerExpr
import org.iota.qupla.qupla.expression.MergeExpr
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Module
import org.iota.qupla.qupla.parser.Tokenizer
import org.iota.qupla.qupla.statement.ExecStmt
import java.util.*

object Qupla {
    private val flags = arrayOf("-abra", "-echo", "-eval", "-fpga", "-math", "-test", "-tree")
    private val options = HashSet<String>()
    private var quplaToAbraContext: QuplaToAbraContext? = null

    fun codeException(ex: CodeException) {
        val token = ex.token
        if (token == null) {
            log("  ... Error:  " + ex.message)
            ex.printStackTrace(System.out)
            throw ExitException()
        }

        //TODO actual filename?
        if (token.source == null) {
            log("  ... Error:  " + ex.message)
            ex.printStackTrace(System.out)
            throw ExitException()
        }

        val path = token.source!!.pathName
        val fileName = path.substring(path.lastIndexOf('/') + 1)
        log("  ...(" + fileName + ":" + (token.lineNr + 1) + ") Error:  " + ex.message)
        if (BaseExpr.currentUse != null) {
            val use = BaseExpr.currentUse
            val types = use!!.typeInstantiations[BaseExpr.currentUseIndex]

            var name = use.name
            var first = true
            for (type in types) {
                name += if (first) "<" else ", "
                first = false
                name += type
            }

            log("  Function instantiated by: use $name>")
        }

        ex.printStackTrace(System.out)
        throw ExitException()
    }

    private fun evalExpression(statement: String) {
        log("\nEvaluate: $statement")
        val tokenizer = Tokenizer()
        tokenizer.lines.add(statement)
        tokenizer.module = Module(ArrayList())
        tokenizer.module!!.modules.addAll(Module.allModules.values)
        tokenizer.nextToken()
        val expr = MergeExpr(tokenizer).optimize()
        expr.analyze()
        runEval(expr)
    }

    fun log(text: String) {
        BaseExpr.logLine(text)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            try {
                for (arg in args) {
                    for (flag in flags) {
                        if (flag == arg) {
                            options.add(flag)
                            break
                        }
                    }

                    if (arg.startsWith("-")) {
                        if (!options.contains(arg)) {
                            log("Unrecognized argument:$arg")
                        }

                        continue
                    }

                    // expression to be evaluated later
                    if (arg.contains("(")) {
                        continue
                    }

                    Module.parse(arg)
                }

                processOptions()

                for (arg in args) {
                    // interpret as expression to be evaluated
                    // can be a function call, but also an expression surrounded by ( and )
                    if (arg.contains("(")) {
                        evalExpression(arg)
                    }
                }
            } catch (ex: CodeException) {
                codeException(ex)
            }

        } catch (ex: ExitException) {
        }

    }

    private fun processOptions() {
        // echo back all modules as source
        if (options.contains("-echo")) {
            runEchoSource()
        }

        // echo back all modules as Abra tritcode
        if (options.contains("-abra")) {
            runAbraGenerator()
        }

        // emit verilog code to Verilog.txt
        if (options.contains("-fpga")) {
            runFpgaGenerator()
        }

        // display the syntax tree
        if (options.contains("-tree")) {
            runTreeViewer()
        }

        // run all unit test comments
        if (options.contains("-math")) {
            runMathTests()
        }

        // run all unit test comments
        if (options.contains("-test")) {
            runTests()
        }

        // run all evaluation comments
        if (options.contains("-eval")) {
            runEvals()
        }
    }

    private fun runAbraGenerator() {
        log("Run Abra generator")
        val singleModule = Module(Module.allModules.values)
        quplaToAbraContext = QuplaToAbraContext()
        quplaToAbraContext!!.eval(singleModule)
    }

    private fun runEchoSource() {
        log("Generate Qupla.txt")
        val singleModule = Module(Module.allModules.values)
        QuplaPrintContext().eval(singleModule)
    }

    private fun runEval(expr: BaseExpr) {
        log("Eval: " + expr.toString())

        val context = QuplaEvalContext()
        val abraEvalContext = AbraEvalContext()

        var mSec = System.currentTimeMillis()
        if (options.contains("-abra")) {
            abraEvalContext.eval(quplaToAbraContext!!, expr)
            context.value = abraEvalContext.value
        } else {
            expr.eval(context)
        }

        mSec = System.currentTimeMillis() - mSec

        log("  ==> " + expr.typeInfo!!.display(context.value!!))
        log("Time: $mSec ms")

        if (expr is FuncExpr) {
            val dispatcher = Dispatcher(Module.allModules.values)
            context.createEntityEffects(expr.func!!)
            dispatcher.runQuants()
            dispatcher.finished()
        }
    }

    private fun runEvals() {
        var mSec = System.currentTimeMillis()
        for (module in Module.allModules.values) {
            for (exec in module.execs) {
                if (exec.expected == null) {
                    runEval(exec.expr)
                }
            }
        }

        mSec = System.currentTimeMillis() - mSec
        log("All evals: $mSec ms\n")
    }

    private fun runFpgaGenerator() {
        log("Run Verilog compiler")
        val singleModule = Module(Module.allModules.values)
        QuplaToVerilogContext().eval(singleModule)
    }

    private fun runMathTest(context: QuplaEvalContext, expr: FuncExpr, lhs: Int, rhs: Int, result: Int) {
        val lhsArg = expr.args[0] as IntegerExpr
        lhsArg.vector = TritVector(TritConverter.fromLong(lhs.toLong())).slicePadded(0, lhsArg.size)!!
        val rhsArg = expr.args[1] as IntegerExpr
        rhsArg.vector = TritVector(TritConverter.fromLong(rhs.toLong())).slicePadded(0, rhsArg.size)!!

        expr.eval(context)

        val value = context.value!!.displayValue(0, 0)
        if (value != Integer.toString(result)) {
            lhsArg.name = Integer.toString(lhs)
            rhsArg.name = Integer.toString(rhs)
            log(expr.toString() + " = " + result + ", found: " + context.value + ", inputs: " + lhsArg.vector + " and " + rhsArg.vector)
        }
    }

    private fun runMathTests() {
        //final String statement = "fullAdd<Tiny>(1,1,0)";
        //final String statement = "fullMul<Tiny>(1,1)";
        val statement = "div<Tiny>(1,1)"
        log("\nEvaluate: $statement")
        var mSec = System.currentTimeMillis()
        val tokenizer = Tokenizer()
        tokenizer.lines.add(statement)
        tokenizer.module = Module(ArrayList())
        tokenizer.module!!.modules.addAll(Module.allModules.values)
        tokenizer.nextToken()
        val expr = MergeExpr(tokenizer).optimize() as FuncExpr
        expr.analyze()
        val context = QuplaEvalContext()
        var lhs = 2100
        while (lhs <= 9841) {
            log("Iteration: $lhs")
            var rhs = lhs
            while (rhs <= 9841) {
                //runMathTest(context, expr, lhs, rhs, lhs + rhs);
                //runMathTest(context, expr, lhs, rhs, lhs * rhs);
                runMathTest(context, expr, lhs, rhs, lhs / rhs)
                rhs += 1
            }
            lhs += 1
        }

        mSec = System.currentTimeMillis() - mSec
        log("Time: $mSec ms")
    }

    private fun runTest(exec: ExecStmt) {
        log("Test: " + exec.expected + " = " + exec.expr)

        val context = QuplaEvalContext()
        val abraEvalContext = AbraEvalContext()

        var mSec = System.currentTimeMillis()
        if (options.contains("-abra")) {
            abraEvalContext.eval(quplaToAbraContext!!, exec.expr)
            context.value = abraEvalContext.value
        } else {
            exec.expr.eval(context)
        }

        mSec = System.currentTimeMillis() - mSec

        if (!exec.succeed(context.value!!)) {
            val lhs = exec.expr.typeInfo!!.displayValue(exec.expected!!.vector)
            val rhs = exec.expr.typeInfo!!.displayValue(context.value!!)
            exec.error("Test expected $lhs but found $rhs")
        }

        log("Time: $mSec ms")
    }

    private fun runTests() {
        var mSec = System.currentTimeMillis()
        for (module in Module.allModules.values) {
            for (exec in module.execs) {
                if (exec.expected != null) {
                    runTest(exec)
                }
            }
        }

        mSec = System.currentTimeMillis() - mSec
        log("All tests: $mSec ms\n")
    }

    private fun runTreeViewer() {
        //TODO
    }
}
