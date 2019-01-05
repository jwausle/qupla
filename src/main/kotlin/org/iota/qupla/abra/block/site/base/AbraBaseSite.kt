package org.iota.qupla.abra.block.site.base

import org.iota.qupla.abra.context.AbraPrintContext
import org.iota.qupla.abra.context.base.AbraBaseContext
import org.iota.qupla.qupla.expression.base.BaseExpr

abstract class AbraBaseSite {

    var index: Int = 0
    var isLatch: Boolean = false
    var name: String? = null
    var nullifyFalse: AbraBaseSite? = null
    var nullifyTrue: AbraBaseSite? = null
    var origin: BaseExpr? = null
    var references: Int = 0
    var size: Int = 0
    var stmt: BaseExpr? = null
    var varName: String? = null //TODO should be able to remove this

    abstract fun eval(context: AbraBaseContext)

    fun from(expr: BaseExpr) {
        origin = expr
        name = expr.name
        size = expr.size
    }

    fun hasNullifier(): Boolean {
        return nullifyFalse != null || nullifyTrue != null
    }

    open fun markReferences() {
        if (nullifyFalse != null) {
            nullifyFalse!!.references++
        }

        if (nullifyTrue != null) {
            nullifyTrue!!.references++
        }
    }

    fun refer(site: Int): Int {
        //    if (site < index)
        //    {
        //      return index - 1 - site;
        //    }

        return site
    }

    override fun toString(): String {
        val oldString = printer.string
        printer.string = String(CharArray(0))
        eval(printer)
        val ret = printer.string
        printer.string = oldString
        return ret?: ""
    }

    companion object {
        private val printer = AbraPrintContext()
    }
}
