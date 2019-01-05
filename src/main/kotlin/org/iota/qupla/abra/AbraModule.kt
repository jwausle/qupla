package org.iota.qupla.abra

import java.util.ArrayList

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.AbraBlockImport
import org.iota.qupla.abra.block.AbraBlockLut
import org.iota.qupla.abra.block.base.AbraBaseBlock
import org.iota.qupla.qupla.context.QuplaToAbraContext

class AbraModule {
    var blockNr: Int = 0
    var blocks = ArrayList<AbraBaseBlock>()
    var branches = ArrayList<AbraBlockBranch>()
    var imports = ArrayList<AbraBlockImport>()
    var luts = ArrayList<AbraBlockLut>()

    fun addBranch(branch: AbraBlockBranch?) {
        if(branch == null)
            return
        branches.add(branch)
        blocks.add(branch)
    }

    fun addLut(lut: AbraBlockLut) {
        luts.add(lut)
        blocks.add(lut)
    }

    fun addLut(name: String, lookup: String): AbraBlockLut {
        val lut = AbraBlockLut()
        lut.name = name
        lut.lookup = lookup
        addLut(lut)
        return lut
    }

    fun numberBlocks() {
        blockNr = 0
        numberBlocks(imports)
        numberBlocks(luts)
        numberBlocks(branches)
    }

    fun numberBlocks(blocks: ArrayList<out AbraBaseBlock>) {
        for (block in blocks) {
            block.index = blockNr++
        }
    }

    fun optimize(context: QuplaToAbraContext) {
        // determine reference counts for branches and sites
        for (branch in branches) {
            branch.markReferences()
        }

        for (i in branches.indices) {
            val branch = branches[i]
            branch.optimize(context)
        }
    }
}
