package org.iota.qupla.qupla.parser

class Token {

    var colNr: Int = 0
    var id: Int = 0
    var lineNr: Int = 0
    var source: Source? = null
    var symbol: Int = 0
    var text: String? = null

    override fun toString(): String {
        return "[" + (lineNr + 1) + "," + (colNr + 1) + "] " + id + " | " + text
    }

    companion object {
        val TOK_AFFECT = 1
        val TOK_ARRAY_CLOSE = 2
        val TOK_ARRAY_OPEN = 3
        val TOK_COLON = 4
        val TOK_COMMA = 5
        val TOK_COMMENT = 6
        val TOK_CONCAT = 7
        val TOK_DELAY = 8
        val TOK_DIV = 9
        val TOK_DOT = 10
        val TOK_EOF = 11
        val TOK_EQUAL = 12
        val TOK_EVAL = 12
        val TOK_FLOAT = 13
        val TOK_FUNC = 14
        val TOK_FUNC_CLOSE = 15
        val TOK_FUNC_OPEN = 16
        val TOK_GROUP_CLOSE = 17
        val TOK_GROUP_OPEN = 18
        val TOK_IMPORT = 19
        val TOK_JOIN = 20
        val TOK_LIMIT = 21
        val TOK_LUT = 22
        val TOK_MERGE = 23
        val TOK_MINUS = 24
        val TOK_MOD = 25
        val TOK_MUL = 26
        val TOK_NAME = 27
        val TOK_NULL = 28
        val TOK_NUMBER = 29
        val TOK_PLUS = 30
        val TOK_QUESTION = 31
        val TOK_RETURN = 32
        val TOK_STATE = 33
        val TOK_TEMPLATE = 34
        val TOK_TEMPL_CLOSE = 35
        val TOK_TEMPL_OPEN = 36
        val TOK_TEST = 37
        val TOK_TYPE = 38
        val TOK_USE = 39
    }
}
