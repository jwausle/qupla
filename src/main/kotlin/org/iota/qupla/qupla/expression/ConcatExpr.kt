package org.iota.qupla.qupla.expression

import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.expression.base.BaseBinaryExpr
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class ConcatExpr : BaseBinaryExpr {
    constructor(copy: ConcatExpr) : super(copy) {}

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        var leaf = PostfixExpr(tokenizer).optimize()
        while (tokenizer.tokenId() == Token.TOK_CONCAT) {
            val branch = ConcatExpr(this)
            leaf = connectBranch(tokenizer, leaf, branch)
            branch.rhs = PostfixExpr(tokenizer).optimize()
        }

        lhs = leaf
    }

    override fun analyze() {
        if (rhs != null) {
            BaseExpr.constTypeInfo = null
        }
        // TODO Is .lhs always not null here '.lhs!!.'
        lhs!!.analyze()
        size = lhs!!.size

        if (rhs != null) {
            rhs!!.analyze()
            size += rhs!!.size
        }
    }

    override fun clone(): BaseExpr {
        return ConcatExpr(this)
    }

    override fun eval(context: QuplaBaseContext) {
        context.evalConcat(this)
    }
}
