package org.iota.qupla.qupla.expression

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.expression.base.BaseSubExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class FieldExpr : BaseSubExpr {
    constructor(copy: FieldExpr) : super(copy) {}

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        val fieldName = expect(tokenizer, Token.TOK_NAME, "field name")
        name = fieldName.text

        expect(tokenizer, Token.TOK_EQUAL, "'='")

        expr = CondExpr(tokenizer).optimize()
    }

    override fun clone(): BaseExpr {
        return FieldExpr(this)
    }
}
