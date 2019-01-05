package org.iota.qupla.abra.funcmanagers

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.base.AbraBaseBlock
import org.iota.qupla.abra.block.site.AbraSiteMerge
import org.iota.qupla.abra.block.site.AbraSiteParam
import org.iota.qupla.abra.funcmanagers.base.BaseFuncManager
import org.iota.qupla.qupla.context.QuplaToAbraContext

class SliceFuncManager : BaseFuncManager("slice") {
    var start: Int = 0

    override fun createInstance() {
        super.createInstance()

        if (start > 0) {
            // TODO Is branch always not null here 'branch!!.'
            branch!!.addInputParam(start)
        }
        // TODO Is branch always not null here 'branch!!.'
        val inputSite = branch!!.addInputParam(size)

        val merge = AbraSiteMerge()
        merge.size = size
        merge.inputs.add(inputSite)

        // TODO Is branch always not null here 'branch!!.'
        branch!!.specialType = AbraBaseBlock.TYPE_SLICE
        branch!!.offset = start
        branch!!.outputs.add(merge)
    }

    fun find(context: QuplaToAbraContext, size: Int, start: Int): AbraBlockBranch {
        this.context = context
        this.size = size
        this.start = start
        name = funcName + BaseFuncManager.SEPARATOR + size + BaseFuncManager.SEPARATOR + start
        // TODO Is return always not null here ')!!'
        return findInstance()!!
    }
}
