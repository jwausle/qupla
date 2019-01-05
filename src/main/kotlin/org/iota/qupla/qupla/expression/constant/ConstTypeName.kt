package org.iota.qupla.qupla.expression.constant

import java.util.ArrayList

import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer

class ConstTypeName : BaseExpr {
    constructor(copy: ConstTypeName) : super(copy) {

        if (BaseExpr.currentUse != null) {
            // while cloning function replace placeholder type names
            // with the actual type names as defined in the use statement
            val typeArgs = BaseExpr.currentUse!!.typeInstantiations[BaseExpr.currentUseIndex]
            // TODO Is .template always not null here '.template!!.'
            for (i in BaseExpr.currentUse!!.template!!.params.indices) {
                val param = BaseExpr.currentUse!!.template!!.params[i]
                if (name == param.name) {
                    val typeName = typeArgs[i]
                    name = typeName.name
                    return
                }
            }
        }
    }

    constructor(tokenizer: Tokenizer) : super(tokenizer) {

        val typeName = expect(tokenizer, Token.TOK_NAME, "type name")
        name = typeName.text
    }

    override fun analyze() {
        typeInfo = analyzeType()
        // TODO Is typeInfo always not null here 'typeInfo!!.'
        name = typeInfo!!.name
    }

    override fun clone(): BaseExpr {
        return ConstTypeName(this)
    }
}
