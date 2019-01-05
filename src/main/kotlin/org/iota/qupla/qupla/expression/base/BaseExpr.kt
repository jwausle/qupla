package org.iota.qupla.qupla.expression.base

import org.iota.qupla.exception.CodeException
import org.iota.qupla.qupla.context.QuplaPrintContext
import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.parser.Module
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer
import org.iota.qupla.qupla.statement.TypeStmt
import org.iota.qupla.qupla.statement.UseStmt
import java.util.*

abstract class BaseExpr {

    var module: Module? = null
    var name: String? = null
    var next: BaseExpr? = null
    var origin: Token? = null
    var size: Int = 0
    var stackIndex: Int = 0
    var typeInfo: TypeStmt? = null

    protected constructor() {}

    protected constructor(copy: BaseExpr) {
        module = currentModule
        name = copy.name
        origin = copy.origin
        size = copy.size
        stackIndex = copy.stackIndex
        typeInfo = copy.typeInfo
    }

    protected constructor(tokenizer: Tokenizer) {
        module = tokenizer.module
        origin = tokenizer.currentToken()
    }

    protected constructor(tokenizer: Tokenizer, origin: Token?) {
        module = tokenizer.module
        this.origin = origin ?: tokenizer.currentToken()
    }

    abstract fun analyze()

    protected fun analyzeType(): TypeStmt {
        if (currentUse != null) {
            // TODO Is .template always not null here '.template!!.'
            for (type in currentUse!!.template!!.types) {
                if (type.name == name) {
                    // TODO Is .typeInfo always not null here '.typeInfo!!.'
                    if (type.typeInfo!!.size == 0) {
                        error("Did not analyze: $name")
                    }

                    size = type.typeInfo!!.size
                    return type.typeInfo!!
                }
            }
        }

        val type = findEntity(TypeStmt::class.java, "type") as TypeStmt?
        size = type!!.size
        return type
    }

    fun clone(expr: BaseExpr?): BaseExpr? {
        return expr?.clone()
    }

    abstract fun clone(): BaseExpr

    fun cloneArray(lhs: ArrayList<BaseExpr>, rhs: ArrayList<BaseExpr>) {
        for (expr in rhs) {
            lhs.add(expr.clone())
        }
    }

    fun error(token: Token?, message: String): CodeException {
        throw if (token == null) CodeException(message)
        else throw CodeException(message, token)
    }

    fun error(message: String) {
        // TODO Is origin always not null here 'origin!!'
        error(origin!!, message)
    }

    open fun eval(context: QuplaBaseContext) {
        context.evalBaseExpr(this)
    }

    fun expect(tokenizer: Tokenizer, type: Int, what: String): Token {
        val token = tokenizer.currentToken()
        // TODO Is token always not null here 'token!!.'
        if (token!!.id != type) {
            error(token, "Expected $what")
        }

        tokenizer.nextToken()
        return token
    }

    fun findEntity(classId: Class<*>, what: String): BaseExpr? {
        for (entity in module!!.entities(classId)) {
            if (entity.name == name) {
                if (entity.size == 0) {
                    entity.analyze()
                }

                return entity
            }
        }

        var externEntity: BaseExpr? = null
        for (extern in module!!.modules) {
            for (entity in extern.entities(classId)) {
                if (entity.name == name) {
                    if (externEntity == null || externEntity === entity) {
                        externEntity = entity
                        break
                    }

                    error("Ambiguous " + what + " name: " + name + " in " + externEntity.module + " and " + entity.module)
                }
            }
        }

        if (externEntity == null) {
            error("Undefined $what name: $name")
        }

        return externEntity
    }

    fun log(text: String) {
        val name = javaClass.name
        logLine(name.substring(name.lastIndexOf(".") + 1) + ": " + text)
    }

    open fun optimize(): BaseExpr {
        return this
    }

    override fun toString(): String {
        val oldString = printer.string
        printer.string = String(CharArray(0))
        toStringify()
        val ret = printer.string
        printer.string = oldString
        // TODO Is ret always not null here 'ret!!'
        return ret!!
    }

    open fun toStringify() {
        eval(printer)
    }

    fun warning(message: String) {
        log("WARNING: $message")
    }

    companion object {
        val SEPARATOR = "_"
        var callNr: Int = 0
        var constTypeInfo: TypeStmt? = null
        var currentModule: Module? = null
        var currentUse: UseStmt? = null
        var currentUseIndex: Int = 0
        var printer = QuplaPrintContext()
        val scope = ArrayList<BaseExpr>()

        fun logLine(text: String) {
            println(text)
        }
    }
}
