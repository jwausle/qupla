package org.iota.qupla.abra.context

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
import org.iota.qupla.abra.context.base.AbraTritCodeBaseContext

class AbraTritCodeContext : AbraTritCodeBaseContext() {
    override fun eval(module: AbraModule) {
        module.numberBlocks()

        putInt(0) // version
        putInt(module.luts.size)
        evalBlocks(module.luts)
        putInt(module.branches.size)
        evalBlocks(module.branches)
        putInt(module.imports.size)
        evalBlocks(module.imports)
    }

    override fun evalBranch(branch: AbraBlockBranch) {
        // we need a separate temporary buffer to gather everything
        // before we can add the accumulated length and data
        val branchTritCode = AbraTritCodeContext()
        branchTritCode.evalBranchSites(branch)

        // now copy the temporary buffer length and contents
        putInt(branchTritCode.bufferOffset)
        putTrits(String(branchTritCode.buffer, 0, branchTritCode.bufferOffset))
    }

    override fun evalImport(imp: AbraBlockImport) {
        // TODO Is .hash always not null here '.hash!!.'
        putTrits(imp.hash!!)
        putInt(imp.blocks.size)
        for (block in imp.blocks) {
            putInt(block.index)
        }
    }

    override fun evalKnot(knot: AbraSiteKnot) {
        putTrit('-')
        putSiteInputs(knot)
        // TODO Is .block always not null here '.block!!.'
        putInt(knot.block!!.index)
    }

    override fun evalLatch(latch: AbraSiteLatch) {

    }

    override fun evalLut(lut: AbraBlockLut) {
        //TODO convert 27 bct lookup 'trits' to 35 trits
        putTrits(lut.lookup)
    }

    override fun evalMerge(merge: AbraSiteMerge) {
        putTrit('1')
        putSiteInputs(merge)
    }

    override fun evalParam(param: AbraSiteParam) {
        putInt(param.size)
    }

    fun putSiteInputs(merge: AbraSiteMerge) {
        putInt(merge.inputs.size)
        for (input in merge.inputs) {
            putInt(merge.refer(input.index))
        }
    }
}
