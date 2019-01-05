package org.iota.qupla.qupla.statement

import org.iota.qupla.helper.TritVector
import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer
import org.iota.qupla.qupla.statement.helper.TritStructDef
import org.iota.qupla.qupla.statement.helper.TritVectorDef

class TypeStmt : BaseExpr {
    var isFloat: Boolean = false
    var struct: TritStructDef? = null
    var vector: TritVectorDef? = null

    constructor(copy: TypeStmt) : super(copy) {

        isFloat = copy.isFloat
        // TODO Is .struct always not null here '.struct!!'
        struct = if (copy.struct == null) null else TritStructDef(copy.struct!!)
        // TODO Is .vector always not null here '.vector!!'
        vector = if (copy.vector == null) null else TritVectorDef(copy.vector!!)
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        expect(tokenizer, Token.TOK_TYPE, "type")

        val typeName = expect(tokenizer, Token.TOK_NAME, "type name")
        name = typeName.text
        // TODO Is module always not null here 'module!!.'
        module!!.checkDuplicateName(module!!.types, this)

        if (tokenizer.tokenId() == Token.TOK_GROUP_OPEN) {
            struct = TritStructDef(tokenizer, typeName)
            return
        }

        vector = TritVectorDef(tokenizer, typeName)
    }

    override fun analyze() {
        if (size > 0) {
            return
        }

        if (struct != null) {
            struct!!.analyze()
            size = struct!!.size

            if (struct!!.fields.size == 2) {
                val mantissa = struct!!.fields[0]
                if (mantissa.name == "mantissa") {
                    val exponent = struct!!.fields[1]
                    isFloat = exponent.name == "exponent"
                }
            }

            return
        }

        vector!!.analyze()
        size = vector!!.size
    }

    override fun clone(): BaseExpr {
        return TypeStmt(this)
    }

    fun display(value: TritVector): String {
        if (!isFloat) {
            return value.display(0, 0)
        }

        val mantissa = struct!!.fields[0]
        val exponent = struct!!.fields[1]
        return value.display(mantissa.size, exponent.size)
    }

    fun displayValue(value: TritVector): String {
        if (!isFloat) {
            return value.displayValue(0, 0)
        }

        val mantissa = struct!!.fields[0]
        val exponent = struct!!.fields[1]
        return value.displayValue(mantissa.size, exponent.size)
    }

    override fun eval(context: QuplaBaseContext) {
        context.evalTypeDefinition(this)
    }

    override fun toString(): String {
        return if (vector != null) {
            super.toString()
        } else "type $name { ... }"

    }
}
