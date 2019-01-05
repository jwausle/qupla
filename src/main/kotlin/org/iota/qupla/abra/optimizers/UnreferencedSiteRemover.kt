package org.iota.qupla.abra.optimizers

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.site.AbraSiteMerge
import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.abra.optimizers.base.BaseOptimizer
import org.iota.qupla.qupla.context.QuplaToAbraContext
import org.iota.qupla.qupla.expression.base.BaseExpr

class UnreferencedSiteRemover(context: QuplaToAbraContext, branch: AbraBlockBranch) : BaseOptimizer(context, branch) {
    init {
        reverse = true
    }

    private fun moveSiteStmtToNextSite(stmt: BaseExpr?) {
        if (stmt == null) {
            return
        }

        // link statement(s) to next body site or to first output site
        val useBody = index + 1 < branch.sites.size
        val site = if (useBody) branch.sites[index + 1] else branch.outputs[0]
        var last: BaseExpr = stmt
        while (last.next != null) {
            // TODO Is next always not null here 'next!!'
            last = last.next!!
        }

        last.next = site.stmt
        site.stmt = stmt
    }

    override fun processSite(site: AbraSiteMerge) {
        if (site.references != 0 || site.hasNullifier()) {
            return
        }

        updateReferenceCounts(site)

        moveSiteStmtToNextSite(site.stmt)
        branch.sites.removeAt(index)
    }

    override fun run() {
        index = branch.sites.size - 1
        while (index >= 0) {
            processSite(branch.sites[index] as AbraSiteMerge)
            index--
        }
    }

    private fun updateReferenceCounts(site: AbraSiteMerge) {
        for (input in site.inputs) {
            input.references--
        }

        site.inputs.clear()

        if (site.nullifyFalse != null) {
            site.nullifyFalse!!.references--
            site.nullifyFalse = null
        }

        if (site.nullifyTrue != null) {
            site.nullifyTrue!!.references--
            site.nullifyTrue = null
        }
    }
}
