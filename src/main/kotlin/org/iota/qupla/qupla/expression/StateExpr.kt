package org.iota.qupla.qupla.expression

import org.iota.qupla.helper.TritVector
import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.expression.constant.ConstTypeName
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class StateExpr : BaseExpr {
    var stateType: BaseExpr? = null
    var zero: TritVector? = null

    constructor(copy: StateExpr) : super(copy) {

        stateType = clone(copy.stateType)
        zero = if (copy.zero == null) null else TritVector(copy.zero!!)
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        expect(tokenizer, Token.TOK_STATE, "state")

        stateType = ConstTypeName(tokenizer)

        val varName = expect(tokenizer, Token.TOK_NAME, "variable name")
        name = varName.text

        for (i in BaseExpr.scope.indices.reversed()) {
            val `var` = BaseExpr.scope[i]
            if (`var`.name == name) {
                error("Duplicate variable name: $name")
            }
        }

        stackIndex = BaseExpr.scope.size
        BaseExpr.scope.add(this)
    }

    override fun analyze() {
        stateType!!.analyze()
        size = stateType!!.size
        typeInfo = stateType!!.typeInfo

        zero = TritVector(size, '0')

        BaseExpr.scope.add(this)
    }

    override fun clone(): BaseExpr {
        return StateExpr(this)
    }

    override fun eval(context: QuplaBaseContext) {
        context.evalState(this)
    }
}
