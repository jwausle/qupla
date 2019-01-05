package org.iota.qupla.abra.funcmanagers

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.base.AbraBaseBlock
import org.iota.qupla.abra.block.site.AbraSiteKnot
import org.iota.qupla.abra.block.site.AbraSiteParam
import org.iota.qupla.abra.funcmanagers.base.BaseFuncManager
import org.iota.qupla.helper.TritVector

class NullifyFuncManager(var trueFalse: Boolean) : BaseFuncManager(if (trueFalse) "nullifyTrue" else "nullifyFalse") {

    override fun createBaseInstances() {
        createStandardBaseInstances()
    }

    override fun createInstance() {
        createBestFuncFunc()
    }

    override fun generateFuncFunc(inputSize: Int, inputSizes: Array<Int>): AbraBlockBranch? {
        // generate function that use smaller functions
        val manager = NullifyFuncManager(trueFalse)
        manager.instances = instances
        manager.sorted = sorted

        val branch = AbraBlockBranch()
        branch.name = funcName + BaseFuncManager.SEPARATOR + inputSize
        branch.size = inputSize

        val inputFlag = branch.addInputParam(1)
        for (i in inputSizes.indices) {
            val inputValue = branch.addInputParam(inputSizes[i])
            val knot = AbraSiteKnot()
            knot.inputs.add(inputFlag)
            knot.inputs.add(inputValue)
            // TODO Is context always not null here 'context!!.'
            knot.block = manager.find(context!!, inputSizes[i])
            // TODO Is block always not null here 'block!!.'
            knot.size = knot.block!!.size()
            branch.outputs.add(knot)
        }

        branch.specialType = if (trueFalse) AbraBaseBlock.TYPE_NULLIFY_TRUE else AbraBaseBlock.TYPE_NULLIFY_FALSE
        branch.constantValue = TritVector(size, '@')
        return branch
    }

    override fun generateLut() {
        val trueTrits = "@@-@@0@@1@@-@@0@@1@@-@@0@@1"
        val falseTrits = "-@@0@@1@@-@@0@@1@@-@@0@@1@@"
        // TODO Is context always not null here 'context!!.'
        lut = context!!.abraModule.addLut(funcName + BaseFuncManager.SEPARATOR, if (trueFalse) trueTrits else falseTrits)
        // TODO Is lut always not null here 'lut!!.'
        lut!!.specialType = if (trueFalse) AbraBaseBlock.TYPE_NULLIFY_TRUE else AbraBaseBlock.TYPE_NULLIFY_FALSE
        lut!!.constantValue = TritVector(1, '@')
    }

    override fun generateLutFunc(inputSize: Int) {
        // generate function that use LUTs
        val branch = AbraBlockBranch()
        branch.name = funcName + BaseFuncManager.SEPARATOR + inputSize
        branch.size = inputSize

        val inputFlag = branch.addInputParam(1)
        for (i in 0 until inputSize) {
            val inputValue = branch.addInputParam(1)
            val knot = AbraSiteKnot()
            knot.inputs.add(inputFlag)
            knot.inputs.add(inputValue)
            knot.inputs.add(inputValue)
            knot.block = lut
            // TODO Is block always not null here 'block!!.'
            knot.size = knot.block!!.size()
            branch.outputs.add(knot)
        }

        branch.specialType = if (trueFalse) AbraBaseBlock.TYPE_NULLIFY_TRUE else AbraBaseBlock.TYPE_NULLIFY_FALSE
        branch.constantValue = TritVector(size, '@')
        saveBranch(branch)
    }
}
