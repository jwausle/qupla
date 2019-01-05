package org.iota.qupla.helper

class TritVectorBuffer(var used: Int) {

    var buffer: CharArray

    init {
        var newSize = INITIAL_SIZE
        while (newSize < used) {
            newSize *= 3
        }

        buffer = CharArray(newSize)
    }

    fun grow(needed: Int) {
        if (buffer.size < needed) {
            var newSize = buffer.size * 3
            while (newSize < needed) {
                newSize *= 3
            }

            val newBuffer = CharArray(newSize)
            for (i in 0 until used) {
                newBuffer[i] = buffer[i]
            }

            buffer = newBuffer
        }
    }

    override fun toString(): String {
        return String(buffer, 0, used)
    }

    companion object {
        private val INITIAL_SIZE = 27
    }
}
