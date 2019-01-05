package org.iota.qupla.abra.optimizers

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.site.AbraSiteMerge
import org.iota.qupla.abra.optimizers.base.BaseOptimizer
import org.iota.qupla.qupla.context.QuplaToAbraContext

class SlicedInputOptimizer(context: QuplaToAbraContext, branch: AbraBlockBranch) : BaseOptimizer(context, branch) {

    override fun processSite(site: AbraSiteMerge) {
        //TODO split up inputs that will be sliced later so that they
        //     are pre-sliced and add concat calls to replace them
    }
}
