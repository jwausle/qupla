package org.iota.qupla.qupla.expression

import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer
import org.iota.qupla.qupla.statement.TypeStmt

class CondExpr : BaseExpr {
    var condition: BaseExpr? = null
    var falseBranch: BaseExpr? = null
    var trueBranch: BaseExpr? = null

    constructor(copy: CondExpr) : super(copy) {

        condition = clone(copy.condition)
        trueBranch = clone(copy.trueBranch)
        falseBranch = clone(copy.falseBranch)
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        condition = MergeExpr(tokenizer).optimize()

        if (tokenizer.tokenId() == Token.TOK_QUESTION) {
            tokenizer.nextToken()

            trueBranch = MergeExpr(tokenizer).optimize()

            expect(tokenizer, Token.TOK_COLON, "':'")

            if (tokenizer.tokenId() == Token.TOK_NULL) {
                tokenizer.nextToken()
                return
            }

            falseBranch = CondExpr(tokenizer).optimize()
        }
    }

    override fun analyze() {
        if (trueBranch == null) {
            // should have been optimized away
            condition!!.analyze()
            size = condition!!.size
            typeInfo = condition!!.typeInfo
            return
        }

        val saved = BaseExpr.constTypeInfo
        BaseExpr.constTypeInfo = null
        condition!!.analyze()

        if (condition!!.size != 1) {
            condition!!.error("Condition should be single trit")
        }

        BaseExpr.constTypeInfo = saved
        trueBranch!!.analyze()

        if (falseBranch != null) {
            BaseExpr.constTypeInfo = saved
            falseBranch!!.analyze()

            if (trueBranch!!.size != falseBranch!!.size) {
                falseBranch!!.error("Conditional branches size mismatch")
            }
        }

        size = trueBranch!!.size
        typeInfo = trueBranch!!.typeInfo
    }

    override fun clone(): BaseExpr {
        return CondExpr(this)
    }

    override fun eval(context: QuplaBaseContext) {
        context.evalConditional(this)
    }

    override fun optimize(): BaseExpr {
        return if (trueBranch == null) {
            // TODO Is condition always not null here 'condition!!'
            condition!!
        } else super.optimize()

    }
}
