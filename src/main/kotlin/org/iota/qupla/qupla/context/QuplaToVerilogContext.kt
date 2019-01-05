package org.iota.qupla.qupla.context

import java.util.ArrayList

import org.iota.qupla.helper.BaseContext
import org.iota.qupla.helper.TritVector
import org.iota.qupla.helper.Verilog
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
import org.iota.qupla.qupla.parser.Module
import org.iota.qupla.qupla.statement.FuncStmt
import org.iota.qupla.qupla.statement.LutStmt
import org.iota.qupla.qupla.statement.helper.LutEntry

class QuplaToVerilogContext : QuplaBaseContext() {
    private val verilog = Verilog()

    private fun appendVector(trits: String): BaseContext {
        return verilog.appendVector(this, trits)
    }

    override fun eval(module: Module) {
        fileOpen("QuplaVerilog.txt")

        super.eval(module)

        verilog.addMergeLut(this)
        verilog.addMergeFuncs(this)

        fileClose()
    }

    override fun evalAssign(assign: AssignExpr) {
        // TODO Is .name always not null here '.name!!.'
        append(assign.name!!).append(" = ")
        // TODO Is .expr always not null here '.expr!!.'
        assign.expr!!.eval(this)
    }

    override fun evalConcat(concat: ConcatExpr) {
        // TODO Is 'rhs == null' the right check here? 'lhs' is used
        if (concat.rhs == null) {
            // TODO Is .lhs always not null here '.lhs!!.'
            concat.lhs!!.eval(this)
            return
        }

        append("{ ")
        // TODO Is .lhs always not null here '.lhs!!.'
        concat.lhs!!.eval(this)
        append(" : ")

        concat.rhs!!.eval(this)
        append(" }")
    }

    fun evalConcatExprs(exprs: ArrayList<BaseExpr>) {
        if (exprs.size == 1) {
            val expr = exprs[0]
            expr.eval(this)
            return
        }

        var first = true
        for (expr in exprs) {
            append(if (first) "{ " else " : ")
            first = false
            expr.eval(this)
        }

        append(" }")
    }

    override fun evalConditional(conditional: CondExpr) {
        //TODO proper handling of nullify when condition not in [1, -]
        // TODO Is .condition always not null here '.condition!!.'
        conditional.condition!!.eval(this)
        append(" == ")
        appendVector("1").append(" ? ")
        // TODO Is .trueBrnach always not null here '.trueBranch!!.'
        conditional.trueBranch!!.eval(this)
        append(" : ")
        if (conditional.falseBranch == null) {
            appendVector(TritVector(conditional.size, '@').trits())
            return
        }

        conditional.falseBranch!!.eval(this)
    }

    override fun evalFuncBody(func: FuncStmt) {
        newline()

        // TODO Is .name always not null here '.name!!.'
        val funcName = func.name!!
        append("function [" + (func.size * 2 - 1) + ":0] ").append(funcName).append("(").newline().indent()

        var first = true
        for (param in func.params) {
            append(if (first) "  " else ", ")
            first = false
            // TODO Is .name always not null here '.name!!.'
            append("input [" + (param.size * 2 - 1) + ":0] ").append(param.name!!).newline()
        }

        append(");").newline()

        for (assignExpr in func.assignExprs) {
            // TODO Is .name always not null here '.name!!.'
            append("reg [" + (assignExpr.size * 2 - 1) + ":0] ").append(assignExpr.name!!).append(";").newline()
        }

        if (func.assignExprs.size != 0) {
            newline()
        }

        append("begin").newline().indent()

        for (assignExpr in func.assignExprs) {
            assignExpr.eval(this)
            append(";").newline()
        }

        append(funcName).append(" = ")
        // TODO Is .returnExpr always not null here '.returnExpr!!.'
        func.returnExpr!!.eval(this)
        append(";").newline().undent()

        append("end").newline().undent()
        append("endfunction").newline()
    }

    override fun evalFuncCall(call: FuncExpr) {
        // TODO Is .name always not null here '.name!!.'
        append(call.name!!)

        var first = true
        for (arg in call.args) {
            append(if (first) "(" else ", ")
            first = false
            arg.eval(this)
        }

        append(")")
    }

    override fun evalFuncSignature(func: FuncStmt) {
        // generate Verilog forward declarations for functions?
    }

    override fun evalLutDefinition(lut: LutStmt) {
        val lutName = lut.name + "_lut"
        append("function [" + (lut.size * 2 - 1) + ":0] ").append(lutName).append("(").newline().indent()

        var first = true
        for (i in 0 until lut.inputSize) {
            append(if (first) "  " else ", ")
            first = false
            append("input [1:0] ").append("p$i").newline()
        }
        append(");").newline()

        append("begin").newline().indent()

        append("case ({")
        first = true
        for (i in 0 until lut.inputSize) {
            append(if (first) "" else ", ").append("p$i")
            first = false
        }

        append("})").newline()

        for (entry in lut.entries) {
            appendVector(entry.inputs).append(": ").append(lutName).append(" = ")
            appendVector(entry.outputs).append(";").newline()
        }

        append("default: ").append(lutName).append(" = ")
        // TODO Is .undefined always not null here '.undefined!!.'
        appendVector(lut.undefined!!.trits()).append(";").newline()
        append("endcase").newline().undent()

        append("end").newline().undent()
        append("endfunction").newline().newline()
    }

    override fun evalLutLookup(lookup: LutExpr) {
        // TODO Is .name always not null here '.name!!.'
        append(lookup.name!!).append("_lut(")
        var first = true
        for (arg in lookup.args) {
            append(if (first) "" else ", ")
            first = false
            arg.eval(this)
        }

        append(")")
    }

    override fun evalMerge(merge: MergeExpr) {
        // if there is no rhs we return lhs
        // TODO is 'rsh == null' the righ check her? 'lhs' is used
        if (merge.rhs == null) {
            // TODO Is .lhs always not null here '.lhs!!.'
            merge.lhs!!.eval(this)
            return
        }

        // TODO Is .lhs always not null here '.lhs!!.'
        verilog.mergefuncs.add(merge.lhs!!.size)
        append(verilog.prefix + merge.lhs!!.size + "(")
        merge.lhs!!.eval(this)
        append(", ")
        merge.rhs!!.eval(this)
        append(")")
    }

    override fun evalSlice(slice: SliceExpr) {
        // TODO Is .name always not null here '.name!!.'
        append(slice.name!!)
        if (slice.startOffset == null && slice.fields.size == 0) {
            return
        }

        val start = slice.start * 2
        val end = start + slice.size * 2 - 1
        append("[$end:$start]")
    }

    override fun evalState(state: StateExpr) {
        //    // save index of state variable to be able to distinguish
        //    // between multiple state vars in the same function
        //    callTrail[callNr] = (byte) state.stackIndex;
        //
        //    final StateValue call = new StateValue();
        //    call.path = callTrail;
        //    call.pathLength = callNr + 1;
        //
        //    // if state was saved before set to that value otherwise set to zero
        //    final StateValue stateValue = stateValues.get(call);
        //    value = stateValue != null ? stateValue.value : state.zero;
        //    stack.push(value);
    }

    override fun evalType(type: TypeExpr) {
        evalConcatExprs(type.fields)
    }

    override fun evalVector(integer: IntegerExpr) {
        appendVector(integer.vector.trits())
    }
}

