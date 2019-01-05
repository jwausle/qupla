package org.iota.qupla.abra.block

import java.util.ArrayList

import org.iota.qupla.abra.block.base.AbraBaseBlock
import org.iota.qupla.abra.context.base.AbraBaseContext

class AbraBlockImport : AbraBaseBlock() {
    var blocks = ArrayList<AbraBaseBlock>()
    var hash: String? = null

    override fun eval(context: AbraBaseContext) {
        context.evalImport(this)
    }
}
