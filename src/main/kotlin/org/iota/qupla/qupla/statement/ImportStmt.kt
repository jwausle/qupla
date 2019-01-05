package org.iota.qupla.qupla.statement

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Module
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class ImportStmt : BaseExpr {
    var importModule: Module? = null

    constructor(copy: ImportStmt) : super(copy) {
        importModule = copy.importModule
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        expect(tokenizer, Token.TOK_IMPORT, "import")

        val firstPart = expect(tokenizer, Token.TOK_NAME, "module name")
        name = firstPart.text
    }

    override fun analyze() {
        // TODO Is name always not null here 'name!!'
        importModule = Module.parse(name!!)
        size = 1
    }

    override fun clone(): BaseExpr {
        return ImportStmt(this)
    }
}
