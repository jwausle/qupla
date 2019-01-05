package org.iota.qupla.qupla.expression

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.expression.base.BaseSubExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class PostfixExpr : BaseSubExpr {
    constructor(copy: PostfixExpr) : super(copy) {}

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        when (tokenizer.tokenId()) {
            Token.TOK_FUNC_OPEN -> {
                expr = SubExpr(tokenizer)
                return
            }

            Token.TOK_FLOAT, Token.TOK_NUMBER, Token.TOK_MINUS -> {
                expr = IntegerExpr(tokenizer)
                return
            }
        }

        val varName = expect(tokenizer, Token.TOK_NAME, "variable name")
        name = varName.text

        when (tokenizer.tokenId()) {
            Token.TOK_TEMPL_OPEN, Token.TOK_FUNC_OPEN -> {
                expr = FuncExpr(tokenizer, varName)
                return
            }

            Token.TOK_GROUP_OPEN -> {
                expr = TypeExpr(tokenizer, varName)
                return
            }
        }

        for (i in BaseExpr.scope.indices.reversed()) {
            val `var` = BaseExpr.scope[i]
            if (`var`.name == name) {
                expr = SliceExpr(tokenizer, varName)
                return
            }
        }

        expr = LutExpr(tokenizer, varName)
    }

    override fun clone(): BaseExpr {
        return PostfixExpr(this)
    }

    override fun optimize(): BaseExpr {
        // TODO Is expr always not null here 'expr!!'
        return expr!!
    }
}
