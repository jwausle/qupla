package org.iota.qupla.abra.funcmanagers

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.base.AbraBaseBlock
import org.iota.qupla.abra.block.site.AbraSiteKnot
import org.iota.qupla.abra.block.site.AbraSiteParam
import org.iota.qupla.abra.funcmanagers.base.BaseFuncManager
import org.iota.qupla.helper.TritVector

class ConstZeroFuncManager : BaseFuncManager("constZero") {

    override fun createBaseInstances() {
        createStandardBaseInstances()
    }

    override fun createInstance() {
        createBestFuncFunc()
    }

    override fun generateFuncFunc(inputSize: Int, inputSizes: Array<Int>): AbraBlockBranch? {
        // generate function that use smaller functions
        val manager = ConstZeroFuncManager()
        manager.instances = instances
        manager.sorted = sorted

        val branch = AbraBlockBranch()
        branch.name = funcName + BaseFuncManager.SEPARATOR + inputSize
        branch.size = inputSize

        val inputValue = branch.addInputParam(1)
        for (i in inputSizes.indices) {
            val knot = AbraSiteKnot()
            knot.inputs.add(inputValue)
            // TODO Is context always not null here 'context!!.'
            knot.block = manager.find(context!!, inputSizes[i])
            // TODO Is block always not null here 'block!!.'
            knot.size = knot.block!!.size()
            branch.outputs.add(knot)
        }

        branch.specialType = AbraBaseBlock.TYPE_CONSTANT
        branch.constantValue = TritVector(size, '0')

        return branch
    }

    override fun generateLut() {
        // TODO Is context always not null here 'context!!.'
        lut = context!!.abraModule.addLut("constZero" + BaseFuncManager.SEPARATOR, "000000000000000000000000000")
        // TODO Is lut always not null here 'lut!!.'
        lut!!.specialType = AbraBaseBlock.TYPE_CONSTANT
        lut!!.constantValue = TritVector(1, '0')
    }

    override fun generateLutFunc(inputSize: Int) {
        // generate function that use LUTs
        val branch = AbraBlockBranch()
        branch.name = funcName + BaseFuncManager.SEPARATOR + inputSize
        branch.size = inputSize

        val inputValue = branch.addInputParam(1)
        for (i in 0 until inputSize) {
            val knot = AbraSiteKnot()
            knot.inputs.add(inputValue)
            knot.inputs.add(inputValue)
            knot.inputs.add(inputValue)
            knot.block = lut
            // TODO Is branch always not null here 'branch!!.'
            knot.size = knot.block!!.size()
            branch.outputs.add(knot)
        }

        branch.specialType = AbraBaseBlock.TYPE_CONSTANT
        branch.constantValue = TritVector(size, '0')

        saveBranch(branch)
    }
}
