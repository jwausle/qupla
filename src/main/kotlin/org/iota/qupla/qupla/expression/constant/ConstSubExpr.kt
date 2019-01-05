package org.iota.qupla.qupla.expression.constant

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.expression.base.BaseSubExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class ConstSubExpr : BaseSubExpr {
    constructor(copy: ConstSubExpr) : super(copy) {}

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        expr = ConstExpr(tokenizer).optimize()

        expect(tokenizer, Token.TOK_FUNC_CLOSE, "')'")
    }

    override fun clone(): BaseExpr {
        return ConstSubExpr(this)
    }
}
