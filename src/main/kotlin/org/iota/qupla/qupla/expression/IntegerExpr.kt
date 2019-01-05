package org.iota.qupla.qupla.expression

import org.iota.qupla.helper.TritConverter
import org.iota.qupla.helper.TritVector
import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class IntegerExpr : BaseExpr {
    var vector: TritVector

    constructor(copy: IntegerExpr) : super(copy) {

        vector = TritVector(copy.vector)
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        vector = TritVector(0, '@')

        name = ""
        if (tokenizer.tokenId() == Token.TOK_MINUS) {
            name = "-"
            tokenizer.nextToken()
        }

        if (tokenizer.tokenId() == Token.TOK_NUMBER || tokenizer.tokenId() == Token.TOK_FLOAT) {
            // TODO Is .currentToken always not null here '.currentToken()!!.'
            name += tokenizer.currentToken()!!.text
            tokenizer.nextToken()
        }
    }

    override fun analyze() {
        removeLeadingZeroes()

        // are we assigning to a known type?
        if (BaseExpr.constTypeInfo == null) {
            // TODO Is name always not null here 'name!!'
            vector = TritVector(TritConverter.fromDecimal(name!!))
            size = vector.size()
            return
        }

        // is this a float type?
        // TODO Is .constTypeInfo always not null here '.constTypeInfo!!.'
        if (BaseExpr.constTypeInfo!!.isFloat) {
            // TODO Is .struct always not null here '.struct!!.'
            val mantissa = BaseExpr.constTypeInfo!!.struct!!.fields[0]
            val exponent = BaseExpr.constTypeInfo!!.struct!!.fields[1]
            // TODO Is name always not null here 'name!!'
            vector = TritVector(TritConverter.fromFloat(name!!, mantissa.size, exponent.size))
            size = mantissa.size + exponent.size
            return
        }

        // TODO Is name always not null here 'name!!'
        if (name!!.indexOf('.') >= 0) {
            error("Unexpected float constant: $name")
        }
        // TODO Is name always not null here 'name!!'
        vector = TritVector(TritConverter.fromDecimal(name!!))
        size = vector.size()
        // TODO Is .constTypeInfo always not null here '.constTypeInfo!!.'
        if (size > BaseExpr.constTypeInfo!!.size) {
            error("Constant value '" + name + "' exceeds " + BaseExpr.constTypeInfo!!.size + " trits")
        }

        // TODO Is .constTypeInfo always not null here '.constTypeInfo!!.'
        size = BaseExpr.constTypeInfo!!.size
        if (vector.size() < size) {
            // TODO Is return-value always not null here ')!!.'
            vector = TritVector.concat(vector, TritVector(size - vector.size(), '0'))!!
        }
    }

    override fun clone(): BaseExpr {
        return IntegerExpr(this)
    }

    override fun eval(context: QuplaBaseContext) {
        context.evalVector(this)
    }

    private fun removeLeadingZeroes() {
        if (name == "-") {
            // just a single trit
            return
        }

        // strip and save starting minus sign
        // TODO Is name always not null here 'name!!.'
        val negative = name!!.startsWith("-")
        if (negative) {
            name = name!!.substring(1)
        }

        // strip leading zeroes
        while (name!!.startsWith("0")) {
            name = name!!.substring(1)
        }

        // decimal point?
        val dot = name!!.indexOf('.')
        if (dot >= 0) {
            // strip trailing zeroes
            while (name!!.endsWith("0")) {
                name = name!!.substring(0, name!!.length - 1)
            }

            // strip trailing dot
            if (name!!.endsWith(".")) {
                name = name!!.substring(0, name!!.length - 1)
            }
        }

        // re-insert at least one leading zero?
        if (name!!.length == 0 || name!!.startsWith(".")) {
            name = "0$name"
        }

        // restore minus sign?
        if (negative && name != "0") {
            name = "-$name"
        }
    }
}
