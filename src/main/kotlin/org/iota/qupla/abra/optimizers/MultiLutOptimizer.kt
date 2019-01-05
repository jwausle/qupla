package org.iota.qupla.abra.optimizers

import java.util.ArrayList
import java.util.HashMap

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.AbraBlockLut
import org.iota.qupla.abra.block.base.AbraBaseBlock
import org.iota.qupla.abra.block.site.AbraSiteKnot
import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.abra.optimizers.base.BaseOptimizer
import org.iota.qupla.qupla.context.QuplaToAbraContext

class MultiLutOptimizer(context: QuplaToAbraContext, branch: AbraBlockBranch) : BaseOptimizer(context, branch) {
    var values = HashMap<AbraBaseSite, Char>()

    init {
        reverse = true
    }

    private fun generateLookupTable(master: AbraSiteKnot, slave: AbraSiteKnot, inputs: ArrayList<AbraBaseSite>): AbraBaseBlock {
        // initialize with 27 null trits
        val lookup = "@@@@@@@@@@@@@@@@@@@@@@@@@@@".toCharArray()

        val lookupSize = QuplaToAbraContext.powers[inputs.size]
        for (v in 0 until lookupSize) {
            var value = v
            for (i in inputs.indices) {
                values[inputs[i]] = "-01"[value % 3]
                value /= 3
            }

            val slaveTrit = lookupTrit(slave)
            if (slaveTrit == '@') {
                // null value, no need to continue
                continue
            }

            values[slave] = slaveTrit

            lookup[v] = lookupTrit(master)
        }

        // repeat the entries across the entire table if necessary
        var offset = lookupSize
        while (offset < 27) {
            for (i in 0 until lookupSize) {
                lookup[offset + i] = lookup[i]
            }
            offset += lookupSize
        }

        val lookupTable = String(lookup)

        val tmp = AbraSiteKnot()
        tmp.name = AbraBlockLut.unnamed(lookupTable)
        tmp.lut(context)

        // already exists?
        return if (tmp.block != null) {
            tmp.block!!
        } else {
            // TODO Is .name always not null here '.name!!'
            context.abraModule.addLut(tmp.name!!, lookupTable)
        }

        // new LUT, create it
    }

    private fun lookupTrit(lut: AbraSiteKnot): Char {
        var index = 0
        for (i in lut.inputs.indices) {
            val trit = values[lut.inputs[i]]
            val `val` = if (trit == '-') 0 else if (trit == '0') 1 else 2
            index += `val` * QuplaToAbraContext.powers[i]
        }

        // look up the trit in the lut lookup table
        return (lut.block as AbraBlockLut).lookup[index]
    }

    private fun mergeLuts(master: AbraSiteKnot, slave: AbraSiteKnot): Boolean {
        val inputs = ArrayList<AbraBaseSite>()

        // gather all unique master inputs (omit slave)
        for (input in master.inputs) {
            if (input !== slave && !inputs.contains(input)) {
                inputs.add(input)
                continue
            }
        }

        // gather all unique slave inputs
        for (input in slave.inputs) {
            if (!inputs.contains(input)) {
                inputs.add(input)
            }
        }

        // too many inputs to combine LUTs?
        if (inputs.size > 3) {
            return false
        }

        //TODO update block references (not just here)
        // get lookup table for combined LUT
        master.block = generateLookupTable(master, slave, inputs)

        // LUTs always need 3 inputs
        while (inputs.size < 3) {
            inputs.add(inputs[0])
        }

        // update master with new inputs
        for (i in 0..2) {
            master.inputs[i].references--
            master.inputs[i] = inputs[i]
            master.inputs[i].references++
        }

        return true
    }

    override fun processKnot(knot: AbraSiteKnot) {
        if (knot.block !is AbraBlockLut) {
            // nothing to optimize here
            return
        }

        // figure out if this LUT refers to another LUT
        for (input in knot.inputs) {
            if (input is AbraSiteKnot) {
                if (input.block is AbraBlockLut && mergeLuts(knot, input)) {
                    // this could have freed up another optimization possibility,
                    // so we restart the optimization from the end
                    index = branch.sites.size
                    return
                }
            }
        }
    }
}
