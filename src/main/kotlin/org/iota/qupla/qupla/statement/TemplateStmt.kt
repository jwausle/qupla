package org.iota.qupla.qupla.statement

import java.util.ArrayList

import org.iota.qupla.qupla.expression.NameExpr
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class TemplateStmt : BaseExpr {
    var analyzed: Boolean = false
    var funcs = ArrayList<BaseExpr>()
    val params = ArrayList<BaseExpr>()
    val types = ArrayList<BaseExpr>()

    constructor(copy: TemplateStmt) : super(copy) {

        params.addAll(copy.params)
        cloneArray(types, copy.types)
        cloneArray(funcs, copy.funcs)
        this.analyzed = copy.analyzed
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        tokenizer.nextToken()

        val templateName = expect(tokenizer, Token.TOK_NAME, "template name")
        name = templateName.text
        // TODO Is .module always not null here '.module!!.'
        module!!.checkDuplicateName(module!!.templates, this)

        expect(tokenizer, Token.TOK_TEMPL_OPEN, "<")

        params.add(NameExpr(tokenizer, "placeholder type name"))
        while (tokenizer.tokenId() == Token.TOK_COMMA) {
            tokenizer.nextToken()

            params.add(NameExpr(tokenizer, "placeholder type name"))
        }

        expect(tokenizer, Token.TOK_TEMPL_CLOSE, "',' or '>'")

        expect(tokenizer, Token.TOK_GROUP_OPEN, "'{'")

        while (tokenizer.tokenId() == Token.TOK_TYPE) {
            types.add(TypeStmt(tokenizer))
        }

        while (tokenizer.tokenId() == Token.TOK_FUNC) {
            funcs.add(FuncStmt(tokenizer))
        }

        expect(tokenizer, Token.TOK_GROUP_CLOSE, "'}'")
    }

    override fun analyze() {
        // just to avoid findEntity calling analyze again
        size = params.size
        analyzed = true
    }

    override fun clone(): BaseExpr {
        return TemplateStmt(this)
    }

    override fun toStringify() {
        BaseExpr.printer.evalTemplateSignature(this)

        if (funcs.size == 1) {
            val func = funcs[0] as FuncStmt
            BaseExpr.printer.evalFuncBodySignature(func)
            return
        }

        val first = true
        for (func in funcs) {
            // TODO Is .name always not null here '.name!!.'
            BaseExpr.printer.append(if (first) "{ " else ", ").append(func.name!!)
        }

        BaseExpr.printer.append(" }")
    }
}
