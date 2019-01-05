package org.iota.qupla.abra.optimizers

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.site.AbraSiteMerge
import org.iota.qupla.abra.optimizers.base.BaseOptimizer
import org.iota.qupla.qupla.context.QuplaToAbraContext

class SingleInputMergeOptimizer(context: QuplaToAbraContext, branch: AbraBlockBranch) : BaseOptimizer(context, branch) {

    override fun processMerge(merge: AbraSiteMerge) {
        if (merge.inputs.size == 1) {
            // this leaves <merge> unreferenced
            replaceSite(merge, merge.inputs[0])
        }
    }
}
