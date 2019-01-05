package org.iota.qupla.abra.block.base

import org.iota.qupla.abra.context.base.AbraBaseContext
import org.iota.qupla.helper.TritVector
import org.iota.qupla.qupla.context.QuplaToAbraContext
import org.iota.qupla.qupla.expression.base.BaseExpr

abstract class AbraBaseBlock {

    var analyzed: Boolean = false
    var constantValue: TritVector? = null
    var index: Int = 0
    var name: String? = null
    var origin: BaseExpr? = null
    var specialType: Int = 0

    abstract fun eval(context: AbraBaseContext)

    open fun markReferences() {}

    open fun optimize(context: QuplaToAbraContext) {}

    open fun size(): Int {
        return 1
    }

    override fun toString(): String {
        return "block $index // $name"
    }

    companion object {
        val TYPE_CONSTANT = 3
        val TYPE_NULLIFY_FALSE = 2
        val TYPE_NULLIFY_TRUE = 1
        val TYPE_SLICE = 4
    }
}
