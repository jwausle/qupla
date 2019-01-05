package org.iota.qupla.abra.context.base

import java.util.ArrayList
import java.util.HashMap

import org.iota.qupla.abra.AbraModule
import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.AbraBlockImport
import org.iota.qupla.abra.block.AbraBlockLut
import org.iota.qupla.abra.block.base.AbraBaseBlock
import org.iota.qupla.abra.block.site.AbraSiteKnot
import org.iota.qupla.abra.block.site.AbraSiteLatch
import org.iota.qupla.abra.block.site.AbraSiteMerge
import org.iota.qupla.abra.block.site.AbraSiteParam
import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.exception.CodeException
import org.iota.qupla.helper.BaseContext
import org.iota.qupla.qupla.context.QuplaToAbraContext
import org.iota.qupla.qupla.expression.base.BaseExpr

abstract class AbraBaseContext : BaseContext() {

    protected fun error(text: String) {
        throw CodeException(text)
    }

    open fun eval(context: QuplaToAbraContext, expr: BaseExpr) {}

    open fun eval(module: AbraModule) {
        module.numberBlocks()

        evalBlocks(module.imports)
        evalBlocks(module.luts)
        evalBlocks(module.branches)
    }

    protected fun evalBlocks(blocks: ArrayList<out AbraBaseBlock>) {
        for (block in blocks) {
            block.eval(this)
        }
    }

    abstract fun evalBranch(branch: AbraBlockBranch)

    abstract fun evalImport(imp: AbraBlockImport)

    abstract fun evalKnot(knot: AbraSiteKnot)

    abstract fun evalLatch(latch: AbraSiteLatch)

    abstract fun evalLut(lut: AbraBlockLut)

    abstract fun evalMerge(merge: AbraSiteMerge)

    abstract fun evalParam(param: AbraSiteParam)

    protected fun evalSites(sites: ArrayList<out AbraBaseSite>) {
        for (site in sites) {
            site.eval(this)
        }
    }

    companion object {
        val indexFromTrits = HashMap<String, Int>()
        val lutIndexes: Array<String> = arrayOf("---", "0--", "1--", "-0-", "00-", "10-", "-1-", "01-", "11-", "--0", "0-0", "1-0", "-00", "000", "100", "-10", "010", "110", "--1", "0-1", "1-1", "-01", "001", "101", "-11", "011", "111")

        init {
            for (i in 0..26) {
                indexFromTrits[lutIndexes[i]] = i
            }
        }
    }
}
