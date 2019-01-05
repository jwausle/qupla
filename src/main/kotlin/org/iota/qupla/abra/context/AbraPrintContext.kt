package org.iota.qupla.abra.context

import java.util.ArrayList

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
import org.iota.qupla.abra.context.base.AbraBaseContext
import org.iota.qupla.qupla.expression.base.BaseExpr

class AbraPrintContext : AbraBaseContext() {
    var type = "site"

    fun appendSiteInputs(merge: AbraSiteMerge) {
        append("(")

        var first = true
        for (input in merge.inputs) {
            append(if (first) "" else ", ").append("" + input.index)
            first = false
        }

        append(")")
    }

    override fun eval(module: AbraModule) {
        module.blockNr = 0
        module.numberBlocks(module.blocks)

        fileOpen("Abra.txt")

        evalBlocks(module.imports)
        evalBlocks(module.luts)
        evalBlocks(module.branches)

        fileClose()
    }

    private fun evalBlock(block: AbraBaseBlock) {
        newline()
        if (block.origin != null) {
            append("" + block.origin).newline()
        }

        append("// " + block.toString())
    }

    override fun evalBranch(branch: AbraBlockBranch) {
        branch.numberSites()

        evalBlock(branch)

        newline().indent()

        evalBranchSites(branch.inputs, "input")
        evalBranchSites(branch.sites, "body")
        evalBranchSites(branch.outputs, "output")
        evalBranchSites(branch.latches, "latch")

        undent()
    }

    private fun evalBranchSites(sites: ArrayList<out AbraBaseSite>, type: String) {
        this.type = type
        for (site in sites) {
            site.eval(this)
            newline()
        }
    }

    override fun evalImport(imp: AbraBlockImport) {
        evalBlock(imp)
    }

    override fun evalKnot(knot: AbraSiteKnot) {
        evalSite(knot)

        append("knot")
        appendSiteInputs(knot)
        append(" " + knot.block)
    }

    override fun evalLatch(latch: AbraSiteLatch) {
        evalSite(latch)

        append("latch " + latch.name + "[" + latch.size + "]")
    }

    override fun evalLut(lut: AbraBlockLut) {
        evalBlock(lut)

        append("// lut block " + lut.index)
        append(" // " + lut.lookup)
        append(" // " + lut.name + "[]").newline()
    }

    override fun evalMerge(merge: AbraSiteMerge) {
        evalSite(merge)

        append("merge")
        appendSiteInputs(merge)
    }

    override fun evalParam(param: AbraSiteParam) {
        evalSite(param)

        append("param " + param.name + "[" + param.size + "]")
    }

    private fun evalSite(site: AbraBaseSite) {
        var stmt: BaseExpr? = site.stmt
        while (stmt != null) {
            newline().append("" + stmt).newline()
            stmt = stmt.next
        }

        var nullifyIndex = " "
        if (site.nullifyTrue != null) {
            nullifyIndex = "T" + site.nullifyTrue!!.index
        }

        if (site.nullifyFalse != null) {
            nullifyIndex = "F" + site.nullifyFalse!!.index
        }

        append("// " + site.index + " ").append(nullifyIndex)
        append(" " + site.references + " " + type + "(" + site.size + "): ")
    }
}
