package org.iota.qupla.helper

abstract class Indentable {
    private var mustIndent: Boolean = false
    private var spaces: Int = 0

    open fun append(text: String): Indentable {
        if (text.length == 0) {
            return this
        }

        if (mustIndent) {
            mustIndent = false
            for (i in 0 until spaces) {
                appendify(" ")
            }
        }

        appendify(text)
        return this
    }

    protected abstract fun appendify(text: String)

    fun indent(): Indentable {
        spaces += 2
        return this
    }

    fun newline(): Indentable {
        append("\n")
        mustIndent = true
        return this
    }

    fun undent(): Indentable {
        spaces -= 2
        return this
    }
}
