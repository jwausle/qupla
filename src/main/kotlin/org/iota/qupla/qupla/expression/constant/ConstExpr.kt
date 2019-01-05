package org.iota.qupla.qupla.expression.constant

import org.iota.qupla.qupla.expression.base.BaseBinaryExpr
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class ConstExpr : BaseBinaryExpr {
    constructor(copy: ConstExpr) : super(copy) {}

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        var leaf = ConstTerm(tokenizer).optimize()
        while (tokenizer.tokenId() == Token.TOK_PLUS || tokenizer.tokenId() == Token.TOK_MINUS) {
            val branch = ConstExpr(this)
            leaf = connectBranch(tokenizer, leaf, branch)
            branch.rhs = ConstTerm(tokenizer).optimize()
        }

        lhs = leaf
    }

    override fun analyze() {
        // TODO Is .lhs always not null here '.lhs!!.'
        lhs!!.analyze()
        size = lhs!!.size

        if (rhs != null) {
            rhs!!.analyze()
            // TODO Is operator always not null here 'operator!!.'
            when (operator!!.id) {
                Token.TOK_PLUS -> size += rhs!!.size

                Token.TOK_MINUS -> size -= rhs!!.size
            }
        }
    }

    override fun clone(): BaseExpr {
        return ConstExpr(this)
    }
}
