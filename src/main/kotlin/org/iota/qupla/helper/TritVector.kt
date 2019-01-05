package org.iota.qupla.helper

import org.iota.qupla.exception.CodeException

class TritVector {

    var name: String? = null
    private var offset: Int = 0
    private var size: Int = 0
    private var valueTrits: Int = 0
    private var vector: TritVectorBuffer? = null

    val isNull: Boolean
        get() = valueTrits == 0

    val isValue: Boolean
        get() = valueTrits == size()

    val isZero: Boolean
        get() {
            if (vector === zeroes) {
                return true
            }

            if (!isValue) {
                return false
            }

            for (i in 0 until size()) {
                if (trit(i) != '0') {
                    return false
                }
            }

            return true
        }

    constructor(copy: TritVector) {
        vector = copy.vector
        offset = copy.offset
        size = copy.size
        valueTrits = copy.valueTrits
    }

    constructor(trits: String) {
        size = trits.length
        valueTrits = size
        vector = TritVectorBuffer(size)
        for (i in 0 until size) {
            vector!!.buffer[i] = trits[i]
        }
    }

    constructor(size: Int, trit: Char) {
        this.size = size

        when (trit) {
            '@' -> vector = nulls

            '0' -> {
                vector = zeroes
                valueTrits = size
            }

            '-', '1' -> {
                if (size == 1) {
                    vector = singleTrits
                    offset = if (trit == '1') 1 else 0
                    valueTrits = 1
                    return
                }
                throw CodeException("Undefined initialization trit", null)
            }

            else -> throw CodeException("Undefined initialization trit", null)
        }

        vector!!.grow(size)
        while (vector!!.used < vector!!.buffer.size) {
            vector!!.buffer[vector!!.used++] = trit
        }
    }

    private constructor(lhs: TritVector, rhs: TritVector) {
        size = lhs.size() + rhs.size()
        valueTrits = lhs.valueTrits + rhs.valueTrits
        vector = TritVectorBuffer(size)

        copy(lhs, 0)
        copy(rhs, lhs.size())
    }

    private fun copy(src: TritVector, to: Int) {
        for (i in 0 until src.size()) {
            vector!!.buffer[to + i] = src.trit(i)
        }
    }

    fun display(mantissa: Int, exponent: Int): String {
        val varName = if (name != null) name!! + ": " else ""
        if (isValue) {
            return varName + "(" + displayValue(mantissa, exponent) + ") " + trits()
        }

        return if (isNull) {
            varName + "(NULL) " + trits()
        } else varName + "(***SOME NULL TRITS***) " + trits()

    }

    fun displayValue(mantissa: Int, exponent: Int): String {
        return if (exponent > 0 && mantissa > 0) {
            TritConverter.toFloat(trits(), mantissa, exponent)
        } else TritConverter.toDecimal(trits()).toString()

    }

    override fun equals(other: Any?): Boolean {
        if (other !is TritVector) {
            return false
        }

        val rhs = other as TritVector?
        if (size() != rhs!!.size()) {
            return false
        }

        for (i in 0 until size()) {
            if (trit(i) != rhs.trit(i)) {
                return false
            }
        }

        return true
    }

    fun size(): Int {
        return size
    }

    fun slice(start: Int, length: Int): TritVector {
        if (start < 0 || length < 0 || start + length > size()) {
            throw CodeException("Index out of range", null)
        }

        if (start == 0 && length == size()) {
            // slice the entire vector
            return this
        }

        val result = TritVector(this)
        result.offset += start
        result.size = length
        if (isValue) {
            result.valueTrits = length
            return result
        }

        if (isNull) {
            return result
        }

        // have to count non-null trits
        for (i in 0 until result.size()) {
            if (result.trit(i) != '@') {
                result.valueTrits++
            }
        }

        return result
    }

    fun slicePadded(start: Int, length: Int): TritVector? {
        // slices trit vector as if it was padded with infinite zeroes

        if (start + length <= size()) {
            // completely within range, normal slice
            return slice(start, length)
        }

        if (start >= size()) {
            // completely outside range, just zeroes
            return TritVector(length, '0')
        }

        val remain = size() - start
        val paddedZeroes = TritVector(length - remain, '0')
        return TritVector.concat(slice(start, remain), paddedZeroes)
    }

    override fun toString(): String {
        return display(0, 0)
    }

    fun trit(index: Int): Char {
        if (index < 0 || index >= size()) {
            throw CodeException("Index out of range", null)
        }

        return vector!!.buffer[offset + index]
    }

    fun trits(): String {
        return String(vector!!.buffer, offset, size())
    }

    companion object {
        private val nulls = TritVectorBuffer(0)
        private val singleTrits = TritVectorBuffer(2)
        private val zeroes = TritVectorBuffer(0)

        fun concat(lhs: TritVector?, rhs: TritVector?): TritVector? {
            if (lhs == null) {
                return rhs
            }

            if (rhs == null) {
                return lhs
            }

            // can we directly concatenate in lhs vector?
            if (lhs.offset + lhs.size() != lhs.vector!!.used || lhs.vector === nulls || lhs.vector === zeroes) {
                // nope, construct new vector

                // combine two null vectors?
                if (lhs.isNull && rhs.isNull) {
                    return TritVector(lhs.size() + rhs.size(), '@')
                }

                // combine two zero vectors?
                return if (lhs.vector === zeroes && rhs.vector === zeroes) {
                    TritVector(lhs.size() + rhs.size(), '0')
                } else TritVector(lhs, rhs)

            }

            // grow vector if necessary
            lhs.vector!!.grow(lhs.vector!!.used + rhs.size())

            // concatenate into lhs vector
            lhs.copy(rhs, lhs.vector!!.used)
            lhs.vector!!.used += rhs.size()

            // point to the new combined vector
            val result = TritVector(lhs)
            result.size += rhs.size()
            result.valueTrits += rhs.valueTrits
            return result
        }

        init {
            singleTrits.buffer[0] = '-'
            singleTrits.buffer[1] = '1'
        }
    }
}
