package org.iota.qupla.qupla.context

import java.util.HashSet
import java.util.Stack

import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.expression.AssignExpr
import org.iota.qupla.qupla.expression.ConcatExpr
import org.iota.qupla.qupla.expression.CondExpr
import org.iota.qupla.qupla.expression.FuncExpr
import org.iota.qupla.qupla.expression.IntegerExpr
import org.iota.qupla.qupla.expression.LutExpr
import org.iota.qupla.qupla.expression.MergeExpr
import org.iota.qupla.qupla.expression.SliceExpr
import org.iota.qupla.qupla.expression.StateExpr
import org.iota.qupla.qupla.expression.TypeExpr
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.statement.FuncStmt
import org.iota.qupla.qupla.statement.LutStmt

class QuplaAnyNullContext : QuplaBaseContext() {

    var isNull: Boolean = false
    val stack = Stack<Boolean>()
    var stackFrame: Int = 0

    override fun evalAssign(assign: AssignExpr) {
        // TODO Is .expr always not null here 'expr!!.'
        assign.expr!!.eval(this)
        stack.push(isNull)
    }

    override fun evalConcat(concat: ConcatExpr) {
        // if any expr is non-null the entire concat is non-null
        // TODO Is lhs always not null here '.lhs!!.'
        concat.lhs!!.eval(this)
        if (!isNull || concat.rhs == null) {
            return
        }
        // TODO Is .rhs always not null here '.rhs!!.'
        concat.rhs!!.eval(this)
    }

    override fun evalConditional(conditional: CondExpr) {
        // TODO Is .condition always not null here '.condition!!.'
        conditional.condition!!.eval(this)
        if (isNull || conditional.trueBranch == null) {
            // null for nullify() condition parameter on both sides
            // or not a conditional expression
            return
        }

        // if either expression is non-null the result is non-null
        // TODO Is .trueBranch always not null here '.trueBranch!!.'
        conditional.trueBranch!!.eval(this)
        if (isNull && conditional.falseBranch != null) {
            // TODO Is .falseBranch always not null here '.falseBranch!!.'
            conditional.falseBranch!!.eval(this)
        }
    }

    override fun evalFuncBody(func: FuncStmt) {
        if (wasInspected(func)) {
            // already done
            return
        }

        if (func.params.size == 1) {
            // easy decision, this falls under all params null rule
            func.anyNull = true
            return
        }

        func.anyNull = false
        if (inspecting.contains(func)) {
            // recursion detected, cannot determine
            // keep non-null to be on the safe side
            return
        }

        inspecting.add(func)

        // set all params on stack to false
        for (param in func.params) {
            stack.push(false)
        }

        var anyNull = true
        for (i in func.params.indices) {
            stack[i] = true

            func.eval(this)
            if (!isNull) {
                anyNull = false
                break
            }

            stack[i] = false
        }

        inspecting.remove(func)

        func.anyNull = anyNull
        if (!anyNull) {
            log("No anyNull for " + func.name)

            // only need to add the ones that are not anyNull
            // the ones that do have definitely been inspected
            inspected.add(func)
        }

        stack.clear()
        stackFrame = 0
    }

    override fun evalFuncCall(call: FuncExpr) {
        if (call.args.size == 1) {
            // single argument means the all-null rule is invoked
            // so we return whether the argument was null
            val arg = call.args[0]
            arg.eval(this)
            return
        }

        val currentlyInspecting = inspecting.contains(call.func)
        // TODO Is .func always not null here '.func!!.'
        if (!currentlyInspecting && !wasInspected(call.func!!)) {
            // do inspection first
            QuplaAnyNullContext().evalFuncBody(call.func!!)
        }

        val newStackFrame = stack.size

        if (!pushArguments(call)) {
            isNull = false

            // we wait until we know it's not an all null before checking for recursion
            // that way we might be able to decide even with recursion
            if (!currentlyInspecting) {
                // no recursion detected
                val oldStackFrame = stackFrame
                stackFrame = newStackFrame
                inspecting.add(call.func!!)
                call.func!!.eval(this)
                inspecting.remove(call.func!!)
                stackFrame = oldStackFrame
            }
        }

        stack.setSize(newStackFrame)
    }

    override fun evalFuncSignature(func: FuncStmt) {

    }

    override fun evalLutDefinition(lut: LutStmt) {}

    override fun evalLutLookup(lookup: LutExpr) {
        // if any arg is null the result is null
        for (arg in lookup.args) {
            arg.eval(this)
            if (isNull) {
                return
            }
        }
    }

    override fun evalMerge(merge: MergeExpr) {
        // if either expression is non-null the result is non-null
        // TODO Is .lhs always not null here '.lhs!!.'
        merge.lhs!!.eval(this)
        if (merge.rhs == null || !isNull) {
            return
        }
        // TODO Is .rhs always not null here '.rhs!!.'
        merge.rhs!!.eval(this)
    }

    override fun evalSlice(slice: SliceExpr) {
        // a slice of null is null, a slice of non-null is non-null
        isNull = stack[stackFrame + slice.stackIndex]
    }

    override fun evalState(state: StateExpr) {
        // of course this is non-null: state vars cannot be null
        isNull = false
    }

    override fun evalType(type: TypeExpr) {
        // if any field is non-null the entire type is non-null
        for (expr in type.fields) {
            expr.eval(this)
            if (!isNull) {
                return
            }
        }
    }

    override fun evalVector(integer: IntegerExpr) {
        // of course this is non-null
        isNull = false
    }

    private fun log(text: String) {
        if (allowLog) {
            BaseExpr.logLine(text)
        }
    }

    private fun pushArguments(call: FuncExpr): Boolean {
        var allNull = true
        for (arg in call.args) {
            arg.eval(this)
            // TODO Is .func always not null here '.func!!.'
            if (call.func!!.anyNull && isNull) {
                return true
            }

            stack.push(isNull)
            allNull = allNull and isNull
        }

        return allNull
    }

    private fun wasInspected(func: FuncStmt): Boolean {
        return func.anyNull || inspected.contains(func)
    }

    companion object {
        private val allowLog = false
        private val inspected = HashSet<FuncStmt>()
        private val inspecting = HashSet<FuncStmt>()
    }
}
