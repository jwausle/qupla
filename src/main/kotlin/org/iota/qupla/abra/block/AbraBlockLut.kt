package org.iota.qupla.abra.block

import org.iota.qupla.abra.block.base.AbraBaseBlock
import org.iota.qupla.abra.context.base.AbraBaseContext

//TODO merge identical LUTs
class AbraBlockLut : AbraBaseBlock() {
    var lookup = "@@@@@@@@@@@@@@@@@@@@@@@@@@@"

    override fun eval(context: AbraBaseContext) {
        context.evalLut(this)
    }

    override fun toString(): String {
        return super.toString() + "[]"
    }

    companion object {

        fun unnamed(lookupTable: String): String {
            return "lut_" + lookupTable.replace('-', 'T').replace('@', 'N')
        }
    }
}
