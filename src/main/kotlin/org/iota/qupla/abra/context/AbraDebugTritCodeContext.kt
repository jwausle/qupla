package org.iota.qupla.abra.context

import org.iota.qupla.abra.AbraModule
import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.AbraBlockImport
import org.iota.qupla.abra.block.AbraBlockLut
import org.iota.qupla.abra.block.site.AbraSiteKnot
import org.iota.qupla.abra.block.site.AbraSiteLatch
import org.iota.qupla.abra.block.site.AbraSiteMerge
import org.iota.qupla.abra.block.site.AbraSiteParam
import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.abra.context.base.AbraTritCodeBaseContext

class AbraDebugTritCodeContext : AbraTritCodeBaseContext() {
    override fun eval(module: AbraModule) {
        module.numberBlocks()

        //TODO add all types to context and write them out
        //     (vector AND struct) name/size/isFloat?
        putInt(0) // version
        putInt(module.luts.size)
        evalBlocks(module.luts)
        putInt(module.branches.size)
        evalBlocks(module.branches)
        putInt(module.imports.size)
        evalBlocks(module.imports)

        //TODO generate tritcode hash so that we can create a function
        //     in the normal tritcode that returns that hash as a constant
    }

    override fun evalBranch(branch: AbraBlockBranch) {
        //TODO origin?
        putString(branch.name)
        evalBranchSites(branch)
    }

    override fun evalImport(imp: AbraBlockImport) {
        //TODO origin?
        putString(imp.name)
    }

    override fun evalKnot(knot: AbraSiteKnot) {
        evalSite(knot)
    }

    override fun evalLatch(latch: AbraSiteLatch) {
        evalSite(latch)
    }

    override fun evalLut(lut: AbraBlockLut) {
        //TODO origin?
        val isUnnamed = lut.name == AbraBlockLut.unnamed(lut.lookup)
        putString(if (isUnnamed) null else lut.name)
    }

    override fun evalMerge(merge: AbraSiteMerge) {
        evalSite(merge)
    }

    override fun evalParam(param: AbraSiteParam) {
        evalSite(param)
    }

    private fun evalSite(site: AbraBaseSite) {
        //TODO origin?
        //TODO stmt?
        //TODO putInt(typeId) (index of origin.typeInfo)
        putString(site.name)
    }
}
