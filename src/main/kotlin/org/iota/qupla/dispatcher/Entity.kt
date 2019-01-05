package org.iota.qupla.dispatcher

import java.util.ArrayList

import org.iota.qupla.helper.TritVector
import org.iota.qupla.qupla.context.QuplaEvalContext
import org.iota.qupla.qupla.expression.AffectExpr
import org.iota.qupla.qupla.statement.FuncStmt

class Entity(var func: FuncStmt, var limit: Int) {

    val effects = ArrayList<Effect>()
    var id: String? = null
    var invoked: Int = 0

    init {

        // determine the list of effects that this entity produces
        for (envExpr in func.envExprs) {
            if (envExpr is AffectExpr) {
                // TODO Is .name always not null here '.name!!'
                val env = Dispatcher.getEnvironment(envExpr.name!!)
                // TODO Is .delay always not null here '.delay!!.'
                addEffect(env, if (envExpr.delay == null) 0 else envExpr.delay!!.size)
            }
        }
    }

    fun addEffect(env: Environment, delay: Int) {
        val effect = Effect(env, delay)

        //TODO insert ordered by env id to be deterministic
        effects.add(effect)
    }

    fun queueEffectEvents(value: TritVector) {
        // all effects for this entity have been predetermined already
        // from the metadata, so we just need to queue them as events
        for (effect in effects) {
            effect.queueEnvironmentEvents(value)
        }
    }

    fun queueEvent(value: TritVector, delay: Int) {
        // queue an event for this entity with proper delay
        val event = Event(this, value, delay)
        if (delay == 0 && invoked < limit) {
            // can do another invocation during the current quant
            invoked++
        } else {
            // invocation limit exceeded, schedule for next quant
            event.quant++
        }
    }

    fun resetLimit() {
        invoked = 0
    }

    fun runWave(inputValue: TritVector) {
        // have the entity process the value
        val returnValue = evalContext.evalEntity(this, inputValue)

        // queue any effects that the entity triggered
        // TODO Is returnValue always not null here 'returnValue!!.'
        queueEffectEvents(returnValue!!)
    }

    companion object {
        val evalContext = QuplaEvalContext()
    }
}
