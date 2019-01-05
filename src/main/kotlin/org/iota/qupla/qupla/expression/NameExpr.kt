package org.iota.qupla.qupla.expression

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class NameExpr : BaseExpr {
    var type: BaseExpr? = null

    constructor(copy: NameExpr) : super(copy) {

        type = clone(copy.type)
    }

    constructor(tokenizer: Tokenizer, what: String) : super(tokenizer) {

        val varName = expect(tokenizer, Token.TOK_NAME, what)
        name = varName.text
    }

    override fun analyze() {
        if (type != null) {
            type!!.analyze()
            typeInfo = type!!.typeInfo
            size = type!!.size
        }
    }

    override fun clone(): BaseExpr {
        return NameExpr(this)
    }
}
