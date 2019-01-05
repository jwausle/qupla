package org.iota.qupla.qupla.expression.constant

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class ConstNumber : BaseExpr {
    constructor(copy: ConstNumber) : super(copy) {}

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        val number = expect(tokenizer, Token.TOK_NUMBER, "number")
        name = number.text
    }

    override fun analyze() {
        size = Integer.parseInt(name)
    }

    override fun clone(): BaseExpr {
        return ConstNumber(this)
    }
}
