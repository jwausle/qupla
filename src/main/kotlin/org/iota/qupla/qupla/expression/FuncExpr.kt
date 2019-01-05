package org.iota.qupla.qupla.expression

import java.util.ArrayList

import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.expression.constant.ConstTypeName
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer
import org.iota.qupla.qupla.statement.FuncStmt

class FuncExpr : BaseExpr {
    val args = ArrayList<BaseExpr>()
    var callIndex: Int = 0
    var func: FuncStmt? = null
    val funcTypes = ArrayList<BaseExpr>()

    constructor(copy: FuncExpr) : super(copy) {

        callIndex = copy.callIndex
        func = copy.func
        cloneArray(funcTypes, copy.funcTypes)
        cloneArray(args, copy.args)
    }

    constructor(tokenizer: Tokenizer, identifier: Token) : super(tokenizer, identifier) {

        name = identifier.text

        if (tokenizer.tokenId() == Token.TOK_TEMPL_OPEN) {
            tokenizer.nextToken()

            funcTypes.add(ConstTypeName(tokenizer))
            while (tokenizer.tokenId() == Token.TOK_COMMA) {
                tokenizer.nextToken()

                funcTypes.add(ConstTypeName(tokenizer))
            }

            expect(tokenizer, Token.TOK_TEMPL_CLOSE, "'>'")
        }

        expect(tokenizer, Token.TOK_FUNC_OPEN, "'('")

        args.add(CondExpr(tokenizer).optimize())

        while (tokenizer.tokenId() == Token.TOK_COMMA) {
            tokenizer.nextToken()

            args.add(CondExpr(tokenizer).optimize())
        }

        expect(tokenizer, Token.TOK_FUNC_CLOSE, "')'")
    }

    override fun analyze() {
        callIndex = BaseExpr.callNr++

        for (funcType in funcTypes) {
            funcType.analyze()
            name += BaseExpr.SEPARATOR + funcType.size
        }

        func = findEntity(FuncStmt::class.java, "func") as FuncStmt?
        // TODO Is .func always not null here '.func!!.'
        size = func!!.size
        typeInfo = func!!.typeInfo

        val params = func!!.params
        if (params.size > args.size) {
            val param = params[args.size]
            error("Missing argument: " + param.name)
        }

        if (params.size < args.size) {
            val arg = args[params.size]
            arg.error("Extra argument to function: " + func!!.name)
        }

        for (i in params.indices) {
            val param = params[i]
            val arg = args[i]
            BaseExpr.constTypeInfo = param.typeInfo
            arg.analyze()
            BaseExpr.constTypeInfo = null
            if (param.size != arg.size) {
                arg.error("Invalid argument size " + param.size + " != " + arg.size)
            }
        }
    }

    override fun clone(): BaseExpr {
        return FuncExpr(this)
    }

    override fun eval(context: QuplaBaseContext) {
        context.evalFuncCall(this)
    }
}
