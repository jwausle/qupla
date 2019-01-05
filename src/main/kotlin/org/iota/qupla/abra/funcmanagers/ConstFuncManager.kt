package org.iota.qupla.abra.funcmanagers

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.AbraBlockLut
import org.iota.qupla.abra.block.base.AbraBaseBlock
import org.iota.qupla.abra.block.site.AbraSiteKnot
import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.abra.funcmanagers.base.BaseFuncManager
import org.iota.qupla.helper.TritVector
import org.iota.qupla.qupla.context.QuplaToAbraContext

class ConstFuncManager : BaseFuncManager("const") {
    var trits: TritVector? = null
    var value: TritVector? = null

    override fun createBaseInstances() {
        if (constZero != null) {
            // already initialized
            return
        }

        constZero = zeroManager.lut

        //// TODO Is context always not null here 'context!!.'
        constOne = context!!.abraModule.addLut("constOne" + BaseFuncManager.SEPARATOR, "111111111111111111111111111")
        constOne!!.specialType = AbraBaseBlock.TYPE_CONSTANT
        constOne!!.constantValue = TritVector(1, '1')

        //// TODO Is context always not null here 'context!!.'
        constMin = context!!.abraModule.addLut("constMin" + BaseFuncManager.SEPARATOR, "---------------------------")
        constMin!!.specialType = AbraBaseBlock.TYPE_CONSTANT
        constMin!!.constantValue = TritVector(1, '-')
    }

    override fun createInstance() {
        super.createInstance()
        // TODO Is branch always not null here 'branch!!.'
        val input = branch!!.addInputParam(1)

        val siteMin = tritConstant(constMin!!)
        val siteOne = tritConstant(constOne!!)
        val siteZero = tritConstant(constZero!!)

        var zeroes: AbraSiteKnot? = null
        if (trits!!.size() < size) {
            // need to concatenate rest of zeroes
            zeroes = AbraSiteKnot()
            zeroes.inputs.add(input)
            // TODO Is context always not null here 'context!!.'
            zeroes.block = zeroManager.find(context!!, size)
            // TODO Is block always not null here 'block!!.'
            zeroes.size = zeroes.block!!.size()
            // TODO Is branch always not null here 'branch!!.'
            branch!!.sites.add(zeroes)
        }

        val constant = AbraSiteKnot()
        constant.size = size
        for (i in 0 until trits!!.size()) {
            val c = trits!!.trit(i)
            constant.inputs.add(if (c == '0') siteZero else if (c == '1') siteOne else siteMin)
        }

        if (zeroes != null) {
            // TODO Is context always not null here 'context!!.'s
            constant.inputs.add(zeroes)
        }

        constant.concat(context!!)

        // TODO Is branch always not null here 'branch!!.'
        branch!!.specialType = AbraBaseBlock.TYPE_CONSTANT
        branch!!.constantValue = value
        branch!!.outputs.add(constant)
    }

    fun find(context: QuplaToAbraContext, value: TritVector): AbraBlockBranch {
        this.context = context
        this.value = value
        size = value.size()

        // strip off trailing zeroes
        trits = null
        for (i in size - 1 downTo 0) {
            if (value.trit(i) != '0') {
                trits = value.slice(0, i + 1)
                break
            }
        }

        if (trits == null || trits!!.size() == 0) {
            // all zeroes, pass it on to zero manager
            // TODO Is return-value always not null ')!!'
            return zeroManager.find(context, size)!!
        }

        name = funcName + BaseFuncManager.SEPARATOR + size + BaseFuncManager.SEPARATOR + trits!!.trits().replace('-', 'T')
        // TODO Is return-value always not null ')!!'
        return findInstance()!!
    }

    private fun tritConstant(tritLut: AbraBlockLut): AbraSiteKnot {
        // TODO Is branch always not null here 'branch!!.'
        val input = branch!!.inputs[0]

        val site = AbraSiteKnot()
        site.name = tritLut.name
        site.inputs.add(input)
        site.inputs.add(input)
        site.inputs.add(input)
        site.block = tritLut
        // TODO Is block always not null here 'block!!.'
        site.size = site.block!!.size()
        // TODO Is branch always not null here 'branch!!.'
        branch!!.sites.add(site)
        return site
    }

    companion object {
        private var constMin: AbraBlockLut? = null
        private var constOne: AbraBlockLut? = null
        private var constZero: AbraBlockLut? = null
        private val zeroManager = ConstZeroFuncManager()
    }
}
