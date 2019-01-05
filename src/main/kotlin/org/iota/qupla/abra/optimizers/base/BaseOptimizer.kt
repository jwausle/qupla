package org.iota.qupla.abra.optimizers.base

import java.util.ArrayList

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.site.AbraSiteKnot
import org.iota.qupla.abra.block.site.AbraSiteMerge
import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.qupla.context.QuplaToAbraContext

open class BaseOptimizer(var context: QuplaToAbraContext, var branch: AbraBlockBranch) {
    var index: Int = 0
    var reverse: Boolean = false

    private fun process() {
        val site = branch.sites[index]
        if (site.references == 0) {
            return
        }

        if (site.javaClass == AbraSiteMerge::class.java) {
            processMerge(site as AbraSiteMerge)
        }

        if (site.javaClass == AbraSiteKnot::class.java) {
            processKnot(site as AbraSiteKnot)
        }

        processSite(site as AbraSiteMerge)
    }

    protected open fun processKnot(knot: AbraSiteKnot) {}

    protected open fun processMerge(merge: AbraSiteMerge) {}

    protected open fun processSite(site: AbraSiteMerge) {}

    protected fun replaceSite(site: AbraBaseSite, replacement: AbraBaseSite) {
        if (site.hasNullifier()) {
            // extra precaution not to lose info
            return
        }

        replaceSite(site, replacement, branch.sites)
        replaceSite(site, replacement, branch.outputs)
        replaceSite(site, replacement, branch.latches)
    }

    private fun replaceSite(target: AbraBaseSite, replacement: AbraBaseSite, sites: ArrayList<out AbraBaseSite>) {
        for (next in sites) {
            if (next is AbraSiteMerge) {
                for (i in next.inputs.indices) {
                    if (next.inputs[i] === target) {
                        target.references--
                        replacement.references++
                        next.inputs[i] = replacement
                    }
                }
            }

            if (next.nullifyFalse === target) {
                target.references--
                replacement.references++
                next.nullifyFalse = replacement
            }

            if (next.nullifyTrue === target) {
                target.references--
                replacement.references++
                next.nullifyTrue = replacement
            }
        }
    }

    open fun run() {
        if (reverse) {
            index = branch.sites.size - 1
            while (index >= 0) {
                process()
                index--
            }
            return
        }

        index = 0
        while (index < branch.sites.size) {
            process()
            index++
        }
    }
}
