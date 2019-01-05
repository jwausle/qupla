package org.iota.qupla.qupla.expression

import java.util.ArrayList

import org.iota.qupla.qupla.context.base.QuplaBaseContext
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.parser.Token
import org.iota.qupla.qupla.parser.Tokenizer
import org.iota.qupla.qupla.statement.TypeStmt

class TypeExpr : BaseExpr {
    val fields = ArrayList<BaseExpr>()

    constructor(copy: TypeExpr) : super(copy) {

        cloneArray(fields, copy.fields)
    }

    constructor(tokenizer: Tokenizer, identifier: Token) : super(tokenizer, identifier) {

        name = identifier.text

        expect(tokenizer, Token.TOK_GROUP_OPEN, "'{'")

        do {
            fields.add(FieldExpr(tokenizer))
        } while (tokenizer.tokenId() != Token.TOK_GROUP_CLOSE)

        tokenizer.nextToken()
    }

    override fun analyze() {
        val type = analyzeType()
        if (type.struct == null) {
            error("Expected structured trit vector name")
            // TODO why no return here? NPE occur always
        }

        typeInfo = type

        for (field in fields) {
            field.analyze()

            // check that this is a field name
            var found = false
            for (structField in type.struct!!.fields) {
                if (field.name == structField.name) {
                    found = true
                    break
                }
            }

            if (!found) {
                field.error("Unknown field name: " + field.name)
            }
        }

        // check that all subfields of the struct vector are assigned
        // also check the assigned size
        // also sort the fields in the same order as in the struct vector
        val sortedFields = ArrayList<BaseExpr>()
        for (structField in type.struct!!.fields) {
            var found = false
            for (field in fields) {
                if (field.name == structField.name) {
                    found = true
                    if (field.size != structField.size) {
                        field.error("Structured trit field size mismatch: " + field.name)
                    }

                    for (sortedField in sortedFields) {
                        if (field.name == sortedField.name) {
                            field.error("Duplicate field name: " + field.name)
                        }
                    }

                    sortedFields.add(field)
                }
            }

            if (!found) {
                error("Missing assignment to field: " + structField.name)
            }
        }

        fields.clear()
        fields.addAll(sortedFields)
    }

    override fun clone(): BaseExpr {
        return TypeExpr(this)
    }

    override fun eval(context: QuplaBaseContext) {
        context.evalType(this)
    }
}
