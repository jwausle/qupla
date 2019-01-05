package org.iota.qupla.dispatcher

import java.util.HashMap

import org.iota.qupla.qupla.expression.JoinExpr
import org.iota.qupla.qupla.parser.Module

class Dispatcher(modules: Collection<Module>) {

    init {
        // add all functions in all modules that have join statements
        // as entities to their corresponding environment
        for (module in modules) {
            for (func in module.funcs) {
                for (envExpr in func.envExprs) {
                    if (envExpr is JoinExpr) {
                        // TODO Is .name always not null here '.name!!'
                        val environment = getEnvironment(envExpr.name!!)
                        // TODO Is .limit always not null here '.limit!!.'
                        environment.addEntity(func, if (envExpr.limit == null) 1 else envExpr.limit!!.size)
                    }
                }
            }
        }
    }

    fun finished() {
        synchronized(environments) {
            environments.clear()
        }
    }

    fun runQuants() {
        // keep running as long as there are still events in the queue
        while (Event.dispatchCurrentQuantEvents()) {
            // reset all invocation limits for the next quant
            synchronized(environments) {
                for (environment in environments.values) {
                    environment.resetEntityLimits()
                }
            }
        }
    }

    companion object {
        private val environments = HashMap<String, Environment>()

        fun getEnvironment(name: String): Environment {
            // find or create the named environment
            synchronized(environments) {
                val env = environments[name]
                if (env != null) {
                    return env
                }

                val newEnv = Environment(name)
                environments[newEnv.name] = newEnv
                return newEnv
            }
        }
    }
}
