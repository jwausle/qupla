package org.iota.qupla.qupla.expression

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.expression.constant.ConstNumber
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class AffectExpr : BaseExpr {
    var delay: BaseExpr? = null

    constructor(copy: AffectExpr) : super(copy) {

        delay = clone(copy.delay)
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        expect(tokenizer, Token.TOK_AFFECT, "affect")

        val varName = expect(tokenizer, Token.TOK_NAME, "environment name")
        name = varName.text

        if (tokenizer.tokenId() == Token.TOK_DELAY) {
            tokenizer.nextToken()
            delay = ConstNumber(tokenizer)
        }
    }

    override fun analyze() {
        if (delay != null) {
            delay!!.analyze()
        }
    }

    override fun clone(): BaseExpr {
        return AffectExpr(this)
    }
}
