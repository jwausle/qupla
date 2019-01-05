package org.iota.qupla.qupla.expression

import java.util.ArrayList

import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer
import org.iota.qupla.qupla.statement.LutStmt

class LutExpr : BaseExpr {
    val args = ArrayList<BaseExpr>()
    var lut: LutStmt? = null

    constructor(copy: LutExpr) : super(copy) {

        cloneArray(args, copy.args)
        lut = copy.lut
    }

    constructor(tokenizer: Tokenizer, identifier: Token) : super(tokenizer, identifier) {

        name = identifier.text

        expect(tokenizer, Token.TOK_ARRAY_OPEN, "'['")

        args.add(MergeExpr(tokenizer).optimize())
        while (tokenizer.tokenId() == Token.TOK_COMMA) {
            tokenizer.nextToken()
            args.add(MergeExpr(tokenizer).optimize())
        }

        expect(tokenizer, Token.TOK_ARRAY_CLOSE, "']'")
    }

    override fun analyze() {
        lut = findEntity(LutStmt::class.java, "lut") as LutStmt?
        // TODO Is .lut always not null here '.lut!!.'
        size = lut!!.size

        if (lut!!.inputSize > args.size) {
            error("Missing argument for LUT: $name")
        }

        if (lut!!.inputSize < args.size) {
            error("Extra argument to LUT: $name")
        }

        for (arg in args) {
            arg.analyze()
            if (arg.size != 1) {
                arg.error("LUT argument should be a single trit")
            }
        }
    }

    override fun clone(): BaseExpr {
        return LutExpr(this)
    }

    override fun eval(context: QuplaBaseContext) {
        context.evalLutLookup(this)
    }
}
