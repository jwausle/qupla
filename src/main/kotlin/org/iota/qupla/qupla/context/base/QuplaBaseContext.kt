package org.iota.qupla.qupla.context.base

import org.iota.qupla.helper.BaseContext
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
import org.iota.qupla.qupla.expression.base.BaseSubExpr
import org.iota.qupla.qupla.parser.Module
import org.iota.qupla.qupla.statement.FuncStmt
import org.iota.qupla.qupla.statement.LutStmt
import org.iota.qupla.qupla.statement.TypeStmt

abstract class QuplaBaseContext : BaseContext() {
    override fun append(text: String): QuplaBaseContext {
        return super.append(text) as QuplaBaseContext
    }

    open fun eval(module: Module) {
        for (lut in module.luts) {
            evalLutDefinition(lut)
        }

        for (func in module.funcs) {
            evalFuncSignature(func)
        }

        for (func in module.funcs) {
            evalFuncBody(func)
        }
    }

    abstract fun evalAssign(assign: AssignExpr)

    open fun evalBaseExpr(expr: BaseExpr) {
        expr.error("Cannot call eval: " + expr.toString())
    }

    abstract fun evalConcat(concat: ConcatExpr)

    abstract fun evalConditional(conditional: CondExpr)

    abstract fun evalFuncBody(func: FuncStmt)

    abstract fun evalFuncCall(call: FuncExpr)

    abstract fun evalFuncSignature(func: FuncStmt)

    abstract fun evalLutDefinition(lut: LutStmt)

    abstract fun evalLutLookup(lookup: LutExpr)

    abstract fun evalMerge(merge: MergeExpr)

    abstract fun evalSlice(slice: SliceExpr)

    abstract fun evalState(state: StateExpr)

    open fun evalSubExpr(sub: BaseSubExpr) {
        // TODO Is .expr always not null here '.expr!!.'
        sub.expr!!.eval(this)
    }

    abstract fun evalType(type: TypeExpr)

    open fun evalTypeDefinition(type: TypeStmt) {
        evalBaseExpr(type)
    }

    abstract fun evalVector(integer: IntegerExpr)
}
