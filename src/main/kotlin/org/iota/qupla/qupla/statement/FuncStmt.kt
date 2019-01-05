package org.iota.qupla.qupla.statement

import java.util.ArrayList

import org.iota.qupla.helper.TritVector
import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.expression.AffectExpr
import org.iota.qupla.qupla.expression.AssignExpr
import org.iota.qupla.qupla.expression.CondExpr
import org.iota.qupla.qupla.expression.JoinExpr
import org.iota.qupla.qupla.expression.NameExpr
import org.iota.qupla.qupla.expression.StateExpr
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.expression.constant.ConstTypeName
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class FuncStmt : BaseExpr {
    var anyNull: Boolean = false
    val assignExprs = ArrayList<BaseExpr>()
    val envExprs = ArrayList<BaseExpr>()
    val funcTypes = ArrayList<BaseExpr>()
    var nullReturn: TritVector? = null
    val params = ArrayList<BaseExpr>()
    var returnExpr: BaseExpr? = null
    var returnType: BaseExpr? = null
    val stateExprs = ArrayList<BaseExpr>()
    var use: UseStmt? = null
    var useIndex: Int = 0

    constructor(use: UseStmt) {
        // this FuncStmt is a placeholder for template function (required for make references before UseStmt is analyzed)
        this.use = use
        this.module = use.module
        name = ""
    }

    constructor(copy: FuncStmt) : super(copy) {

        anyNull = copy.anyNull
        returnType = clone(copy.returnType)
        cloneArray(funcTypes, copy.funcTypes)
        cloneArray(params, copy.params)
        cloneArray(envExprs, copy.envExprs)
        cloneArray(stateExprs, copy.stateExprs)
        cloneArray(assignExprs, copy.assignExprs)
        returnExpr = clone(copy.returnExpr)
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        expect(tokenizer, Token.TOK_FUNC, "func")

        returnType = ConstTypeName(tokenizer)

        val funcName = expect(tokenizer, Token.TOK_NAME, "function name")
        name = funcName.text

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

        parseParam(tokenizer)

        while (tokenizer.tokenId() == Token.TOK_COMMA) {
            tokenizer.nextToken()

            parseParam(tokenizer)
        }

        expect(tokenizer, Token.TOK_FUNC_CLOSE, "',' or ')'")

        expect(tokenizer, Token.TOK_GROUP_OPEN, "'{'")

        parseBody(tokenizer)
        stackIndex = BaseExpr.scope.size
        BaseExpr.scope.clear()
    }

    override fun analyze() {
        BaseExpr.callNr = 0

        // if this function has an associated use statement
        // then we should replace placeholders with actual types
        BaseExpr.currentUse = use
        BaseExpr.currentUseIndex = useIndex

        if (use != null) {
            // find the template types we created during signature analysis
            // TODO Is .template always not null here '.template!!.'
            for (type in use!!.template!!.types) {
                var typeName = use!!.template!!.name
                val typeArgs = use!!.typeInstantiations[BaseExpr.currentUseIndex]
                for (typeArg in typeArgs) {
                    typeName += BaseExpr.SEPARATOR + typeArg.name
                }

                typeName += BaseExpr.SEPARATOR + type.name
                type.typeInfo = null
                // TODO Is .module always not null here '.module!!.'
                for (moduleType in use!!.module!!.types) {
                    if (moduleType.name == typeName) {
                        type.typeInfo = moduleType
                        break
                    }
                }

                if (type.typeInfo == null) {
                    error("WTF? failed finding type: " + typeName + " in " + use!!.module)
                }
            }
        }

        BaseExpr.scope.addAll(params)

        for (envExpr in envExprs) {
            envExpr.analyze()
        }

        for (stateExpr in stateExprs) {
            stateExpr.analyze()
        }

        for (assignExpr in assignExprs) {
            assignExpr.analyze()
        }

        BaseExpr.constTypeInfo = typeInfo
        returnExpr!!.analyze()
        BaseExpr.constTypeInfo = null

        if (returnExpr!!.size != returnType!!.size) {
            returnExpr!!.error("Return type size mismatch")
        }

        BaseExpr.scope.clear()

        BaseExpr.currentUse = null
        BaseExpr.currentUseIndex = 0
    }

    fun analyzeSignature() {
        for (funcType in funcTypes) {
            funcType.analyze()
            name += BaseExpr.SEPARATOR + funcType.size
        }

        for (param in params) {
            param.analyze()
        }

        returnType!!.analyze()
        size = returnType!!.size
        typeInfo = returnType!!.typeInfo

        nullReturn = TritVector(size, '@')

        // TODO Is .module always not null here '.module!!.'
        for (func in module!!.funcs) {
            if (name == func.name && func !== this) {
                error("Duplicate function name: $name")
            }
        }
    }

    override fun clone(): BaseExpr {
        return FuncStmt(this)
    }

    fun copyFrom(copy: FuncStmt) {
        // replace placeholder with template function instantiated by UseStmt
        // constructor FuncStmt(final FuncStmt copy) cannot be used here, as that would create a new object
        module = copy.module
        name = copy.name
        origin = copy.origin
        size = copy.size
        stackIndex = copy.stackIndex
        typeInfo = copy.typeInfo
        anyNull = copy.anyNull
        use = copy.use
        useIndex = copy.useIndex
        returnType = clone(copy.returnType)
        nullReturn = copy.nullReturn
        cloneArray(funcTypes, copy.funcTypes)
        cloneArray(params, copy.params)
        cloneArray(envExprs, copy.envExprs)
        cloneArray(stateExprs, copy.stateExprs)
        cloneArray(assignExprs, copy.assignExprs)
        returnExpr = clone(copy.returnExpr)
    }

    override fun eval(context: QuplaBaseContext) {
        for (stateExpr in stateExprs) {
            stateExpr.eval(context)
        }

        for (assignExpr in assignExprs) {
            assignExpr.eval(context)
        }

        returnExpr!!.eval(context)
    }

    private fun parseBody(tokenizer: Tokenizer) {
        while (tokenizer.tokenId() == Token.TOK_JOIN) {
            envExprs.add(JoinExpr(tokenizer))
        }

        while (tokenizer.tokenId() == Token.TOK_AFFECT) {
            envExprs.add(AffectExpr(tokenizer))
        }

        while (tokenizer.tokenId() == Token.TOK_STATE) {
            stateExprs.add(StateExpr(tokenizer))
        }

        while (tokenizer.tokenId() != Token.TOK_RETURN) {
            assignExprs.add(AssignExpr(tokenizer))
        }

        tokenizer.nextToken()

        returnExpr = CondExpr(tokenizer).optimize()

        expect(tokenizer, Token.TOK_GROUP_CLOSE, "'}'")
    }

    private fun parseParam(tokenizer: Tokenizer) {
        val paramType = ConstTypeName(tokenizer)

        val param = NameExpr(tokenizer, "param name")
        param.type = paramType

        param.stackIndex = BaseExpr.scope.size
        params.add(param)
        BaseExpr.scope.add(param)
    }

    override fun toStringify() {
        BaseExpr.printer.evalFuncBodySignature(this)
    }
}
