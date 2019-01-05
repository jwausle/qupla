package org.iota.qupla.abra.block.site

import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.abra.context.base.AbraBaseContext

class AbraSiteParam : AbraBaseSite() {
    var offset: Int = 0

    override fun eval(context: AbraBaseContext) {
        context.evalParam(this)
    }
}
