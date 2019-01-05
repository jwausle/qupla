package org.iota.qupla.qupla.expression

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.expression.base.BaseSubExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class SubExpr : BaseSubExpr {
    constructor(copy: SubExpr) : super(copy) {}

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        expect(tokenizer, Token.TOK_FUNC_OPEN, "'('")

        expr = CondExpr(tokenizer).optimize()

        expect(tokenizer, Token.TOK_FUNC_CLOSE, "')'")
    }

    override fun clone(): BaseExpr {
        return SubExpr(this)
    }
}
