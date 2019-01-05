package org.iota.qupla.qupla.statement.helper

import java.util.ArrayList

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class TritStructDef : BaseExpr {
    val fields = ArrayList<BaseExpr>()

    constructor(copy: TritStructDef) : super(copy) {

        cloneArray(fields, copy.fields)
    }

    constructor(tokenizer: Tokenizer, identifier: Token) : super(tokenizer, identifier) {

        name = identifier.text

        tokenizer.nextToken()
        do {
            val fieldName = expect(tokenizer, Token.TOK_NAME, "field name")

            fields.add(TritVectorDef(tokenizer, fieldName))
        } while (tokenizer.tokenId() != Token.TOK_GROUP_CLOSE)

        tokenizer.nextToken()
    }

    override fun analyze() {
        for (field in fields) {
            field.analyze()
            size += field.size
        }
    }

    override fun clone(): BaseExpr {
        return TritStructDef(this)
    }
}
