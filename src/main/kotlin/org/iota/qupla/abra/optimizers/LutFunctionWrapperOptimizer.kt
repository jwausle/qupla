package org.iota.qupla.abra.optimizers

import java.util.ArrayList

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.AbraBlockLut
import org.iota.qupla.abra.block.site.AbraSiteKnot
import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.abra.optimizers.base.BaseOptimizer
import org.iota.qupla.qupla.context.QuplaToAbraContext

class LutFunctionWrapperOptimizer(context: QuplaToAbraContext, branch: AbraBlockBranch) : BaseOptimizer(context, branch) {

    override fun processKnot(knot: AbraSiteKnot) {
        // replace lut wrapper function with direct lut operation

        // must be a function call with max 3 inputs
        if (knot.block !is AbraBlockBranch || knot.inputs.size > 3) {
            return
        }

        // all input values must be single trit
        for (input in knot.inputs) {
            if (input.size != 1) {
                return
            }
        }

        val target = knot.block as AbraBlockBranch
        if (target.sites.size != 0 || target.latches.size != 0) {
            // not an otherwise empty function
            return
        }

        if (target.inputs.size != knot.inputs.size || target.outputs.size != 1) {
            // not simply passing the inputs to output LUT knot
            return
        }

        // all input params must be single trit
        for (input in target.inputs) {
            if (input.size != 1) {
                return
            }
        }

        if (target.outputs[0].javaClass != AbraSiteKnot::class.java) {
            // definitely not a lut lookup
            return
        }

        val output = target.outputs[0] as AbraSiteKnot
        if (output.inputs.size != 3 || output.size != 1 || output.block !is AbraBlockLut) {
            // not a lut lookup
            return
        }

        // well, looks like we have a candidate
        // reroute knot directly to LUT
        val inputs = ArrayList<AbraBaseSite>()
        for (input in output.inputs) {
            val idx = target.inputs.indexOf(input)
            val knotInput = knot.inputs[idx]
            inputs.add(knotInput)
            knotInput.references++
        }

        for (input in knot.inputs) {
            input.references--
        }

        knot.inputs = inputs
        knot.block = output.block
    }
}
