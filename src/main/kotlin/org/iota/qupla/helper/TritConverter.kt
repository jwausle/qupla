package org.iota.qupla.helper

import java.math.BigInteger

import org.iota.qupla.exception.CodeException
import java.util.*

object TritConverter {
    private val powerDigits = ArrayList<Int>()
    private val powers = ArrayList<BigInteger>()
    private val three = BigInteger("3")

    fun fromDecimal(decimal: String): String {
        if (decimal.length == 1 && decimal[0] < '2') {
            return decimal
        }

        // take abs(name)
        val negative = decimal.startsWith("-")
        val value = if (negative) decimal.substring(1) else decimal

        // convert to unbalanced ternary
        val buffer = CharArray(value.length * 3)
        val quotient = value.toCharArray()
        for (i in quotient.indices) {
            quotient[i] = (quotient[i].toInt() - '0'.toInt()).toChar()
        }

        var qLength = quotient.size

        var bLength = 0
        while (qLength != 1 || quotient[0].toInt() != 0) {
            val vLength = qLength
            qLength = 0
            var digit = quotient[0]
            if (digit.toInt() >= 3 || vLength == 1) {
                quotient[qLength++] = (digit.toInt() / 3).toChar()
                digit = (digit.toInt() % 3).toChar()
            }

            for (index in 1 until vLength) {
                digit = (digit.toInt() * 10 + quotient[index].toInt()).toChar()
                quotient[qLength++] = (digit.toInt() / 3).toChar()
                digit = (digit.toInt() % 3).toChar()
            }

            buffer[bLength++] = digit
        }

        // convert unbalanced to balanced ternary
        // note that we negate the result if necessary in the same pass
        var carry = 0
        for (i in 0 until bLength) {
            when (buffer[i].toInt() + carry) {
                0 -> {
                    buffer[i] = '0'
                    carry = 0
                }

                1 -> {
                    buffer[i] = if (negative) '-' else '1'
                    carry = 0
                }

                2 -> {
                    buffer[i] = if (negative) '1' else '-'
                    carry = 1
                }

                3 -> {
                    buffer[i] = '0'
                    carry = 1
                }
            }
        }

        if (carry != 0) {
            buffer[bLength++] = if (negative) '-' else '1'
        }

        return String(buffer, 0, bLength)
    }

    fun fromFloat(value: String, manSize: Int, expSize: Int): String {
        val dot = value.indexOf('.')
        if (dot < 0) {
            // handle integer constant

            if (value == "0") {
                // special case: both mantissa and exponent zero
                return zeroes(manSize + expSize)
            }

            // get minimum trit vector that represents integer
            val trits = fromDecimal(value)

            // make sure it fits in the mantissa
            if (trits.length > manSize) {
                throw CodeException("Mantissa '$value' exceeds $manSize trits")
            }

            // shift all significant trits to normalize
            val mantissa = zeroes(manSize - trits.length) + trits
            return makeFloat(mantissa, trits.length, expSize)
        }

        // handle float constant

        // use BigInteger arithmetic to convert the value
        // <integer> * 10^-<decimals> * 3^0
        // into the following without losing too much precision
        // <ternary> * 10^0 * 3^<exponent>
        // we do that by calculating a minimum necessary ternary exponent,
        // then multiply by that <exponent> and divide by 10^<decimals>
        // after that it becomes a matter of normalizing and rounding the
        // ternary representation of the result

        val decimals = value.length - dot - 1
        val integer = value.substring(0, dot) + value.substring(dot + 1)
        val intValue = BigInteger(integer)
        val tenPower = BigInteger("1" + zeroes(decimals))

        // do a rough estimate of how many trits we will need at a minimum
        // add at least 20 trits of wiggling room to reduce rounding errors
        // also: calculate 3 trits per decimal just to be sure
        val exponent = -(manSize + 20 + 3 * decimals)
        val ternary = intValue.multiply(getPower(-exponent)).divide(tenPower)
        val trits = fromDecimal(ternary.toString())

        // take <manSize> most significant trits
        val mantissa = trits.substring(trits.length - manSize)
        return makeFloat(mantissa, exponent + trits.length, expSize)
    }

    fun fromLong(decimal: Long): String {
        //TODO replace this inefficient lazy-ass code :-P
        return fromDecimal("" + decimal)
    }

    private fun getPower(nr: Int): BigInteger {
        if (nr >= powers.size) {
            if (powers.size == 0) {
                powers.add(BigInteger("1"))
                powerDigits.add(1)
            }

            var big = powers[powers.size - 1]
            for (i in powers.size..nr) {
                big = big.multiply(three)
                powers.add(big)
                powerDigits.add(big.toString().length)
            }
        }

        return powers[nr]
    }

    private fun makeFloat(mantissa: String, exponent: Int, expSize: Int): String {
        val trits = fromLong(exponent.toLong())

        // make sure exponent fits
        if (trits.length > expSize) {
            throw CodeException("Exponent '$exponent' exceeds $expSize trits")
        }

        return mantissa + trits + zeroes(expSize - trits.length)
    }

    fun toDecimal(trits: String): BigInteger {
        var result = BigInteger("0")
        for (i in 0 until trits.length) {
            val c = trits[i]
            if (c != '0') {
                val power = getPower(i)
                result = if (c == '-') result.subtract(power) else result.add(power)
            }
        }

        return result
    }

    fun toFloat(trits: String, manSize: Int, expSize: Int): String {
        Objects.requireNonNull(expSize,"Unused toFloat.expSize=$expSize parameter")
        // find first significant trit
        var significant = 0
        while (significant < manSize && trits[significant] == '0') {
            significant++
        }

        // special case: all zero trits in mantissa
        if (significant == manSize) {
            return "0.0"
        }

        // shift the significant trits of the mantissa to the left to get
        // its integer representation (we will need to correct the exponent)
        val mantissa = trits.substring(significant, manSize)
        val integer = toDecimal(mantissa)

        // get exponent and correct with mantissa shift factor
        val exponent = TritConverter.toInt(trits.substring(manSize)) - mantissa.length
        if (exponent == 0) {
            // simple case: 3^0 equals 1, just return integer
            return integer.toString() + ".0"
        }

        return if (exponent > 0) {
            integer.multiply(getPower(exponent)).toString() + ".0"
        } else toFloatWithFraction(integer, exponent, manSize)

    }

    private fun toFloatWithFraction(integer: BigInteger, exponent: Int, manSize: Int): String {
        if (integer.signum() < 0) {
            return "-" + toFloatWithFraction(integer.negate(), exponent, manSize)
        }

        getPower(manSize)
        val digits = powerDigits[manSize] - 1

        val lhsLength = integer.toString().length
        val power = getPower(-exponent)
        val rhsLength = powerDigits[-exponent]
        val extra = if (lhsLength < rhsLength) rhsLength - lhsLength else 0
        val mul = integer.multiply(BigInteger("1" + zeroes(digits + extra)))
        val div = mul.divide(power)
        val divResult = div.toString()
        val decimal = divResult.length - digits - extra
        var last = divResult.length - 1
        while (last > 0 && last > decimal && divResult[last - 1] == '0') {
            last--
        }

        if (decimal < 0) {
            return "0." + zeroes(-decimal) + divResult.substring(0, last)
        }

        val fraction = if (last == decimal) "0" else divResult.substring(decimal, last)
        return if (decimal == 0) {
            "0.$fraction"
        } else divResult.substring(0, decimal) + "." + fraction

    }

    fun toInt(trits: String): Int {
        var result = 0
        var power = 1
        for (i in 0 until trits.length) {
            val trit = trits[i]
            if (trit != '0') {
                result += if (trit == '-') -power else power
            }

            power *= 3
        }

        return result
    }

    private fun zeroes(size: Int): String {
        return TritVector(size, '0').trits()
    }
}
