package org.iota.qupla.qupla.context

import java.util.ArrayList
import java.util.Stack

import org.iota.qupla.abra.AbraModule
import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.AbraBlockLut
import org.iota.qupla.abra.block.site.AbraSiteKnot
import org.iota.qupla.abra.block.site.AbraSiteLatch
import org.iota.qupla.abra.block.site.AbraSiteMerge
import org.iota.qupla.abra.block.site.AbraSiteParam
import org.iota.qupla.abra.block.site.base.AbraBaseSite
import org.iota.qupla.abra.context.AbraAnalyzeContext
import org.iota.qupla.abra.context.AbraDebugTritCodeContext
import org.iota.qupla.abra.context.AbraPrintContext
import org.iota.qupla.abra.context.AbraToVerilogContext
import org.iota.qupla.abra.context.AbraTritCodeContext
import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.expression.AssignExpr
import org.iota.qupla.qupla.expression.ConcatExpr
import org.iota.qupla.qupla.expression.CondExpr
import org.iota.qupla.qupla.expression.FuncExpr
import org.iota.qupla.qupla.expression.IntegerExpr
import org.iota.qupla.qupla.expression.LutExpr
import org.iota.qupla.qupla.expression.MergeExpr
import org.iota.qupla.qupla.expression.SliceExpr
import org.iota.qupla.qupla.expression.StateExpr
import org.iota.qupla.qupla.expression.TypeExpr
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Module
import org.iota.qupla.qupla.statement.FuncStmt
import org.iota.qupla.qupla.statement.LutStmt
import org.iota.qupla.qupla.statement.helper.LutEntry

class QuplaToAbraContext : QuplaBaseContext() {
    var abraModule = AbraModule()
    var bodies: Int = 0
    var branch: AbraBlockBranch? = null
    var lastSite: AbraBaseSite? = null
    var stack = Stack<AbraBaseSite>()
    var stmt: BaseExpr? = null

    private fun addSite(site: AbraBaseSite) {
        if (stmt != null) {
            site.stmt = stmt
            stmt = null
        }

        branch!!.sites.add(site)
        lastSite = site
    }

    override fun eval(module: Module) {
        super.eval(module)

        abraModule.optimize(this)

        AbraPrintContext().eval(abraModule)
        AbraToVerilogContext().eval(abraModule)
        AbraAnalyzeContext().eval(abraModule)
        AbraDebugTritCodeContext().eval(abraModule)
        AbraTritCodeContext().eval(abraModule)
    }

    override fun evalAssign(assign: AssignExpr) {
        // TODO Is .expr always not null here '.expr!!.'
        assign.expr!!.eval(this)
        // TODO Is .lastSite always not null here 'lastSite!!.'
        lastSite!!.varName = assign.name
        stack.push(lastSite)

        if (assign.stateIndex != 0) {
            // move last site to latches
            branch!!.sites.removeAt(branch!!.sites.size - 1)
            // TODO Is lastSite always not null here 'lastSite!!.'
            branch!!.latches.add(lastSite!!)
            lastSite!!.isLatch = true

            // forward placeholder state site to actual state site
            val state = stack[assign.stateIndex] as AbraSiteLatch
            state.latch = lastSite
        }
    }

    override fun evalConcat(concat: ConcatExpr) {
        val exprs = ArrayList<BaseExpr>()
        // TODO Is .lhs always not null here '..lhs!!'
        exprs.add(concat.lhs!!)
        if (concat.rhs != null) {
            exprs.add(concat.rhs!!)
        }

        evalConcatExprs(exprs)
    }

    fun evalConcatExprs(exprs: ArrayList<BaseExpr>) {
        val site = AbraSiteKnot()
        for (expr in exprs) {
            expr.eval(this)
            site.size += expr.size
            if (expr is ConcatExpr) {
                site.inputs.addAll((lastSite as AbraSiteKnot).inputs)
            } else {
                // TODO Is .lastSite always not null here '.lastSite!!'
                site.inputs.add(lastSite!!)
            }
        }

        site.concat(this)
        addSite(site)
    }

    override fun evalConditional(conditional: CondExpr) {
        // TODO Is .condition always not null here '.condition!!.'
        conditional.condition!!.eval(this)
        if (conditional.trueBranch == null) {
            // not really a conditional operator
            // should have been optimized away
            return
        }

        val condition = lastSite

        conditional.trueBranch!!.eval(this)
        val trueBranch = lastSite

        // note that actual insertion of nullifyTrue(condition, ...)
        // is done after nullify position has been optimized
        // TODO Is .trueBranch always not null here '.trueBranch!!.'
        trueBranch!!.nullifyTrue = condition

        // create a site for trueBranch ( | falseBranch)
        val merge = AbraSiteMerge()
        merge.size = conditional.size
        merge.inputs.add(trueBranch)

        if (conditional.falseBranch != null) {
            conditional.falseBranch!!.eval(this)
            val falseBranch = lastSite

            // note that actual insertion of nullifyFalse(condition, ...)
            // is done after nullify position has been optimized
            falseBranch!!.nullifyFalse = condition

            merge.inputs.add(falseBranch)
        }

        addSite(merge)
    }

    override fun evalFuncBody(func: FuncStmt) {
        stack.clear()

        branch = abraModule.branches[bodies++]

        for (param in func.params) {
            val site = AbraSiteParam()
            site.from(param)
            site.varName = param.name
            stack.push(site)
            branch!!.addInput(site)
        }

        for (stateExpr in func.stateExprs) {
            stateExpr.eval(this)
            // TODO Is lastSite always not null here 'lastSite!!'
            branch!!.latches.add(lastSite!!)
            lastSite!!.isLatch = true
        }

        for (assignExpr in func.assignExprs) {
            stmt = assignExpr
            assignExpr.eval(this)
        }

        stmt = func.returnExpr
        // TODO Is .returnExpr always not null here '.returnExpr!!.'
        func.returnExpr!!.eval(this)

        // move last site to outputs
        branch!!.sites.removeAt(branch!!.sites.size - 1)
        // TODO Is lastSite always not null here 'lastSite!!'
        branch!!.outputs.add(lastSite!!)

        branch = null
    }

    override fun evalFuncCall(call: FuncExpr) {
        val site = AbraSiteKnot()
        site.from(call)

        for (arg in call.args) {
            arg.eval(this)
            // TODO Is lastSite always not null here 'lastSite!!'
            site.inputs.add(lastSite!!)
        }

        site.branch(this)
        addSite(site)
    }

    override fun evalFuncSignature(func: FuncStmt) {
        branch = AbraBlockBranch()
        branch!!.origin = func
        branch!!.name = func.name
        branch!!.size = func.size
        abraModule.addBranch(branch)
    }

    override fun evalLutDefinition(lut: LutStmt) {
        // note: lut output size can be >1, so we need a lut per output trit
        for (tritNr in 0 until lut.size) {
            val lookup = "@@@@@@@@@@@@@@@@@@@@@@@@@@@".toCharArray()

            for (entry in lut.entries) {
                // build index for this entry in lookup table
                var index = 0
                for (i in 0 until entry.inputs.length) {
                    val trit = entry.inputs[i]
                    val `val` = if (trit == '-') 0 else if (trit == '0') 1 else 2
                    index += `val` * powers[i]
                }

                // set corresponding character
                lookup[index] = entry.outputs[tritNr]
            }

            // repeat the entries across the entire table if necessary
            val lookupSize = powers[lut.inputSize]
            var offset = lookupSize
            while (offset < 27) {
                for (i in 0 until lookupSize) {
                    lookup[offset + i] = lookup[i]
                }
                offset += lookupSize
            }

            val block = abraModule.addLut(lut.name + "_" + tritNr, String(lookup))
            block.origin = lut
        }
    }

    override fun evalLutLookup(lookup: LutExpr) {
        val args = AbraSiteKnot()
        for (arg in lookup.args) {
            arg.eval(this)
            // TODO Is lastSite always not null here '.lastSite!!'
            args.inputs.add(lastSite!!)
        }

        val concat = AbraSiteKnot()
        for (i in 0 until lookup.size) {
            val site = AbraSiteKnot()
            site.from(lookup)
            site.name += "_$i"
            site.size = 1
            site.inputs.addAll(args.inputs)
            while (site.inputs.size < 3) {
                site.inputs.add(site.inputs[0])
            }

            site.lut(this)
            addSite(site)

            concat.size += 1
            concat.inputs.add(site)
        }

        if (concat.inputs.size > 1) {
            concat.concat(this)
            addSite(concat)
        }
    }

    override fun evalMerge(merge: MergeExpr) {
        // TODO Is 'merge.rsh == null' the right check? used is merge.lhs
        if (merge.rhs == null) {
            // TODO Is .lhs always not null here '.lhs!!.'
            merge.lhs!!.eval(this)
            return
        }

        val site = AbraSiteMerge()
        site.from(merge)

        // TODO Is .lhs always not null here '.lhs!!.'
        merge.lhs!!.eval(this)
        if (merge.lhs is MergeExpr) {
            site.inputs.addAll((lastSite as AbraSiteMerge).inputs)
        } else {
            // TODO Is lastSite always not null here 'lastSite!'
            site.inputs.add(lastSite!!)
        }

        merge.rhs!!.eval(this)
        if (merge.rhs is MergeExpr) {
            site.inputs.addAll((lastSite as AbraSiteMerge).inputs)
        } else {
            // TODO Is lastSite always not null here 'lastSite!!'
            site.inputs.add(lastSite!!)
        }

        addSite(site)
    }

    override fun evalSlice(slice: SliceExpr) {
        val varSite = stack[slice.stackIndex]

        if (slice.startOffset == null && slice.fields.size == 0) {
            // entire variable, use single-input merge
            val site = AbraSiteMerge()
            site.from(slice)
            site.inputs.add(varSite)
            addSite(site)
            return
        }

        // slice of variable, use correct slice function
        val site = AbraSiteKnot()
        site.from(slice)
        site.inputs.add(varSite)
        site.slice(this, varSite.size, slice.start)
        addSite(site)
    }

    override fun evalState(state: StateExpr) {
        // create placeholder for latch
        val site = AbraSiteLatch()
        site.from(state)

        lastSite = site
        stack.push(site)
    }

    override fun evalType(type: TypeExpr) {
        evalConcatExprs(type.fields)
    }

    override fun evalVector(integer: IntegerExpr) {
        val site = AbraSiteKnot()
        site.from(integer)
        site.inputs.add(branch!!.inputs[0])
        site.vector(this, integer.vector)
        addSite(site)
    }

    companion object {
        val powers = intArrayOf(1, 3, 9, 27)
    }
}
