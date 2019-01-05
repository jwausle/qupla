package org.iota.qupla.abra.context

import java.util.ArrayList

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
import org.iota.qupla.helper.BaseContext
import org.iota.qupla.helper.Verilog

class AbraToVerilogContext : AbraBaseContext() {
    var branchSites = ArrayList<AbraBaseSite>()
    private val verilog = Verilog()

    private fun appendVector(trits: String): BaseContext {
        return verilog.appendVector(this, trits)
    }

    override fun eval(module: AbraModule) {
        fileOpen("AbraVerilog.txt")

        super.eval(module)

        verilog.addMergeLut(this)
        verilog.addMergeFuncs(this)

        fileClose()
    }

    override fun evalBranch(branch: AbraBlockBranch) {
        if (branch.specialType == AbraBaseBlock.TYPE_SLICE) {
            return
        }

        newline()

        branchSites.clear()
        branchSites.addAll(branch.inputs)
        branchSites.addAll(branch.sites)
        branchSites.addAll(branch.outputs)
        branchSites.addAll(branch.latches)

        // give unnamed sites a name
        for (i in branchSites.indices) {
            val site = branchSites[i]
            if (site.varName == null) {
                site.varName = "v_$i"
            }
        }

        // TODO solve '!!' not null branch.name
        val funcName = branch.name!!
        append("function [" + (branch.size * 2 - 1) + ":0] ").append(funcName).append("(").newline().indent()

        var first = true
        for (input in branch.inputs) {
            append(if (first) "  " else ", ")
            first = false
            // TODO is this 'input.varName?:""' the right handle if .varName=null?
            append("input [" + (input.size * 2 - 1) + ":0] ").append(input.varName?:"").newline()
        }

        append(");").newline()

        for (site in branch.sites) {
            // TODO is this 'site.varName?:""' the right handle if .varName=null?
            append("reg [" + (site.size * 2 - 1) + ":0] ").append(site.varName?:"").append(";").newline()
        }

        if (branch.sites.size != 0) {
            newline()
        }

        append("begin").newline().indent()

        for (site in branch.sites) {
            if (site.references == 0) {
                continue
            }
            // TODO is this 'site.varName?:""' the right handle if .varName=null?
            append(site.varName?:"").append(" = ")
            site.eval(this)
            append(";").newline()
        }

        append(funcName).append(" = ")
        if (branch.outputs.size != 1) {
            append("{ ")
        }

        first = true
        for (output in branch.outputs) {
            append(if (first) "" else ", ")
            first = false
            output.eval(this)
        }

        if (branch.outputs.size != 1) {
            append(" }")
        }

        append(";").newline()

        undent().append("end").newline().undent()
        append("endfunction").newline()

        //TODO branch.latches!
    }

    override fun evalImport(imp: AbraBlockImport) {}

    override fun evalKnot(knot: AbraSiteKnot) {
        // TODO Is block always not null here 'knot.block!!.'
        if (knot.block!!.specialType == AbraBaseBlock.TYPE_SLICE) {
            evalKnotSlice(knot)
            return
        }
        // TODO Is block always not null here 'knot.block!!.'
        // TODO-AND Is this '.name?:""' the right handle if .name=null?
        append(knot.block!!.name?:"")

        var branch: AbraBlockBranch? = null
        if (knot.block is AbraBlockLut) {
            append("_lut")
        } else {
            branch = knot.block as AbraBlockBranch
        }

        var first = true
        for (i in knot.inputs.indices) {
            val input = knot.inputs[i]
            // TODO Is this '.varName?:""' the right handle if .varName=null?
            append(if (first) "(" else ", ").append(input.varName?:"")
            first = false

            if (branch == null) {
                continue
            }

            val param = branch.inputs[i]
            if (input.size > param.size) {
                // must take slice
                append("[" + param.size + ":0]")
            }
        }

        append(")")
    }

    private fun evalKnotSlice(knot: AbraSiteKnot) {
        if (knot.inputs.size > 1) {
            append("{ ")
        }

        var totalSize = 0
        var first = true
        for (input in knot.inputs) {
            // TODO Is this '.varName?:""' the right handle if .varName=null?
            append(if (first) "" else " : ").append(input.varName?:"")
            first = false
            totalSize += input.size
        }

        if (knot.inputs.size > 1) {
            append(" }")
        }

        val branch = knot.block as AbraBlockBranch
        val input = branch.inputs[branch.inputs.size - 1] as AbraSiteParam
        if (totalSize > input.size) {
            val start = input.offset * 2
            val end = start + input.size * 2 - 1
            append("[$end:$start]")
        }
    }

    override fun evalLatch(latch: AbraSiteLatch) {}

    override fun evalLut(lut: AbraBlockLut) {
        val lutName = lut.name + "_lut"
        append("function [1:0] ").append(lutName).append("(").newline().indent()

        var first = true
        for (i in 0..2) {
            append(if (first) "  " else ", ")
            first = false
            append("input [1:0] ").append("p$i").newline()
        }
        append(");").newline()

        append("begin").newline().indent()

        append("case ({p0, p1, p2})").newline()

        for (i in 0..26) {
            val trit = lut.lookup[i]
            if (trit == '@') {
                continue
            }

            appendVector(AbraBaseContext.lutIndexes[i]).append(": ").append(lutName).append(" = ")
            appendVector("" + trit).append(";").newline()
        }

        append("default: ").append(lutName).append(" = ")
        appendVector("@").append(";").newline()
        append("endcase").newline().undent()

        append("end").newline().undent()
        append("endfunction").newline().newline()
    }

    override fun evalMerge(merge: AbraSiteMerge) {
        if (merge.inputs.size == 1) {
            // single-input merge just returns value
            val input = merge.inputs[0]
            // TODO Is this '.varName?:""' the right handle if .varName=null?
            append(input.varName?:"")
            return
        }

        verilog.mergefuncs.add(merge.size)

        for (i in 0 until merge.inputs.size - 1) {
            val input = merge.inputs[i]
            // TODO Is this '.varName?:""' the right handle if .varName=null?
            append(verilog.prefix + merge.size + "(").append(input.varName?:"").append(", ")
        }

        val input = merge.inputs[merge.inputs.size - 1]
        // TODO Is this '.varName?:""' the right handle if .varName=null?
        append(input.varName?:"")

        for (i in 0 until merge.inputs.size - 1) {
            append(")")
        }
    }

    override fun evalParam(param: AbraSiteParam) {}
}
