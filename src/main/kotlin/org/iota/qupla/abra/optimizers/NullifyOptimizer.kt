package org.iota.qupla.abra.optimizers

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.site.AbraSiteMerge
import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.abra.optimizers.base.BaseOptimizer
import org.iota.qupla.qupla.context.QuplaToAbraContext

class NullifyOptimizer(context: QuplaToAbraContext, branch: AbraBlockBranch) : BaseOptimizer(context, branch) {
    init {
        reverse = true
    }

    override fun processSite(site: AbraSiteMerge) {
        if (index == 0 || !site.hasNullifier()) {
            // no need to move it up the chain anyway
            return
        }

        // check if all inputs have only a single reference
        for (input in site.inputs) {
            if (input.isLatch) {
                // don't fuck around with latches
                return
            }

            if (input.hasNullifier()) {
                // cannot force a nullify on something that already has one
                return
            }

            //TODO when at least one input has a single reference
            //     insert knots for the inputs that have >1 references
            //     and then move the nullifies there to avoid calling the knot
            if (input.references != 1 || input !is AbraSiteMerge) {
                // cannot force nullify on something referenced from somewhere else
                // nor on something that isn't a merge or a knot
                return
            }
        }

        // first move those inputs to the nullify point
        for (input in site.inputs) {
            branch.sites.remove(input)
        }

        branch.sites.addAll(index - site.inputs.size, site.inputs)

        // move nullifyFalse up the chain??
        if (site.nullifyFalse != null) {
            for (input in site.inputs) {
                input.nullifyFalse = site.nullifyFalse
                // TODO Is nullifyFalse always not null here 'nullifyFalse!!.'
                site.nullifyFalse!!.references++
            }
            // TODO Is nullifyFalse always not null here 'nullifyFalse!!.'
            site.nullifyFalse!!.references--
            site.nullifyFalse = null
        }

        // move nullifyTrue up the chain??
        if (site.nullifyTrue != null) {
            for (input in site.inputs) {
                input.nullifyTrue = site.nullifyTrue
                // TODO Is nullifyFalse always not null here 'nullifyFalse!!.'
                site.nullifyTrue!!.references++
            }
            // TODO Is nullifyFalse always not null here 'nullifyFalse!!.'
            site.nullifyTrue!!.references--
            site.nullifyTrue = null
        }

        // this could have freed up another optimization possibility,
        // so we restart the optimization from the end
        index = branch.sites.size
    }
}
