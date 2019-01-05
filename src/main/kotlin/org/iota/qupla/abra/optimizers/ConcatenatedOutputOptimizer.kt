package org.iota.qupla.abra.optimizers

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.site.AbraSiteMerge
import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.abra.optimizers.base.BaseOptimizer
import org.iota.qupla.qupla.context.QuplaToAbraContext

class ConcatenatedOutputOptimizer(context: QuplaToAbraContext, branch: AbraBlockBranch) : BaseOptimizer(context, branch) {

    override fun processSite(site: AbraSiteMerge) {
        //TODO replace concatenation by moving concatenated sites from body to outputs
    }

    override fun run() {
        index = 0
        while (index < branch.outputs.size) {
            val site = branch.outputs[index]
            processSite(site as AbraSiteMerge)
            index++
        }
    }
}
