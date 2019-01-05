package org.iota.qupla.qupla.parser

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.statement.ExecStmt
import org.iota.qupla.qupla.statement.FuncStmt
import org.iota.qupla.qupla.statement.ImportStmt
import org.iota.qupla.qupla.statement.LutStmt
import org.iota.qupla.qupla.statement.TemplateStmt
import org.iota.qupla.qupla.statement.TypeStmt
import org.iota.qupla.qupla.statement.UseStmt
import java.lang.NullPointerException

class Source(tokenizer: Tokenizer, var pathName: String) : BaseExpr(tokenizer) {

    init {
        // TODO Is module always not null here '.module!!.'
        module!!.currentSource = this

        origin = tokenizer.nextToken()

        while (tokenizer.tokenId() != Token.TOK_EOF) {
            when (tokenizer.tokenId()) {
                Token.TOK_EVAL -> module!!.execs.add(ExecStmt(tokenizer, false))

                Token.TOK_FUNC -> module!!.funcs.add(FuncStmt(tokenizer))

                Token.TOK_IMPORT -> module!!.imports.add(ImportStmt(tokenizer))

                Token.TOK_LUT -> module!!.luts.add(LutStmt(tokenizer))

                Token.TOK_TEMPLATE -> module!!.templates.add(TemplateStmt(tokenizer))

                Token.TOK_TEST -> module!!.execs.add(ExecStmt(tokenizer, true))

                Token.TOK_TYPE -> module!!.types.add(TypeStmt(tokenizer))

                Token.TOK_USE -> module!!.uses.add(UseStmt(tokenizer))

                else -> {
                    val token = tokenizer.currentToken()
                    // TODO Is token always not null here 'token!!.'
                    error(token!!, "Unexpected token: '" + token.text + "'")
                }
            }
        }
    }

    override fun analyze() {}

    override fun clone(): BaseExpr {
        // TODO is NPE instead 'return null!!' the right choice?
        throw NullPointerException("Source.clone is not implemented yet")
    }

    override fun toString(): String {
        return pathName
    }
}
