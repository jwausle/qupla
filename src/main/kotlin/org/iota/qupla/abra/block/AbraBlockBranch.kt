package org.iota.qupla.abra.block

import org.iota.qupla.abra.block.base.AbraBaseBlock
import org.iota.qupla.abra.block.site.AbraSiteParam
import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.abra.context.base.AbraBaseContext
import org.iota.qupla.abra.optimizers.ConcatenatedOutputOptimizer
import org.iota.qupla.abra.optimizers.ConcatenationOptimizer
import org.iota.qupla.abra.optimizers.EmptyFunctionOptimizer
import org.iota.qupla.abra.optimizers.LutFunctionWrapperOptimizer
import org.iota.qupla.abra.optimizers.MultiLutOptimizer
import org.iota.qupla.abra.optimizers.NullifyInserter
import org.iota.qupla.abra.optimizers.NullifyOptimizer
import org.iota.qupla.abra.optimizers.SingleInputMergeOptimizer
import org.iota.qupla.abra.optimizers.SlicedInputOptimizer
import org.iota.qupla.abra.optimizers.UnreferencedSiteRemover
import org.iota.qupla.qupla.context.QuplaToAbraContext
import java.util.*

class AbraBlockBranch : AbraBaseBlock() {
    var inputs = ArrayList<AbraBaseSite>()
    var latches = ArrayList<AbraBaseSite>()
    var offset: Int = 0
    var outputs = ArrayList<AbraBaseSite>()
    var sites = ArrayList<AbraBaseSite>()
    var size: Int = 0

    fun addInput(inputSite: AbraSiteParam) {
        if (inputs.size != 0) {
            val lastInput = inputs[inputs.size - 1] as AbraSiteParam
            inputSite.offset = lastInput.offset + lastInput.size
        }

        inputs.add(inputSite)
    }

    fun addInputParam(inputSize: Int): AbraSiteParam {
        val inputSite = AbraSiteParam()
        inputSite.size = inputSize
        inputSite.name = "P" + inputs.size
        addInput(inputSite)
        return inputSite
    }

    override fun eval(context: AbraBaseContext) {
        context.evalBranch(this)
    }

    override fun markReferences() {
        markReferences(inputs)
        markReferences(sites)
        markReferences(outputs)
        markReferences(latches)
    }

    private fun markReferences(sites: ArrayList<out AbraBaseSite>) {
        for (site in sites) {
            site.markReferences()
        }
    }

    fun numberSites() {
        var siteNr = 0
        siteNr = numberSites(siteNr, inputs)
        siteNr = numberSites(siteNr, sites)
        siteNr = numberSites(siteNr, outputs)
        siteNr = numberSites(siteNr, latches)
        Objects.requireNonNull(siteNr,"unused numberSites.siteNr=$siteNr variable");
    }

    private fun numberSites(siteNr: Int, sites: ArrayList<out AbraBaseSite>): Int {
        var localSiteNr = siteNr
        for (site in sites) {
            site.index = localSiteNr++
        }

        return localSiteNr
    }

    override fun optimize(context: QuplaToAbraContext) {
        // first move the nullifies up the chain as far as possible
        NullifyOptimizer(context, this).run()

        // then insert actual nullify operations and rewire accordingly
        NullifyInserter(context, this).run()

        // remove some obvious nonsense before doing more complex analysis
        optimizeCleanup(context)

        // run the set of actual optimizations
        optimizePass(context)

        // and finally one last cleanup
        optimizeCleanup(context)
    }

    private fun optimizeCleanup(context: QuplaToAbraContext) {
        // bypass superfluous single-input merges
        SingleInputMergeOptimizer(context, this).run()

        // and remove all unreferenced sites
        UnreferencedSiteRemover(context, this).run()
    }

    private fun optimizePass(context: QuplaToAbraContext) {
        // bypass all function calls that do nothing
        EmptyFunctionOptimizer(context, this).run()

        // replace lut wrapper function with direct lut operations
        LutFunctionWrapperOptimizer(context, this).run()

        // pre-slice inputs that will be sliced later on
        SlicedInputOptimizer(context, this).run()

        // replace concatenation knot that is passed as input to a knot
        ConcatenationOptimizer(context, this).run()

        // if possible, replace lut calling lut with a single lut that does it all
        MultiLutOptimizer(context, this).run()

        // move concatenated sites from body to outputs
        ConcatenatedOutputOptimizer(context, this).run()
    }

    override fun size(): Int {
        return size
    }

    override fun toString(): String {
        return super.toString() + "()"
    }

    fun totalSites(): Int {
        return inputs.size + sites.size + outputs.size + latches.size
    }
}
