package org.iota.qupla.qupla.expression.base

import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

abstract class BaseBinaryExpr : BaseExpr {
    var lhs: BaseExpr? = null
    var operator: Token? = null
    var rhs: BaseExpr? = null

    protected constructor(copy: BaseBinaryExpr) : super(copy) {
        lhs = clone(copy.lhs)
        operator = copy.operator
        rhs = clone(copy.rhs)
    }

    protected constructor(tokenizer: Tokenizer) : super(tokenizer) {}

    protected fun connectBranch(tokenizer: Tokenizer, leaf: BaseExpr, branch: BaseBinaryExpr): BaseExpr {
        branch.lhs = leaf
        branch.origin = leaf.origin

        branch.operator = tokenizer.currentToken()
        tokenizer.nextToken()

        return branch
    }

    override fun optimize(): BaseExpr {
        // TODO Is lhs always not null here 'lhs!!.'
        return if (rhs == null) lhs!! else this
    }
}
