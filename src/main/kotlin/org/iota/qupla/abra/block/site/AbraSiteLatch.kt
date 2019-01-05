package org.iota.qupla.abra.block.site

import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.abra.context.base.AbraBaseContext

class AbraSiteLatch : AbraBaseSite() {
    var latch: AbraBaseSite? = null

    override fun eval(context: AbraBaseContext) {
        context.evalLatch(this)
    }
}
