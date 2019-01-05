package org.iota.qupla.qupla.statement

import java.util.ArrayList

import org.iota.qupla.helper.TritVector
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer
import org.iota.qupla.qupla.statement.helper.LutEntry

class LutStmt : BaseExpr {

    val entries = ArrayList<LutEntry>()
    var inputSize: Int = 0
    var lookup: Array<TritVector?>? = null
    var undefined: TritVector? = null

    constructor(copy: LutStmt) : super(copy) {

        entries.addAll(copy.entries)
        inputSize = copy.inputSize
        lookup = copy.lookup
        undefined = copy.undefined
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        expect(tokenizer, Token.TOK_LUT, "lut")

        val lutName = expect(tokenizer, Token.TOK_NAME, "LUT name")
        name = lutName.text
        // TODO Is module always not null here 'module!!.'
        module!!.checkDuplicateName(module!!.luts, this)

        expect(tokenizer, Token.TOK_GROUP_OPEN, "'{'")

        do {
            entries.add(LutEntry(tokenizer))
        } while (tokenizer.tokenId() != Token.TOK_GROUP_CLOSE)

        tokenizer.nextToken()
    }

    override fun analyze() {
        val first = entries[0]
        inputSize = first.inputs.length
        size = first.outputs.length
        lookup = arrayOfNulls(tableSize[inputSize])
        for (entry in entries) {
            entry.analyze()
            if (entry.inputs.length != inputSize) {
                entry.error("Expected $inputSize input trits")
            }

            if (entry.outputs.length != size) {
                entry.error("Expected $size output trits")
            }

            val input = TritVector(entry.inputs)
            val lutIndex = index(input)
            // TODO Is lookup always not null here 'lookup!!'
            if (lookup!![lutIndex] != null) {
                entry.error("Duplicate input trits")
            }

            lookup!![lutIndex] = TritVector(entry.outputs)
        }

        undefined = TritVector(size, '@')
    }

    override fun clone(): BaseExpr {
        return LutStmt(this)
    }

    override fun toString(): String {
        return "lut $name"
    }

    companion object {
        private val tableSize = intArrayOf(0, 3, 9, 27, 81, 243, 729, 2187, 6561, 19683)

        fun index(value: TritVector): Int {
            var index = 0
            for (i in 0 until value.size()) {
                index *= 3
                val trit = value.trit(i)
                if (trit != '-') {
                    index += if (trit == '0') 1 else 2
                }
            }

            return index
        }
    }
}
