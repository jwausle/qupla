package org.iota.qupla.qupla.statement.helper

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class LutEntry : BaseExpr {
    val inputs: String
    val outputs: String

    constructor(copy: LutEntry) : super(copy) {

        inputs = copy.inputs
        outputs = copy.outputs
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        inputs = parseTritList(tokenizer)
        expect(tokenizer, Token.TOK_EQUAL, "'='")
        outputs = parseTritList(tokenizer)
    }

    override fun analyze() {}

    override fun clone(): BaseExpr {
        return LutEntry(this)
    }

    private fun parseTrit(tokenizer: Tokenizer): String {
        val trit = tokenizer.currentToken()
        // TODO Is trit always not null here 'trit!!.'
        if (trit!!.id == Token.TOK_MINUS) {
            tokenizer.nextToken()
            // TODO Is .text always not null here '.text!!'
            return trit.text!!
        }

        expect(tokenizer, Token.TOK_NUMBER, "trit value")
        // TODO Is .text always not null here '.text!!.'
        if (trit.text!!.length != 1 || trit.text!!.get(0) > '1') {
            error(trit, "Invalid trit value")
        }

        return trit.text!!
    }

    private fun parseTritList(tokenizer: Tokenizer): String {
        var trits = parseTrit(tokenizer)
        while (tokenizer.tokenId() == Token.TOK_COMMA) {
            tokenizer.nextToken()
            trits += parseTrit(tokenizer)
        }

        return trits
    }
}
