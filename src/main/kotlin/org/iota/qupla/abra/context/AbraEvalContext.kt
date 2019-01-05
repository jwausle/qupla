package org.iota.qupla.abra.context

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.AbraBlockImport
import org.iota.qupla.abra.block.AbraBlockLut
import org.iota.qupla.abra.block.base.AbraBaseBlock
import org.iota.qupla.abra.block.site.AbraSiteKnot
import org.iota.qupla.abra.block.site.AbraSiteLatch
import org.iota.qupla.abra.block.site.AbraSiteMerge
import org.iota.qupla.abra.block.site.AbraSiteParam
import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.abra.context.base.AbraBaseContext
import org.iota.qupla.helper.StateValue
import org.iota.qupla.helper.TritVector
import org.iota.qupla.qupla.context.QuplaToAbraContext
import org.iota.qupla.qupla.expression.FuncExpr
import org.iota.qupla.qupla.expression.IntegerExpr
import org.iota.qupla.qupla.expression.base.BaseExpr
import java.util.*

class AbraEvalContext : AbraBaseContext() {

    var abra: QuplaToAbraContext? = null
    var args = ArrayList<TritVector>()
    var callNr: Int = 0
    var callTrail = ByteArray(4096)
    var stack: Array<TritVector?> = Array(0) { null }
    var value: TritVector? = null

    override fun eval(context: QuplaToAbraContext, expr: BaseExpr) {
        abra = context
        if (expr is FuncExpr) {
            for (branch in context.abraModule.branches) {
                if (branch.origin === expr.func) {
                    args.clear()
                    for (arg in expr.args) {
                        if (arg is IntegerExpr) {
                            args.add(arg.vector)
                            continue
                        }

                        error("Expected constant value")
                    }

                    branch.eval(this)
                }
            }
        }
    }

    override fun evalBranch(branch: AbraBlockBranch) {
        if (branch.specialType == AbraBaseBlock.TYPE_CONSTANT) {
            value = branch.constantValue
            return
        }

        if (branch.specialType == AbraBaseBlock.TYPE_NULLIFY_TRUE) {
            if (args[0].trit(0) != '1') {
                value = branch.constantValue
                return
            }

            value = args[1]
            return
        }

        if (branch.specialType == AbraBaseBlock.TYPE_NULLIFY_FALSE) {
            if (args[0].trit(0) != '-') {
                value = branch.constantValue
                return
            }

            value = args[1]
            return
        }

        if (branch.specialType == AbraBaseBlock.TYPE_SLICE) {
            if (args.size == 1) {
                value = args[0].slice(branch.offset, branch.size)
                return
            }
        }

        val oldStack = stack
        stack = arrayOfNulls(branch.totalSites())

        if (!evalBranchInputsMatch(branch)) {
            value = null
            for (arg in args) {
                value = TritVector.concat(value, arg)
            }

            if (branch.specialType == AbraBaseBlock.TYPE_SLICE) {
                stack = oldStack
                return
            }

            for (input in branch.inputs) {
                input.eval(this)
            }
        }

        // initialize latches with old values
        for (latch in branch.latches) {
            initializeLatch(latch)
        }

        for (site in branch.sites) {
            site.eval(this)
        }

        var result: TritVector? = null
        for (output in branch.outputs) {
            output.eval(this)
            result = TritVector.concat(result, value)
        }

        // update latches with new values
        for (latch in branch.latches) {
            updateLatch(latch)
        }

        stack = oldStack
        value = result
    }

    private fun evalBranchInputsMatch(branch: AbraBlockBranch): Boolean {
        if (args.size != branch.inputs.size) {
            return false
        }

        for (i in args.indices) {
            val arg = args[i]
            val input = branch.inputs[i]
            if (arg.size() != input.size) {
                return false
            }

            stack[input.index] = arg
        }

        return true
    }

    override fun evalImport(imp: AbraBlockImport) {}

    override fun evalKnot(knot: AbraSiteKnot) {
        args.clear()
        var isAllNull = true
        for (input in knot.inputs) {
            val arg = stack[input.index]
            isAllNull = isAllNull && arg?.isNull ?: true
            args.add(arg!!)
        }

        if (isAllNull) {
            stack[knot.index] = TritVector(knot.size, '@')
            return
        }

        callTrail[callNr++] = knot.index.toByte()

        // TODO solve - not null block expected
        knot.block!!.eval(this)
        stack[knot.index] = value

        callNr--
    }

    override fun evalLatch(latch: AbraSiteLatch) {}

    override fun evalLut(lut: AbraBlockLut) {
        if (args.size != 3) {
            error("LUT needs exactly 3 inputs")
        }

        val trits = CharArray(3)
        for (i in 0..2) {
            val arg = args[i]
            if (arg.size() != 1) {
                error("LUT inputs need to be exactly 1 trit")
            }

            trits[i] = arg.trit(0)
        }

        val index = AbraBaseContext.indexFromTrits[String(trits)]
        if (index != null) {
            when (lut.lookup[index]) {
                '0' -> {
                    value = tritZero
                    return
                }

                '1' -> {
                    value = tritOne
                    return
                }

                '-' -> {
                    value = tritMin
                    return
                }
            }
        }

        value = tritNull
    }

    override fun evalMerge(merge: AbraSiteMerge) {
        value = null
        for (input in merge.inputs) {
            val mergeValue = stack[input.index]
            if (mergeValue?.isNull ?: true) {
                continue
            }

            if (value == null) {
                value = mergeValue
                continue
            }

            error("Multiple non-null merge values")
        }

        if (value == null) {
            value = TritVector(merge.size, '@')
        }

        stack[merge.index] = value
    }

    override fun evalParam(param: AbraSiteParam) {
        if (value!!.size() < param.offset + param.size) {
            error("Insufficient input trits: " + value!!)
        }

        stack[param.index] = value!!.slice(param.offset, param.size)
    }

    private fun initializeLatch(latch: AbraBaseSite) {
        if (latch.references == 0) {
            return
        }

        callTrail[callNr] = latch.index.toByte()

        val call = StateValue()
        call.path = callTrail
        call.pathLength = callNr + 1

        // if state was saved before set latch to that value otherwise set to zero
        val stateValue = stateValues[call]
        if (stateValue != null) {
            stack[latch.index] = stateValue.value
            return
        }

        stack[latch.index] = TritVector(latch.size, '0')
    }

    private fun updateLatch(latch: AbraBaseSite) {
        if (latch.references == 0) {
            return
        }

        latch.eval(this)

        // do NOT update latch site on stack with new value
        // other latches may need the old value still
        // again, do NOT add: stack[latch.index] = value;

        callTrail[callNr] = latch.index.toByte()

        val call = StateValue()
        call.path = callTrail
        call.pathLength = callNr + 1

        // state already saved?
        val stateValue = stateValues[call]
        if (stateValue != null) {
            // reset state?
            if (value!!.isZero) {
                stateValues.remove(call)
                return
            }

            // overwrite state
            stateValue.value = value
            return
        }

        // state not saved yet

        // reset state?
        if (value!!.isZero) {
            // already reset
            return
        }

        // save state
        call.path = Arrays.copyOf(callTrail, callNr + 1)
        call.value = value
        stateValues[call] = call
    }

    companion object {
        // note: stateValues needs to be static so that state is preserved between invocations
        private val stateValues = HashMap<StateValue, StateValue>()

        private val tritMin = TritVector(1, '-')
        private val tritNull = TritVector(1, '@')
        private val tritOne = TritVector(1, '1')
        private val tritZero = TritVector(1, '0')
    }
}
