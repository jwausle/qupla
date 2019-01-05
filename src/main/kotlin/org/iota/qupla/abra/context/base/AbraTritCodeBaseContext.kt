package org.iota.qupla.abra.context.base

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.exception.CodeException

abstract class AbraTritCodeBaseContext : AbraBaseContext() {

    var buffer = CharArray(32)
    var bufferOffset: Int = 0

    val char: Char
        get() {
            when (trit) {
                '0' -> return getChar(0)

                '1' -> return getChar(1)
            }

            when (trit) {
                '0' -> return getChar(2)

                '1' -> return getChar(3)
            }

            return int.toChar()
        }

    val int: Int
        get() {
            var value = 0
            var trit = trit
            while (trit != '0') {
                if (trit == '-') {
                    value = value shl 1
                    trit = trit
                    continue
                }

                value = value shl 1 or 1
                trit = trit
            }

            return value
        }

    private val string2: String?
        get() {
            if (trit == '0') {
                return null
            }

            val length = int
            val buffer = CharArray(length)
            for (i in 0 until length) {
                buffer[i] = char
            }

            return String(buffer)
        }

    val trit: Char
        get() {
            if (bufferOffset >= buffer.size) {
                throw CodeException("Buffer overflow in getTrit")
            }

            return buffer[bufferOffset++]
        }

    fun evalBranchSites(branch: AbraBlockBranch) {
        branch.numberSites()

        putInt(branch.inputs.size)
        evalSites(branch.inputs)
        putInt(branch.sites.size)
        putInt(branch.outputs.size)
        putInt(branch.latches.size)
        evalSites(branch.sites)
        evalSites(branch.outputs)
        evalSites(branch.latches)
    }

    private fun getChar(codePageNr: Int): Char {
        var index = 0
        var power = 1
        for (i in 0..2) {
            val trit = trit
            if (trit != '0') {
                index += if (trit == '-') -power else power
            }

            power *= 3
        }

        return codePage[codePageNr][index]
    }

    fun getTrits(size: Int): String {
        bufferOffset += size
        if (bufferOffset > buffer.size) {
            throw CodeException("Buffer overflow in getTrits($size)")
        }

        return String(buffer, bufferOffset - size, size)
    }

    fun putChar(c: Char): AbraTritCodeBaseContext {
        // encode ASCII as efficient as possible
        for (page in codePage.indices) {
            val index = codePage[page].indexOf(c)
            if (index >= 0) {
                return putTrits(codePageId[page]).putTrits(AbraBaseContext.lutIndexes[index])
            }
        }

        return putTrits("--").putInt(c.toInt())
    }

    fun putInt(value: Int): AbraTritCodeBaseContext {
        sizes[if (value < 0) 299 else if (value < 298) value else 298]++

        // binary coded trit value
        var v = value
        while (v != 0) {
            putTrit(if (v and 1 == 0) '-' else '1')
            v = v shr 1
        }

        return putTrit('0')

        // here are some possible improvements to reduce size:
        //    // most-used trit value
        //    if (value < 2)
        //    {
        //      return putTrit(value == 0 ? '0' : '1');
        //    }
        //
        //    putTrit('-');
        //
        //    int v = value >> 1;
        //    if (v != 0)
        //    {
        //      v--;
        //      putTrit((v & 1) == 0 ? '-' : '1');
        //      for (v >>= 1; v != 0; v >>= 1)
        //      {
        //        putTrit((v & 1) == 0 ? '-' : '1');
        //      }
        //    }
        //
        //    return putTrit('0');
    }

    fun putString(text: String?): AbraTritCodeBaseContext {
        if (text == null) {
            return putTrit('0')
        }

        putTrit('1')
        putInt(text.length)
        for (i in 0 until text.length) {
            putChar(text[i])
        }

        return this
    }

    fun putTrit(trit: Char): AbraTritCodeBaseContext {
        if (bufferOffset < buffer.size) {
            buffer[bufferOffset++] = trit
            return this
        }

        // expand buffer
        return putTrits("" + trit)
    }

    fun putTrits(trits: String): AbraTritCodeBaseContext {
        if (bufferOffset + trits.length <= buffer.size) {
            val copy = trits.toCharArray()
            System.arraycopy(copy, 0, buffer, bufferOffset, copy.size)
            bufferOffset += copy.size
            return this
        }

        // double buffer size and try again
        val old = buffer
        buffer = CharArray(old.size * 2)
        System.arraycopy(old, 0, buffer, 0, bufferOffset)
        return putTrits(trits)
    }

    override fun toString(): String {
        // display 40 trit tail end of buffer
        val start = if (bufferOffset > 40) bufferOffset - 40 else 0
        return bufferOffset.toString() + " " + String(buffer, start, bufferOffset - start)
    }

    companion object {
        // ASCII code pages, with best encoding for text
        private val codePage: Array<String> = arrayOf(" ABCDEFGHIJKLMNOPQRSTUVWXYZ", ".abcdefghijklmnopqrstuvwxyz", "0123456789_$,=+-*/%&|^?:;@#", "[]{}()<>!~`'\"\\\r\n\t")
        private val codePageId: Array<String> = arrayOf("0", "1", "-0", "-1")

        private val sizes = IntArray(300)
    }
}
