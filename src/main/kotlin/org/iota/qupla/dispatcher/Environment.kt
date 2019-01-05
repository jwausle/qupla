package org.iota.qupla.dispatcher

import java.util.ArrayList

import org.iota.qupla.helper.TritVector
import org.iota.qupla.qupla.statement.FuncStmt

class Environment(var name: String) {
    private val entities = ArrayList<Entity>()
    var id: String? = null

    fun addEntity(func: FuncStmt, limit: Int) {
        val entity = Entity(func, limit)

        //TODO insert ordered by entity id to be deterministic
        synchronized(entities) {
            entities.add(entity)
        }
    }

    fun queueEntityEvents(value: TritVector, delay: Int) {
        synchronized(entities) {
            // create properly delayed events for all entities in this environment
            for (entity in entities) {
                entity.queueEvent(value, delay)
            }
        }
    }

    fun resetEntityLimits() {
        synchronized(entities) {
            for (entity in entities) {
                entity.resetLimit()
            }
        }
    }
}
