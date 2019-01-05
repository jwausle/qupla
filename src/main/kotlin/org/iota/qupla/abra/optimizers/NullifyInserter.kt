package org.iota.qupla.abra.optimizers

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.site.AbraSiteKnot
import org.iota.qupla.abra.block.site.AbraSiteMerge
import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.abra.optimizers.base.BaseOptimizer
import org.iota.qupla.qupla.context.QuplaToAbraContext

class NullifyInserter(context: QuplaToAbraContext, branch: AbraBlockBranch) : BaseOptimizer(context, branch) {

    private fun insertNullify(condition: AbraBaseSite, trueFalse: Boolean) {
        val site = branch.sites[index]

        // create a site for nullify<site.size>(conditon, site)
        val nullify = AbraSiteKnot()
        nullify.size = site.size
        nullify.inputs.add(condition)
        nullify.nullify(context, trueFalse)

        site.nullifyFalse = null
        site.nullifyTrue = null
        branch.sites.add(index + 1, nullify)

        replaceSite(site, nullify)
        nullify.inputs.add(site)
        site.references++
    }

    override fun processSite(site: AbraSiteMerge) {
        if (site.nullifyFalse != null) {
            insertNullify(site.nullifyFalse!!, false)
            return
        }

        if (site.nullifyTrue != null) {
            insertNullify(site.nullifyTrue!!, true)
        }
    }
}
