package org.iota.qupla.qupla.statement

import java.util.ArrayList

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.expression.constant.ConstTypeName
import org.iota.qupla.qupla.parser.Module
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class UseStmt : BaseExpr {
    var placeHolders = ArrayList<FuncStmt>()
    var template: TemplateStmt? = null
    val typeInstantiations = ArrayList<ArrayList<BaseExpr>>()

    constructor(copy: UseStmt) : super(copy) {

        error("Cannot clone UseStmt")
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        expect(tokenizer, Token.TOK_USE, "use")

        val templateName = expect(tokenizer, Token.TOK_NAME, "template name")
        name = templateName.text

        parseTypeInstantiation(tokenizer)

        while (tokenizer.tokenId() == Token.TOK_COMMA) {
            tokenizer.nextToken()
            parseTypeInstantiation(tokenizer)
        }
    }

    override fun analyze() {
        for (typeArgs in typeInstantiations) {
            for (typeArg in typeArgs) {
                typeArg.analyze()
            }
        }

        template = findEntity(TemplateStmt::class.java, "template") as TemplateStmt
        // TODO Is template always not null here 'template!!.'
        for (i in 0 until template!!.funcs.size * typeInstantiations.size) {
            placeHolders.add(FuncStmt(this))
        }
        // TODO Is module always not null here 'module!!.'
        module!!.funcs.addAll(placeHolders)

        val oldCurrentModule = BaseExpr.currentModule
        BaseExpr.currentModule = module
        BaseExpr.currentUse = this
        BaseExpr.currentUseIndex = 0
        var placeHolderIndex = 0
        for (typeArgs in typeInstantiations) {
            if (template!!.params.size > typeArgs.size) {
                val param = template!!.params[typeArgs.size]
                error("Missing type argument: " + param.name)
            }

            if (template!!.params.size < typeArgs.size) {
                val typeArg = typeArgs[template!!.params.size]
                typeArg.error("Extra type argument: " + typeArg.name)
            }

            generateTypes()

            for (func in template!!.funcs) {
                val useFunc = FuncStmt(func as FuncStmt)
                useFunc.origin = origin
                useFunc.module = module
                useFunc.use = BaseExpr.currentUse
                useFunc.useIndex = BaseExpr.currentUseIndex
                useFunc.analyzeSignature()
                //TODO WTF?
                placeHolders[placeHolderIndex].copyFrom(useFunc)
                placeHolderIndex++
            }

            BaseExpr.currentUseIndex++
        }

        BaseExpr.currentModule = oldCurrentModule
        BaseExpr.currentUse = null
        BaseExpr.currentUseIndex = 0
    }

    override fun clone(): BaseExpr {
        return UseStmt(this)
    }

    fun generateTypes() {
        // set up template types
        // TODO Is template always not null here 'template!!.'
        for (type in template!!.types) {
            type.typeInfo = null
            type.typeInfo = type.clone() as TypeStmt
            // TODO Is .typeInfo always not null here '.typeInfo!!.'
            type.typeInfo!!.analyze()

            type.typeInfo!!.name = template!!.name
            val typeArgs = typeInstantiations[BaseExpr.currentUseIndex]
            for (typeArg in typeArgs) {
                type.typeInfo!!.name += BaseExpr.SEPARATOR + typeArg.name
            }

            type.typeInfo!!.name += BaseExpr.SEPARATOR + type.name
            // TODO Is module always not null here 'module!!.'
            module!!.types.add(type.typeInfo!!)
        }
    }

    private fun parseTypeInstantiation(tokenizer: Tokenizer) {
        val typeArgs = ArrayList<BaseExpr>()
        expect(tokenizer, Token.TOK_TEMPL_OPEN, "<")

        typeArgs.add(ConstTypeName(tokenizer))

        while (tokenizer.tokenId() == Token.TOK_COMMA) {
            tokenizer.nextToken()

            typeArgs.add(ConstTypeName(tokenizer))
        }

        expect(tokenizer, Token.TOK_TEMPL_CLOSE, "',' or '>'")
        typeInstantiations.add(typeArgs)
    }
}
