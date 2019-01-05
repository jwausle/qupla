package org.iota.qupla.dispatcher

import org.iota.qupla.helper.TritVector

class Effect(var environment: Environment, var delay: Int) {

    fun queueEnvironmentEvents(value: TritVector) {
        // transform the effect into one or more entity events in the event queue
        environment.queueEntityEvents(value, delay)
    }
}
