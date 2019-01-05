package org.iota.qupla.abra.block.site

import org.iota.qupla.abra.block.base.AbraBaseBlock
import org.iota.qupla.abra.context.base.AbraBaseContext
import org.iota.qupla.abra.funcmanagers.ConstFuncManager
import org.iota.qupla.abra.funcmanagers.NullifyFuncManager
import org.iota.qupla.abra.funcmanagers.SliceFuncManager
import org.iota.qupla.helper.TritVector
import org.iota.qupla.qupla.context.QuplaToAbraContext
import java.util.*

class AbraSiteKnot : AbraSiteMerge() {

    var block: AbraBaseBlock? = null

    fun branch(context: QuplaToAbraContext) {
        for (branch in context.abraModule.branches) {
            if (branch.name == name) {
                block = branch
                break
            }
        }
    }

    fun concat(context: QuplaToAbraContext) {
        block = slicers.find(context, size, 0)
    }

    override fun eval(context: AbraBaseContext) {
        context.evalKnot(this)
    }

    fun lut(context: QuplaToAbraContext) {
        for (lut in context.abraModule.luts) {
            if (lut.name == name) {
                block = lut
                break
            }
        }
    }

    fun nullify(context: QuplaToAbraContext, trueFalse: Boolean) {
        val nullify = if (trueFalse) AbraSiteKnot.nullifyTrue else AbraSiteKnot.nullifyFalse
        block = nullify.find(context, size)
    }

    fun slice(context: QuplaToAbraContext, inputSize: Int, start: Int) {
        Objects.requireNonNull(inputSize,"Unused slice.inputSize=$inputSize parameter")
        block = slicers.find(context, size, start)
    }

    fun vector(context: QuplaToAbraContext, vector: TritVector) {
        block = constants.find(context, vector)
    }

    companion object {
        var constants = ConstFuncManager()
        var nullifyFalse:NullifyFuncManager = NullifyFuncManager(false)
        var nullifyTrue :NullifyFuncManager= NullifyFuncManager(true)
        var slicers = SliceFuncManager()
    }
}
