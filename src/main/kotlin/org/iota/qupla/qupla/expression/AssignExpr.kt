package org.iota.qupla.qupla.expression

import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer
import java.util.*

class AssignExpr : BaseExpr {

    var expr: BaseExpr? = null
    var stateIndex: Int = 0

    constructor(copy: AssignExpr) : super(copy) {

        expr = clone(copy.expr)
        stateIndex = copy.stateIndex
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        val varName = expect(tokenizer, Token.TOK_NAME, "variable name")
        name = varName.text

        for (i in BaseExpr.scope.indices.reversed()) {
            val `var` = BaseExpr.scope[i]
            if (`var`.name == name) {
                if (`var` is StateExpr) {
                    stateIndex = i
                    break
                }

                error("Duplicate variable name: $name")
            }
        }

        expect(tokenizer, Token.TOK_EQUAL, "'='")

        expr = CondExpr(tokenizer).optimize()
        stackIndex = BaseExpr.scope.size
        BaseExpr.scope.add(this)

        if (useBreak && expr!!.name != null && expr!!.name == "break") {
            val breakPoint = 0
            Objects.requireNonNull(breakPoint,"Unused QuplaToAbraContext.breakPoint=$breakPoint variable")
        }
    }

    override fun analyze() {
        BaseExpr.constTypeInfo = null
        if (stateIndex != 0) {
            val stateVar = BaseExpr.scope[stateIndex]
            BaseExpr.constTypeInfo = stateVar.typeInfo
        }

        expr!!.analyze()
        if (size != 0 && size != expr!!.size) {
            expr!!.error("Expression size mismatch")
        }

        size = expr!!.size
        typeInfo = expr!!.typeInfo

        if (stateIndex != 0) {
            val stateVar = BaseExpr.scope[stateIndex]
            if (stateVar.size != size) {
                expr!!.error("State variable size mismatch")
            }
        }

        BaseExpr.scope.add(this)
    }

    override fun clone(): BaseExpr {
        return AssignExpr(this)
    }

    override fun eval(context: QuplaBaseContext) {
        context.evalAssign(this)
    }

    companion object {
        private val useBreak = false
    }
}
