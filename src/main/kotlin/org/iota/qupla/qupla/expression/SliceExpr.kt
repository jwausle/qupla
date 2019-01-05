package org.iota.qupla.qupla.expression

import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.expression.constant.ConstExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer
import java.util.*

class SliceExpr : BaseExpr {
    var endOffset: BaseExpr? = null
    val fields = ArrayList<BaseExpr>()
    var start: Int = 0
    var startOffset: BaseExpr? = null

    constructor(copy: SliceExpr) : super(copy) {

        endOffset = clone(copy.endOffset)
        fields.addAll(copy.fields)
        start = copy.start
        startOffset = clone(copy.startOffset)
    }

    constructor(tokenizer: Tokenizer, identifier: Token) : super(tokenizer, identifier) {

        name = identifier.text

        while (tokenizer.tokenId() == Token.TOK_DOT) {
            tokenizer.nextToken()

            fields.add(NameExpr(tokenizer, "field name"))
        }

        if (tokenizer.tokenId() == Token.TOK_ARRAY_OPEN) {
            tokenizer.nextToken()

            startOffset = ConstExpr(tokenizer).optimize()

            when (tokenizer.tokenId()) {
                Token.TOK_COLON -> {
                    tokenizer.nextToken()
                    endOffset = ConstExpr(tokenizer).optimize()
                }
            }

            expect(tokenizer, Token.TOK_ARRAY_CLOSE, "']'")
        }
    }

    override fun analyze() {
        analyzeVar()

        if (startOffset == null) {
            return
        }

        startOffset!!.analyze()
        if (startOffset!!.size < 0 || startOffset!!.size >= size) {
            startOffset!!.error("Invalid slice start offset")
        }

        // at least a single indexed trit
        val offset = startOffset!!.size
        var end = offset

        if (endOffset != null) {
            endOffset!!.analyze()
            if (offset + endOffset!!.size > size) {
                endOffset!!.error("Invalid slice size (" + offset + "+" + endOffset!!.size + ">" + size + ")")
            }

            end = offset + endOffset!!.size - 1
        }

        start += offset
        size = end - offset + 1
    }

    private fun analyzeVar() {
        for (i in BaseExpr.scope.indices.reversed()) {
            var `var` = BaseExpr.scope[i]
            if (`var`.name == name) {
                if (`var` is AssignExpr) {
                    // does this assignment assign to a state variable?
                    val stateIndex = `var`.stateIndex
                    if (stateIndex != 0) {
                        // reference the actual state variable instead
                        `var` = BaseExpr.scope[stateIndex]
                    }
                }

                stackIndex = `var`.stackIndex
                typeInfo = `var`.typeInfo
                size = `var`.size

                analyzeVarFields(`var`)
                return
            }
        }

        error("Cannot find variable: $name")
    }

    private fun analyzeVarFields(`var`: BaseExpr) {
        Objects.requireNonNull(`var`,"Unused analyzeVarFields.var=$`var` parameter")
        // start with entire vector
        start = 0

        var fieldPath = name
        for (field in fields) {
            if (typeInfo == null || typeInfo!!.struct == null) {
                error("Expected structured trit vector: $fieldPath")
                // why no return here? NPE happens always
            }

            var found = false
            for (structField in typeInfo!!.struct!!.fields) {
                if (structField.name == field.name) {
                    found = true
                    typeInfo = structField.typeInfo
                    size = structField.size
                    break
                }

                start += structField.size
            }

            if (!found) {
                error("Invalid structured trit vector field name: $field")
            }

            fieldPath += ".$field"
        }
    }

    override fun clone(): BaseExpr {
        return SliceExpr(this)
    }

    override fun eval(context: QuplaBaseContext) {
        context.evalSlice(this)
    }
}
