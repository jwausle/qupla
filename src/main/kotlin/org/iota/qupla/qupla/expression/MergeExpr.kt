package org.iota.qupla.qupla.expression

import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.expression.base.BaseBinaryExpr
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class MergeExpr : BaseBinaryExpr {
    constructor(copy: MergeExpr) : super(copy) {}

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        var leaf = ConcatExpr(tokenizer).optimize()
        while (tokenizer.tokenId() == Token.TOK_MERGE) {
            val branch = MergeExpr(this)
            leaf = connectBranch(tokenizer, leaf, branch)
            branch.rhs = ConcatExpr(tokenizer).optimize()
        }

        lhs = leaf
    }

    override fun analyze() {
        // TODO Is .lhs always not null here '.lhs!!.'
        lhs!!.analyze()
        size = lhs!!.size

        if (rhs != null) {
            rhs!!.analyze()
            if (rhs!!.size != size) {
                rhs!!.error("Invalid merge input size")
            }
        }
    }

    override fun clone(): BaseExpr {
        return MergeExpr(this)
    }

    override fun eval(context: QuplaBaseContext) {
        context.evalMerge(this)
    }
}
