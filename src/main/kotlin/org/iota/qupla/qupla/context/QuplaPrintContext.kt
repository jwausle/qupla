package org.iota.qupla.qupla.context

import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.expression.AffectExpr
import org.iota.qupla.qupla.expression.AssignExpr
import org.iota.qupla.qupla.expression.ConcatExpr
import org.iota.qupla.qupla.expression.CondExpr
import org.iota.qupla.qupla.expression.FieldExpr
import org.iota.qupla.qupla.expression.FuncExpr
import org.iota.qupla.qupla.expression.IntegerExpr
import org.iota.qupla.qupla.expression.JoinExpr
import org.iota.qupla.qupla.expression.LutExpr
import org.iota.qupla.qupla.expression.MergeExpr
import org.iota.qupla.qupla.expression.NameExpr
import org.iota.qupla.qupla.expression.SliceExpr
import org.iota.qupla.qupla.expression.StateExpr
import org.iota.qupla.qupla.expression.SubExpr
import org.iota.qupla.qupla.expression.TypeExpr
import org.iota.qupla.qupla.expression.base.BaseBinaryExpr
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.expression.base.BaseSubExpr
import org.iota.qupla.qupla.expression.constant.ConstFactor
import org.iota.qupla.qupla.expression.constant.ConstNumber
import org.iota.qupla.qupla.expression.constant.ConstSubExpr
import org.iota.qupla.qupla.expression.constant.ConstTypeName
import org.iota.qupla.qupla.parser.Module
import org.iota.qupla.qupla.statement.ExecStmt
import org.iota.qupla.qupla.statement.FuncStmt
import org.iota.qupla.qupla.statement.LutStmt
import org.iota.qupla.qupla.statement.TemplateStmt
import org.iota.qupla.qupla.statement.TypeStmt
import org.iota.qupla.qupla.statement.UseStmt
import org.iota.qupla.qupla.statement.helper.LutEntry
import org.iota.qupla.qupla.statement.helper.TritStructDef
import org.iota.qupla.qupla.statement.helper.TritVectorDef

class QuplaPrintContext : QuplaBaseContext() {
    override fun eval(module: Module) {
        fileOpen("Qupla.txt")

        for (imp in module.imports) {
            // TODO Is .name always not null here '.name!!.'
            append("import ").append(imp.name!!).newline()
        }

        for (type in module.types) {
            evalTypeDefinition(type)
        }

        super.eval(module)

        for (template in module.templates) {
            evalTemplateDefinition(template)
        }

        for (use in module.uses) {
            evalUseDefinition(use)
        }

        for (exec in module.execs) {
            evalExec(exec)
        }

        fileClose()
    }

    fun evalAffect(affect: AffectExpr) {
        // TODO Is .name always not null here '.name!!.'
        append("affect ").append(affect.name!!)
        if (affect.delay != null) {
            // TODO Is .delay always not null here '.delay!!.'
            // TODO-AND Is .name always not null here '.name!!.'
            append(" delay ").append(affect.delay!!.name!!)
        }
    }

    override fun evalAssign(assign: AssignExpr) {
        // TODO Is .name always not null here '.name!!.'
        append(assign.name!!).append(" = ")
        // TODO Is .expr always not null here '.expr!!.'
        assign.expr!!.eval(this)
    }

    override fun evalBaseExpr(expr: BaseExpr) {
        if (expr is ConstTypeName || expr is ConstNumber) {
            // TODO Is .name always not null here '.name!!.'
            append(expr.name!!)
            return
        }

        if (expr is TritVectorDef) {
            if (expr.name != null) {
                // TODO Is .name always not null here '.name!!.'
                append(expr.name!!).append(" ")
            }

            evalTritVector(expr)
            return
        }

        if (expr is TritStructDef) {
            // TODO Is .name always not null here '.name!!.'
            append(expr.name!!).append(" ")
            evalTritStruct(expr)
            return
        }

        if (expr is LutEntry) {
            evalLutEntry(expr)
            return
        }

        if (expr is NameExpr) {
            if (expr.type != null) {
                // TODO Is .type always not null here '.type!!.'
                // TODO-AND Is .name always not null here '.name!!.'
                append(expr.type!!.name!!).append(" ")
            }
            // TODO Is .name always not null here '.name!!.'
            append(expr.name!!)
            return
        }

        if (expr is BaseBinaryExpr) {
            // TODO Is .lhs always not null here '.lhs!!.'
            expr.lhs!!.eval(this)
            if (expr.rhs != null) {
                // TODO Is .operator always not null here '.operator!!.'
                append(" " + expr.operator!!.text + " ")
                expr.rhs!!.eval(this)
            }
            return
        }

        if (expr is ExecStmt) {
            evalExec(expr)
            return
        }

        if (expr is UseStmt) {
            evalUseDefinition(expr)
            return
        }

        super.evalBaseExpr(expr)
    }

    override fun evalConcat(concat: ConcatExpr) {
        // TODO Is .lhs always not null here '.lhs!!.'
        concat.lhs!!.eval(this)
        if (concat.rhs == null) {
            return
        }

        append(" & ")
        concat.rhs!!.eval(this)
    }

    override fun evalConditional(conditional: CondExpr) {
        // TODO Is .condition always not null here '.condition!!.'
        conditional.condition!!.eval(this)

        if (conditional.trueBranch == null) {
            return
        }

        append(" ? ")
        conditional.trueBranch!!.eval(this)
        append(" : ")
        if (conditional.falseBranch == null) {
            append("null")
            return
        }

        conditional.falseBranch!!.eval(this)
    }

    private fun evalExec(exec: ExecStmt) {
        if (exec.expected == null) {
            append("eval ")
            exec.expr.eval(this)
            return
        }

        append("test ")
        exec.expected!!.eval(this)
        append(" = ")
        exec.expr.eval(this)
    }

    override fun evalFuncBody(func: FuncStmt) {
        newline()

        evalFuncBodySignature(func)

        append(" {").newline().indent()

        for (envExpr in func.envExprs) {
            if (envExpr is AffectExpr) {
                evalAffect(envExpr)
                newline()
                continue
            }

            evalJoin(envExpr as JoinExpr)
            newline()
        }

        for (stateExpr in func.stateExprs) {
            evalState(stateExpr as StateExpr)
            newline()
        }

        for (assignExpr in func.assignExprs) {
            assignExpr.eval(this)
            newline()
        }

        append("return ")
        // TODO Is .returnExpr always not null here '.returnExpr!!.'
        func.returnExpr!!.eval(this)
        newline()

        undent().append("}").newline()
    }

    fun evalFuncBodySignature(func: FuncStmt) {
        // TODO Is .returnType always not null here '.returnType!!.'
        // TODO-AND Is .name always not null here '.name!!.'
        append("func ").append(func.returnType!!.name!!).append(" ").append(func.name!!.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
        if (func.funcTypes.size != 0) {
            var first = true
            for (funcType in func.funcTypes) {
                append(if (first) "<" else ", ").append(funcType.name!!)
                first = false
            }

            append(">")
        }

        var first = true
        for (param in func.params) {
            val `var` = param as NameExpr
            // TODO Is .type always not null here '.type!!.'
            // TODO-NAME Is .name always not null here '.name!!.'
            append(if (first) "(" else ", ").append(`var`.type!!.name!!).append(" ").append(`var`.name!!)
            first = false
        }

        append(")")
    }

    override fun evalFuncCall(call: FuncExpr) {
        // TODO Is .name always not null here '.name!!.'
        append(call.name!!.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])

        if (call.funcTypes.size != 0) {
            var first = true
            for (funcType in call.funcTypes) {
                // TODO Is .name always not null here '.name!!.'
                append(if (first) "<" else ", ").append(funcType.name!!)
                first = false
            }

            append(">")
        }

        var first = true
        for (arg in call.args) {
            append(if (first) "(" else ", ")
            first = false
            arg.eval(this)
        }

        append(")")
    }

    override fun evalFuncSignature(func: FuncStmt) {

    }

    fun evalJoin(join: JoinExpr) {
        // TODO Is .name always not null here '.name!!.'
        append("join ").append(join.name!!)
        if (join.limit != null) {
            // TODO Is .name always not null here '.name!!.'
            append(" limit ").append(join.limit!!.name!!)
        }
    }

    override fun evalLutDefinition(lut: LutStmt) {
        newline()
        // TODO Is .name always not null here '.name!!.'
        append("lut ").append(lut.name!!).append(" {").newline().indent()

        for (entry in lut.entries) {
            evalLutEntry(entry)
            newline()
        }

        undent().append("}").newline()
    }

    private fun evalLutEntry(entry: LutEntry) {
        var first = true
        for (i in 0 until entry.inputs.length) {
            append(if (first) "" else ",").append(entry.inputs.substring(i, i + 1))
            first = false
        }

        append(" = ")

        first = true
        for (i in 0 until entry.outputs.length) {
            append(if (first) "" else ",").append(entry.outputs.substring(i, i + 1))
            first = false
        }
    }

    override fun evalLutLookup(lookup: LutExpr) {
        // TODO Is .name always not null here '.name!!.'
        append(lookup.name!!)

        var first = true
        for (arg in lookup.args) {
            append(if (first) "[" else ", ")
            first = false
            arg.eval(this)
        }

        append("]")
    }

    override fun evalMerge(merge: MergeExpr) {
        // TODO Is .lhs always not null here '.lhs!!.'
        merge.lhs!!.eval(this)
        if (merge.rhs == null) {
            return
        }

        append(" | ")
        merge.rhs!!.eval(this)
    }

    override fun evalSlice(slice: SliceExpr) {
        // TODO Is .name always not null here '.name!!.'
        append(slice.name!!)

        for (field in slice.fields) {
            append(".").append(field.name!!)
        }

        if (slice.startOffset != null) {
            append("[")
            slice.startOffset!!.eval(this)

            if (slice.endOffset != null) {
                append(" : ")
                slice.endOffset!!.eval(this)
            }

            append("]")
        }
    }

    override fun evalState(state: StateExpr) {
        // TODO Is .stateType always not null here '.stateType!!.'
        // TODO-aND Is .name always not null here '.name!!.'
        append("state ").append(state.stateType!!.name!!).append(" ").append(state.name!!)
    }

    override fun evalSubExpr(sub: BaseSubExpr) {
        if (sub is ConstSubExpr || sub is SubExpr) {
            append("(")
            super.evalSubExpr(sub)
            append(")")
            return
        }

        if (sub is ConstFactor) {
            append(if (sub.negative) "-" else "")
        } else if (sub is FieldExpr) {
            append(sub.name!!).append(" = ")
        }

        super.evalSubExpr(sub)
    }

    private fun evalTemplateDefinition(template: TemplateStmt) {
        newline()

        evalTemplateSignature(template)

        append("{").newline().indent()

        for (type in template.types) {
            type.eval(this)
            newline()
        }

        for (func in template.funcs) {
            func.eval(this)
            newline()
        }

        newline().undent().append("}").newline()
    }

    fun evalTemplateSignature(template: TemplateStmt) {
        // TODO Is .name always not null here '.name!!.'
        append("template ").append(template.name!!)

        var first = true
        for (param in template.params) {
            // TODO Is .name always not null here '.name!!.'
            append(if (first) "<" else ", ").append(param.name!!)
            first = false
        }

        append("> ")
    }

    private fun evalTritStruct(struct: TritStructDef) {
        append("{").newline().indent()

        for (field in struct.fields) {
            field.eval(this)
            newline()
        }

        undent().append("}")
    }

    private fun evalTritVector(vector: TritVectorDef) {
        append("[")
        // TODO Is .typeExpr always not null here '.typeExpr!!.'
        vector.typeExpr!!.eval(this)
        append("]")
    }

    override fun evalType(type: TypeExpr) {
        append(type.name!!).append("{").newline().indent()

        for (field in type.fields) {
            field.eval(this)
            newline()
        }

        undent().append("}")
    }

    override fun evalTypeDefinition(type: TypeStmt) {
        append("type ").append(type.name!!).append(" ")
        if (type.struct != null) {
            evalTritStruct(type.struct!!)
            newline()
            return
        }
        // TODO Is .vector always not null here '.vector!!'
        evalTritVector(type.vector!!)
        newline()
    }

    private fun evalUseDefinition(use: UseStmt) {
        append("use ").append(use.name!!)

        var next = false
        for (typeArgs in use.typeInstantiations) {
            append(if (next) ", " else "")
            next = true

            var first = true
            for (typeArg in typeArgs) {
                // TODO Is .name always not null here '.name!!.'
                append(if (first) "<" else ", ").append(typeArg.name!!)
                first = false
            }

            append(">")
        }
    }

    override fun evalVector(integer: IntegerExpr) {
        // TODO Is .name always not null here '.name!!.'
        append(integer.name!!)
    }
}
