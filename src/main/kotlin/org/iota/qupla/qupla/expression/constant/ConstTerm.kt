package org.iota.qupla.qupla.expression.constant

import org.iota.qupla.qupla.expression.base.BaseBinaryExpr
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class ConstTerm : BaseBinaryExpr {
    constructor(copy: ConstTerm) : super(copy) {}

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        var leaf = ConstFactor(tokenizer).optimize()
        while (tokenizer.tokenId() == Token.TOK_MUL || tokenizer.tokenId() == Token.TOK_DIV || tokenizer.tokenId() == Token.TOK_MOD) {
            val branch = ConstTerm(this)
            leaf = connectBranch(tokenizer, leaf, branch)
            branch.rhs = ConstFactor(tokenizer).optimize()
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
                Token.TOK_MUL -> size *= rhs!!.size

                Token.TOK_DIV -> {
                    if (rhs!!.size == 0) {
                        rhs!!.error("Divide by zero in constant expression")
                    }

                    size /= rhs!!.size
                }

                Token.TOK_MOD -> {
                    if (rhs!!.size == 0) {
                        rhs!!.error("Divide by zero in constant expression")
                    }

                    size %= rhs!!.size
                }
            }
        }
    }

    override fun clone(): BaseExpr {
        return ConstTerm(this)
    }
}
