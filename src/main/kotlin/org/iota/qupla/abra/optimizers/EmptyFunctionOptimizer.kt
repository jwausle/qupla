package org.iota.qupla.abra.optimizers

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.site.AbraSiteKnot
import org.iota.qupla.abra.block.site.AbraSiteMerge
import org.iota.qupla.abra.block.site.AbraSiteParam
import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.abra.optimizers.base.BaseOptimizer
import org.iota.qupla.qupla.context.QuplaToAbraContext

class EmptyFunctionOptimizer(context: QuplaToAbraContext, branch: AbraBlockBranch) : BaseOptimizer(context, branch) {

    override fun processKnot(knot: AbraSiteKnot) {
        // find and disable all function calls that do nothing

        // must be a function call that has a single input
        if (knot.block !is AbraBlockBranch || knot.inputs.size != 1) {
            return
        }

        val target = knot.block as AbraBlockBranch
        if (target.sites.size != 0 || target.latches.size != 0) {
            // not an empty function
            return
        }

        if (target.inputs.size != 1 || target.outputs.size != 1) {
            // not simply passing the input back to output
            return
        }

        val knotInput = knot.inputs[0]

        val input = target.inputs[0] as AbraSiteParam
        if (input.size != knotInput.size) {
            // some slicing going on
            return
        }

        val output = target.outputs[0] as AbraSiteMerge
        if (output.javaClass != AbraSiteMerge::class.java || output.inputs.size != 1) {
            // not a single-input merge
            return
        }

        if (output.inputs[0] !== input) {
            // WTF? how is this even possible?
            return
        }

        if (output.size != knotInput.size) {
            // another WTF moment
            return
        }

        // well, looks like we have a candidate
        replaceSite(knot, knotInput)
    }
}
