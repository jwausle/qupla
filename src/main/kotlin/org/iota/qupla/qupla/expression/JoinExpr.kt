package org.iota.qupla.qupla.expression

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.expression.constant.ConstNumber
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class JoinExpr : BaseExpr {
    var limit: BaseExpr? = null

    constructor(copy: JoinExpr) : super(copy) {

        limit = clone(copy.limit)
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        expect(tokenizer, Token.TOK_JOIN, "join")

        val varName = expect(tokenizer, Token.TOK_NAME, "environment name")
        name = varName.text

        if (tokenizer.tokenId() == Token.TOK_LIMIT) {
            tokenizer.nextToken()
            limit = ConstNumber(tokenizer)
        }
    }

    override fun analyze() {
        if (limit != null) {
            limit!!.analyze()
        }
    }

    override fun clone(): BaseExpr {
        return JoinExpr(this)
    }

}
