package org.iota.qupla.abra.block.site

import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.abra.context.base.AbraBaseContext
import java.util.*

open class AbraSiteMerge : AbraBaseSite() {
    var inputs = ArrayList<AbraBaseSite>()

    override fun eval(context: AbraBaseContext) {
        context.evalMerge(this)
    }

    override fun markReferences() {
        super.markReferences()

        for (i in inputs.indices) {
            val input = inputs[i]
            if (input is AbraSiteLatch) {
                // reroute from placeholder to actual latch site
                inputs.set(i, input.latch!!)
                input.latch!!.references++

                continue
            }

            input.references++
        }
    }
}
