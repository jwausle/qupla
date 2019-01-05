package org.iota.qupla.qupla.expression.base

import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.parser.Tokenizer

abstract class BaseSubExpr : BaseExpr {
    var expr: BaseExpr? = null

    constructor(copy: BaseSubExpr) : super(copy) {

        expr = clone(copy.expr)
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {}

    override fun analyze() {
        expr!!.analyze()
        size = expr!!.size
        typeInfo = expr!!.typeInfo
    }

    override fun eval(context: QuplaBaseContext) {
        context.evalSubExpr(this)
    }
}
