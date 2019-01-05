package org.iota.qupla.qupla.context


import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.Stack

import org.iota.qupla.dispatcher.Entity
import org.iota.qupla.helper.StateValue
import org.iota.qupla.helper.TritVector
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
import org.iota.qupla.qupla.statement.FuncStmt
import org.iota.qupla.qupla.statement.LutStmt

class QuplaEvalContext : QuplaBaseContext() {

    var callNr: Int = 0
    var callTrail = ByteArray(4096)
    val stack = Stack<TritVector>()
    var stackFrame: Int = 0
    var value: TritVector? = null

    fun createEntityEffects(func: FuncStmt) {
        val entity = Entity(func, 1)
        // TODO Is value always not null here 'value!!'
        entity.queueEffectEvents(value!!)
    }

    override fun evalAssign(assign: AssignExpr) {
        // TODO Is .expr always not null here '.expr!!.'
        assign.expr!!.eval(this)
        log("     " + assign.name + " = ", stack.peek(), assign.expr!!)

        if (varNamesOnStack) {
            value = TritVector(value!!)
            value!!.name = assign.name
        }

        stack.push(value)

        // is this actually an assignment to a state variable?
        if (assign.stateIndex == 0) {
            // nope, done
            return
        }

        //  only assign non-null trits, other trits remain the same
        if (value!!.isNull) {
            // all null, just don't assign anything
            return
        }

        // save index of state variable to be able to distinguish
        // between multiple state vars in the same function
        // assuming no more than about 250 state variables here
        callTrail[callNr] = assign.stateIndex.toByte()

        val call = StateValue()
        call.path = callTrail
        call.pathLength = callNr + 1
        val stateValue = stateValues[call]

        // overwrite all trits?
        if (!value!!.isValue) {
            assign.warning("Partially overwriting state")

            // get existing state or all zero default state
            val trits = if (stateValue != null) stateValue.value else TritVector(value!!.size(), '0')
            // TODO Is .trits always not null here '.trits!!.'
            val buffer = CharArray(trits!!.size())
            for (i in 0 until value!!.size()) {
                // only overwrite non-null trits
                val trit = value!!.trit(i)
                buffer[i] = if (trit == '@') trits.trit(i) else trit
            }

            // use the merged result as the value to set the state to
            value = TritVector(String(buffer))
        }

        // state already saved?
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

    override fun evalConcat(concat: ConcatExpr) {
        // TODO Is .lhs always not null here '.lhs!!.'
        concat.lhs!!.eval(this)
        if (concat.rhs == null) {
            return
        }

        val lhsValue = value
        // TODO Is .rhs always not null here '.rhs!!.'
        concat.rhs!!.eval(this)
        value = TritVector.concat(lhsValue, value)
    }

    private fun evalConcatExprs(exprs: ArrayList<BaseExpr>) {
        var result: TritVector? = null
        for (expr in exprs) {
            expr.eval(this)
            result = TritVector.concat(result, value)
        }

        value = result
    }

    override fun evalConditional(conditional: CondExpr) {
        // TODO Is .condition always not null here '.condition!!.'
        conditional.condition!!.eval(this)
        if (conditional.trueBranch == null) {
            // not really a conditional operator
            // should have been optimized away
            return
        }

        val trit = value!!.trit(0)
        if (trit == '1') {
            // TODO Is .trueBranch always not null here '.trueBranch!!.'
            conditional.trueBranch!!.eval(this)
            return
        }

        if (conditional.falseBranch != null && trit == '-') {
            conditional.falseBranch!!.eval(this)
            return
        }

        // a non-bool condition value will result in a null return value
        // because both nullify calls will return null
        value = TritVector(conditional.size, '@')
    }

    fun evalEntity(entity: Entity, vector: TritVector): TritVector? {
        log("effect ", vector, entity.func)

        var start = 0
        for (param in entity.func.params) {
            value = vector.slicePadded(start, param.size)
            value!!.name = param.name
            stack.push(value)
            start += param.size
        }

        entity.func.eval(this)
        // TODO Is .returnExpr always not null here '.returnExpr!!'
        log("     return ", value, entity.func.returnExpr!!)

        stack.clear()
        return value
    }

    override fun evalFuncBody(func: FuncStmt) {}

    override fun evalFuncCall(call: FuncExpr) {
        //TODO initialize callTrail with some id to distinguish between
        //     top level functions so that we don't accidentally use the same
        //     call path from within different top level functions to store
        //     state data in the stateValues HashMap when call path is short

        if (callNr == 4000) {
            call.error("Exceeded function call nesting limit")
        }

        callTrail[callNr++] = call.callIndex.toByte()

        val newStackFrame = stack.size

        if (pushArguments(call)) {
            // TODO Is .func always not null here '.func!!.'
            log("short-circuit " + call.func!!.name)
            value = call.func!!.nullReturn
        } else {
            // TODO Is .func always not null here '.func!!.'
            log("call " + call.func!!.name)
            val oldStackFrame = stackFrame
            stackFrame = newStackFrame
            call.func!!.eval(this)
            stackFrame = oldStackFrame
            // TODO Is .returnExpr always not null here '.returnExpr!!'
            // TODO-AND Is .func always not null here '.func!!.'
            log("     return ", value, call.func!!.returnExpr!!)
        }

        stack.setSize(newStackFrame)
        callNr--

        interceptCall(call)
    }

    override fun evalFuncSignature(func: FuncStmt) {}

    override fun evalLutDefinition(lut: LutStmt) {}

    override fun evalLutLookup(lookup: LutExpr) {
        evalConcatExprs(lookup.args)

        // all trits non-null?
        if (value!!.isValue) {
            val lutIndex = LutStmt.index(value!!)
            // TODO Is .lut always not null here '.lut!!.'
            // TODO-AND Is .lookup always not null here '.lookup!!'
            value = lookup.lut!!.lookup!![lutIndex]
            if (value != null) {
                return
            }
        }
        // TODO Is .lut always not null here '.lut!!.'
        value = lookup.lut!!.undefined
    }

    override fun evalMerge(merge: MergeExpr) {
        // TODO Is .lhs always not null here '.lhs!!.'
        merge.lhs!!.eval(this)

        // if there is no rhs we return lhs
        if (merge.rhs == null) {
            return
        }

        // if lhs is null then we return rhs
        if (value!!.isNull) {
            merge.rhs!!.eval(this)
            return
        }

        // if rhs is null then we return lhs
        val savedLhsBranch = value
        merge.rhs!!.eval(this)
        if (value!!.isNull) {
            value = savedLhsBranch
            return
        }

        merge.rhs!!.error("Multiple non-null merge branches")
    }

    override fun evalSlice(slice: SliceExpr) {
        val `var` = stack[stackFrame + slice.stackIndex]
        if (slice.startOffset == null && slice.fields.size == 0) {
            value = `var`
            return
        }

        value = `var`.slice(slice.start, slice.size)
    }

    override fun evalState(state: StateExpr) {
        // save index of state variable to be able to distinguish
        // between multiple state vars in the same function
        callTrail[callNr] = state.stackIndex.toByte()

        val call = StateValue()
        call.path = callTrail
        call.pathLength = callNr + 1

        // if state was saved before set to that value otherwise set to zero
        val stateValue = stateValues[call]
        value = if (stateValue != null) stateValue.value else state.zero
        if (varNamesOnStack) {
            value = TritVector(value!!)
            value!!.name = state.name
        }

        stack.push(value)
        log("     state " + state.name + " = ", stack.peek(), state)
    }

    override fun evalType(type: TypeExpr) {
        evalConcatExprs(type.fields)
    }

    override fun evalVector(integer: IntegerExpr) {
        value = integer.vector
    }

    private fun interceptCall(call: FuncExpr) {
        // TODO Is .name always not null here '.name!!.'
        if (usePrint && call.name!!.startsWith("print_")) {
            val arg = call.args[0]
            // TODO Is .value always not null here 'value!!'
            // TODO-AND Is .typeInfo always not null here '.typeInfo!!.'
            BaseExpr.logLine("" + arg.typeInfo!!.display(value!!))
        }

        if (useBreak && call.name!!.startsWith("break")) {
            val arg = call.args[0]
            // TODO Is value always not null here 'value!!'
            // TODO-AND Is .typeInfo always not null here '.typeInfo!!.'
            BaseExpr.logLine("" + arg.typeInfo!!.display(value!!))
        }
    }

    fun log(text: String, vector: TritVector?, expr: BaseExpr) {
        // avoid converting vector to string, which is slow
        if (allowLog) {
            log("$text$vector : $expr")
        }
    }

    fun log(text: String) {
        if (allowLog) {
            BaseExpr.logLine(text)
        }
    }

    private fun pushArguments(call: FuncExpr): Boolean {
        var isAllNull = true
        for (i in call.args.indices) {
            val arg = call.args[i]
            arg.eval(this)

            val isNull = value!!.isNull
            // TODO Is .func always not null here '.func!!.'
            if (call.func!!.anyNull && isNull) {
                return true
            }

            isAllNull = isAllNull && isNull
            if (varNamesOnStack) {
                value = TritVector(value!!)
                value!!.name = call.func!!.params[i].name
            }

            stack.push(value)
            log("push ", value, arg)
        }

        return isAllNull
    }

    companion object {
        private val allowLog = false
        // note: stateValues needs to be static so that state is preserved between invocations
        private val stateValues = HashMap<StateValue, StateValue>()
        private val useBreak = true
        private val usePrint = true
        private val varNamesOnStack = true
    }
}

