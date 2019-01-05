package org.iota.qupla.abra.context

import org.iota.qupla.abra.AbraModule
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
import org.iota.qupla.helper.TritVector
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.omg.CORBA.Object
import java.util.*

class AbraAnalyzeContext : AbraBaseContext() {
    var missing: Int = 0
    var offset: Int = 0

    fun check(condition: Boolean) {
        if (sanityCheck && !condition) {
            error("Check failed")
        }
    }

    private fun clearSizes(sites: ArrayList<AbraBaseSite>) {
        for (site in sites) {
            site.size = 0
        }
    }

    fun clearSizes(module: AbraModule) {
        for (branch in module.branches) {
            branch.size = 0
            clearSizes(branch.sites)
            clearSizes(branch.outputs)
            clearSizes(branch.latches)
            for (latch in branch.latches) {
                latch.isLatch = true
            }
        }
    }

    fun ensure(condition: Boolean) {
        if (!condition) {
            error("Ensure failed")
        }
    }

    override fun eval(module: AbraModule) {
        // we're going to recalculate all sizes
        clearSizes(module)

        // single pass over everything
        super.eval(module)

        // some sizes may have been indeterminable for now due to recursion
        resolveRecursions(module)
    }

    override fun evalBranch(branch: AbraBlockBranch) {
        if (branch.analyzed) {
            return
        }

        branch.analyzed = true

        offset = 0
        var index = 0
        val lastMissing = missing
        index = evalBranchSites(index, branch.inputs)
        index = evalBranchSites(index, branch.sites)
        index = evalBranchSites(index, branch.outputs)
        index = evalBranchSites(index, branch.latches)
        Objects.requireNonNull(index, "Ununsed evalBranch.index=$index is never used.")

        var size = 0
        for (output in branch.outputs) {
            if (output.size == 0) {
                // insufficient data to calculate return size yet
                branch.analyzed = false
                return
            }

            size += output.size
        }

        branch.size = size
        ensure(branch.size != 0)

        if (lastMissing != missing) {
            // come back later to fill in missing sites
            branch.analyzed = false
            return
        }

        evalBranchSpecial(branch)

        branch.analyzed = true
    }

    fun evalBranchSites(index: Int, sites: ArrayList<AbraBaseSite>): Int {
        var localIndex = index
        for (site in sites) {
            check(site.index == localIndex)
            site.index = localIndex++

            site.eval(this)
        }

        return localIndex
    }

    private fun evalBranchSpecial(branch: AbraBlockBranch): Boolean {
        return if (branch.latches.size != 0) {
            false
        } else evalBranchSpecialConstantZero(branch) || //

                evalBranchSpecialConstantNonZero(branch) || //

                evalBranchSpecialNullify(branch) || //

                evalBranchSpecialSlice(branch)

    }

    private fun evalBranchSpecialConstantNonZero(branch: AbraBlockBranch): Boolean {
        // nonzero constant function has 1 input, multiple sites, and 1 output
        if (branch.inputs.size != 1 || branch.sites.size < 2 || branch.outputs.size != 1) {
            return false
        }

        // input is any single trit that triggers data flow
        val inputTrit = branch.inputs[0]
        if (inputTrit.size != 1) {
            return false
        }

        var constant: TritVector? = null
        for (site in branch.sites) {
            if (site !is AbraSiteKnot) {
                return false
            }
            // TODO Is .block always not null here '.block!!.'
            if (site.block!!.specialType != AbraBaseBlock.TYPE_CONSTANT) {
                return false
            }

            // all inputs triggered by input trit
            for (input in site.inputs) {
                if (input !== inputTrit) {
                    return false
                }
            }

            constant = TritVector.concat(constant, site.block!!.constantValue)
        }

        val output = branch.outputs[0] as? AbraSiteKnot ?: return false

        if (output.block!!.specialType != AbraBaseBlock.TYPE_SLICE || output.inputs.size != branch.sites.size) {
            return false
        }

        //TODO could verify that all knot.inputs are all branch.sites
        // TODO Is .name always not null here '.name!!.'
        check(branch.name != null && branch.name!!.startsWith("const_"))

        branch.specialType = AbraBaseBlock.TYPE_CONSTANT
        branch.constantValue = constant!!.slice(0, output.size)
        return true
    }

    private fun evalBranchSpecialConstantZero(branch: AbraBlockBranch): Boolean {
        if (branch.inputs.size != 1 || branch.sites.size != 0) {
            return false
        }

        // input is any single trit that triggers data flow
        val inputTrit = branch.inputs[0]
        if (inputTrit.size != 1) {
            return false
        }

        var constant: TritVector? = null
        for (output in branch.outputs) {
            if (output !is AbraSiteKnot) {
                return false
            }
            // TODO Is .block always not null here '.block!!.'
            if (output.block!!.specialType != AbraBaseBlock.TYPE_CONSTANT) {
                return false
            }

            // all inputs triggered by input trit
            for (input in output.inputs) {
                if (input !== inputTrit) {
                    return false
                }
            }

            constant = TritVector.concat(constant, output.block!!.constantValue)
        }
        // TODO Is .name always not null here '.name!!.'
        check(branch.name != null && branch.name!!.startsWith("constZero"))

        branch.specialType = AbraBaseBlock.TYPE_CONSTANT
        branch.constantValue = constant
        return true
    }

    private fun evalBranchSpecialNullify(branch: AbraBlockBranch): Boolean {
        if (branch.sites.size != 0 || branch.outputs.size == 0) {
            return false
        }

        // nullify function has 1 input more than outputs
        if (branch.inputs.size != branch.outputs.size + 1) {
            return false
        }

        // first input is the boolean flag
        val inputFlag = branch.inputs[0]
        if (inputFlag.size != 1) {
            return false
        }

        val firstOutput = branch.outputs[0] as? AbraSiteKnot ?: return false

        // TODO Is .block always not null here '.block!!.'
        val type = firstOutput.block!!.specialType
        if (type != AbraBaseBlock.TYPE_NULLIFY_FALSE && type != AbraBaseBlock.TYPE_NULLIFY_TRUE) {
            return false
        }

        for (i in branch.outputs.indices) {
            val output = branch.outputs[0] as? AbraSiteKnot ?: return false

            if (output.block!!.specialType != type) {
                // must be same nullify type
                return false
            }

            if (output.inputs[0] !== inputFlag || output.inputs[1] !== branch.inputs[i + 1]) {
                return false
            }

            //TODO double-check number of knot inputs (2 or 3) against knot type (branch or lut)?
        }

        // TODO Is .name always not null here '.name!!.'
        check(branch.name != null && branch.name!!.startsWith("nullify"))

        // set nullify type and have a correctly sized null vector ready
        branch.specialType = type
        branch.constantValue = TritVector(branch.size, '@')
        return true
    }

    private fun evalBranchSpecialSlice(branch: AbraBlockBranch): Boolean {
        if (branch.inputs.size > 2 || branch.sites.size != 0 || branch.outputs.size != 1) {
            return false
        }

        // last input is the sliced input
        val input = branch.inputs[branch.inputs.size - 1] as AbraSiteParam

        val output = branch.outputs[0] as? AbraSiteMerge ?: return false

        if (output.inputs.size != 1 || output.inputs[0] !== input) {
            return false
        }

        branch.specialType = AbraBaseBlock.TYPE_SLICE
        branch.offset = input.offset
        return true
    }

    override fun evalImport(imp: AbraBlockImport) {

    }

    override fun evalKnot(knot: AbraSiteKnot) {
        if (knot.inputs.size == 0 && knot.references == 0) {
            return
        }
        // TODO Is .block always not null here '.block!!.'
        knot.block!!.eval(this)
        if (knot.block!!.size() == 0) {
            missing++
            return
        }

        knot.size = knot.block!!.size()
        ensure(knot.size != 0)

        if (knot.block is AbraBlockBranch) {
            evalKnotBranch(knot)
            return
        }

        // knot.block is a lut
        ensure(knot.inputs.size == 3)

        for (input in knot.inputs) {
            ensure(input.size == 1)
        }
    }

    private fun evalKnotBranch(knot: AbraSiteKnot) {
        Objects.requireNonNull(knot,"Unused evalKnotBranch.knot=$knot parameter")
        //TODO
    }

    override fun evalLatch(latch: AbraSiteLatch) {
        check(latch.references == 0)
    }

    override fun evalLut(lut: AbraBlockLut) {
        if (lut.analyzed) {
            return
        }

        lut.analyzed = true

        if (lut.lookup == constZero) {
            lut.specialType = AbraBaseBlock.TYPE_CONSTANT
            lut.constantValue = TritVector(1, '0')
            return
        }

        if (lut.lookup == constMin) {
            lut.specialType = AbraBaseBlock.TYPE_CONSTANT
            lut.constantValue = TritVector(1, '-')
            return
        }

        if (lut.lookup == constOne) {
            lut.specialType = AbraBaseBlock.TYPE_CONSTANT
            lut.constantValue = TritVector(1, '1')
            return
        }

        if (lut.lookup == nullifyFalse) {
            lut.specialType = AbraBaseBlock.TYPE_NULLIFY_FALSE
            return
        }

        if (lut.lookup == nullifyTrue) {
            lut.specialType = AbraBaseBlock.TYPE_NULLIFY_TRUE
            return
        }
    }

    override fun evalMerge(merge: AbraSiteMerge) {
        if (merge.inputs.size == 0 && merge.references == 0) {
            return
        }

        for (input in merge.inputs) {
            if (input.size != 0) {
                if (merge.size == 0) {
                    merge.size = input.size
                    continue
                }

                if (merge.size != input.size) {
                    error("Merge size mismatch")
                }
            }
        }

        if (merge.size == 0) {
            missing++
        }
    }

    override fun evalParam(param: AbraSiteParam) {
        check(param.offset == offset)

        ensure(param.size > 0)
        ensure(!param.isLatch)
        ensure(param.nullifyFalse == null)
        ensure(param.nullifyTrue == null)

        param.offset = offset
        offset += param.size
    }

    fun resolveRecursions(module: AbraModule) {
        // did we encounter any missing branch sizes?
        var lastMissing = 0
        while (missing != lastMissing) {
            // try to resolve missing ones by running another pass
            // over the ones that have not been done analyzing yet
            // and see if that results in less missing branch sizes
            lastMissing = missing
            missing = 0
            evalBlocks(module.branches)
        }

        if (missing != 0) {
            // still missing some branch sizes
            // must be due to recursion issues
            for (branch in module.branches) {
                if (branch.size() == 0) {
                    BaseExpr.logLine("Unresolved trit vector size in branch: " + branch.name)
                }
            }

            error("Recursion issue detected")
        }

        // quick sanity check if everything has a size now
        for (branch in module.branches) {
            for (site in branch.sites) {
                if (site.size == 0 && (site as AbraSiteMerge).inputs.size != 0) {
                    error("WTF?")
                }
            }

            for (site in branch.outputs) {
                if (site.size == 0 && (site as AbraSiteMerge).inputs.size != 0) {
                    error("WTF?")
                }
            }

            for (site in branch.latches) {
                if (site.size == 0 && site.references != 0) {
                    error("WTF?")
                }
            }
        }
    }

    companion object {
        private val constMin = "---------------------------"
        private val constOne = "111111111111111111111111111"
        private val constZero = "000000000000000000000000000"
        private val nullifyFalse = "-@@0@@1@@-@@0@@1@@-@@0@@1@@"
        private val nullifyTrue = "@@-@@0@@1@@-@@0@@1@@-@@0@@1"
        private val sanityCheck = true
    }
}
