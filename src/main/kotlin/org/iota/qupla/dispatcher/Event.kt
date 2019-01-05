package org.iota.qupla.dispatcher

import java.util.ArrayList

import org.iota.qupla.helper.TritVector

class Event(var entity: Entity, var value: TritVector, delay: Int) {
    var quant: Int = 0

    init {
        this.quant = currentQuant + delay
        synchronized(queue) {
            queue.add(this)
        }
    }

    fun dispatch() {
        entity.runWave(value)
    }

    companion object {
        var currentQuant: Int = 0
        private val queue = ArrayList<Event>()

        fun dispatchCurrentQuantEvents(): Boolean {
            if (queue.size == 0) {
                return false
            }

            // process every event in the queue that is meant to run in the current quant
            // we may optimize this in the future by ordering the queue by quant number
            var i = 0
            while (i < queue.size) {
                val event = queue[i]
                if (event.quant <= currentQuant) {
                    synchronized(queue) {
                        queue.removeAt(i)
                    }
                    event.dispatch()
                    continue
                }

                i++
            }

            currentQuant++
            return true
        }
    }
}
