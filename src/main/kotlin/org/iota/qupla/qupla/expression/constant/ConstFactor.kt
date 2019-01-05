package org.iota.qupla.qupla.expression.constant

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.expression.base.BaseSubExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class ConstFactor : BaseSubExpr {
    var negative: Boolean = false

    constructor(copy: ConstFactor) : super(copy) {
        negative = copy.negative
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        when (tokenizer.tokenId()) {
            Token.TOK_FUNC_OPEN -> {
                expr = ConstSubExpr(tokenizer)
                return
            }

            Token.TOK_NAME -> {
                expr = ConstTypeName(tokenizer)
                return
            }

            Token.TOK_NUMBER -> {
                expr = ConstNumber(tokenizer)
                return
            }
        }

        expect(tokenizer, Token.TOK_MINUS, "name, number, '-', or '('")

        negative = true
        expr = ConstFactor(tokenizer)
    }

    override fun analyze() {
        // TODO Is .expr always not null here '.expr!!.'
        expr!!.analyze()
        size = if (negative) - expr!!.size else expr!!.size
    }

    override fun clone(): BaseExpr {
        return ConstFactor(this)
    }

    override fun optimize(): BaseExpr {
        // TODO Is expr always not null here 'expr!!'
        return if (negative) this else expr!!
    }
}
