package org.iota.qupla.helper

class StateValue {
    var hash: Int = 0
    var path: ByteArray? = null
    var pathLength: Int = 0
    var value: TritVector? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other == null || javaClass != other.javaClass) {
            return false
        }

        val rhs = other as StateValue?
        if (pathLength != rhs!!.pathLength) {
            return false
        }

        for (i in 0 until pathLength) {
            if (path!![i] != rhs.path!![i]) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        if (hash == 0) {
            // cache hash value
            hash = 1
            for (i in 0 until pathLength) {
                hash = hash * 31 + path!![i]
            }

            // make sure not to calculate again
            if (hash == 0) {
                hash = -1
            }
        }

        return hash
    }
}
