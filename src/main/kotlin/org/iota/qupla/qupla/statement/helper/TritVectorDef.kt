package org.iota.qupla.qupla.statement.helper

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.expression.constant.ConstExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class TritVectorDef : BaseExpr {
    var typeExpr: BaseExpr? = null

    constructor(copy: TritVectorDef) : super(copy) {

        typeExpr = clone(copy.typeExpr)
    }

    constructor(tokenizer: Tokenizer, identifier: Token?) : super(tokenizer, identifier) {

        name = identifier?.text

        expect(tokenizer, Token.TOK_ARRAY_OPEN, "'['")

        typeExpr = ConstExpr(tokenizer).optimize()

        expect(tokenizer, Token.TOK_ARRAY_CLOSE, "']'")
    }

    override fun analyze() {
        typeExpr!!.analyze()
        typeInfo = typeExpr!!.typeInfo
        size = typeExpr!!.size
        if (size <= 0) {
            error("Invalid trit vector size value")
        }
    }

    override fun clone(): BaseExpr {
        return TritVectorDef(this)
    }
}
