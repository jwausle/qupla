package org.iota.qupla.qupla.statement

import org.iota.qupla.helper.TritVector
import org.iota.qupla.qupla.expression.FuncExpr
import org.iota.qupla.qupla.expression.IntegerExpr
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class ExecStmt : BaseExpr {
    var expected: IntegerExpr? = null
    var expr: BaseExpr

    private constructor(copy: ExecStmt) : super(copy) {
        expected = copy.expected
        expr = copy.expr
    }

    constructor(tokenizer: Tokenizer, test: Boolean) : super(tokenizer) {

        tokenizer.nextToken()
        this.module = tokenizer.module

        if (test) {
            expected = IntegerExpr(tokenizer)
            expected!!.expect(tokenizer, Token.TOK_EQUAL, "'='")
        }

        val funcName = expect(tokenizer, Token.TOK_NAME, "func name")
        expr = FuncExpr(tokenizer, funcName)
    }

    override fun analyze() {
        // make sure we always start at call index zero,
        // or else state variables won't work correctly
        val saveCallNr = BaseExpr.callNr
        BaseExpr.callNr = 0

        expr.analyze()
        typeInfo = expr.typeInfo

        if (expected != null) {
            // make sure expected and expr are same type (integer/float)
            BaseExpr.constTypeInfo = expr.typeInfo
            expected!!.analyze()
            BaseExpr.constTypeInfo = null
        }

        BaseExpr.callNr = saveCallNr
    }

    override fun clone(): BaseExpr {
        return ExecStmt(this)
    }

    fun succeed(result: TritVector): Boolean {
        if (expected!!.vector == result) {
            return true
        }

        // TODO Is .typeInfo always not null here '.typeInfo!!.'
        if (!typeInfo!!.isFloat || expected!!.vector.size() != result.size()) {
            return false
        }

        // when it's a float allow least significant bit to differ
        val size = result.size() - 1
        return expected!!.vector.slice(1, size) == result.slice(1, size)
    }
}
