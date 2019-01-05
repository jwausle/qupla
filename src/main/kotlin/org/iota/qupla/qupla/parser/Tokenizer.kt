package org.iota.qupla.qupla.parser

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.ArrayList
import java.util.HashMap

import org.iota.qupla.exception.CodeException

class Tokenizer {

    var colNr: Int = 0
    var lineNr: Int = 0
    val lines = ArrayList<String>()
    var module: Module? = null
    private var token: Token? = null

    fun currentToken(): Token? {
        return token
    }

    fun nextToken(): Token? {
        // end of file?
        if (lineNr == lines.size) {
            token = Token()
            token!!.source = module!!.currentSource
            token!!.lineNr = lineNr
            token!!.id = Token.TOK_EOF
            return token
        }

        val line = lines[lineNr]

        // skip whitespace
        while (colNr < line.length && Character.isWhitespace(line[colNr])) {
            colNr++
        }

        // end of line?
        if (colNr == line.length) {
            // process next line instead
            lineNr++
            colNr = 0
            return nextToken()
        }

        // found start of next token
        token = Token()
        token!!.source = module!!.currentSource
        token!!.lineNr = lineNr
        token!!.colNr = colNr
        token!!.text = line.substring(colNr)

        // first parse multi-character tokens

        // skip comment-to-end-of-line
        // TODO Is .text always not null here '.text!!.'
        if (token!!.text!!.startsWith("//")) {
            lineNr++
            colNr = 0
            return nextToken()
        }

        // parse single-character tokens
        val tokenId = tokenMap[token!!.text!!.substring(0, 1)]
        if (tokenId != null) {
            token!!.id = tokenId
            token!!.text = tokens[tokenId]
            colNr++
            return token
        }

        // number?
        var c = token!!.text!!.get(0)
        if (Character.isDigit(c)) {
            token!!.id = Token.TOK_NUMBER
            for (i in 0 until token!!.text!!.length) {
                c = token!!.text!!.get(i)
                if (Character.isDigit(c)) {
                    continue
                }

                if (token!!.id == Token.TOK_NUMBER && c == '.') {
                    token!!.id = Token.TOK_FLOAT
                    continue
                }

                token!!.text = token!!.text!!.substring(0, i)
                break
            }

            colNr += token!!.text!!.length
            return token
        }

        // identifier?
        if (Character.isLetter(c) || c == '_') {
            token!!.id = Token.TOK_NAME
            for (i in 1 until token!!.text!!.length) {
                c = token!!.text!!.get(i)
                if (Character.isLetterOrDigit(c) || Character.isDigit(c) || c == '_') {
                    continue
                }

                token!!.text = token!!.text!!.substring(0, i)
                break
            }

            colNr += token!!.text!!.length

            // check for keywords
            val keyword = tokenMap[token!!.text]
            if (keyword != null) {
                token!!.id = keyword
                return token
            }

            val symbol = symbolMap[token!!.text]
            if (symbol != null) {
                token!!.text = symbols[symbol]
                token!!.symbol = symbol
            } else {
                token!!.symbol = symbols.size
                symbolMap[token!!.text!!] = token!!.symbol
                symbols.add(token!!.text!!)
            }

            return token
        }
        // TODO Is token always not null here 'token!!'
        throw CodeException("Invalid character: '$c'", token!!)
    }

    fun readFile(`in`: File): ArrayList<String> {
        try {
            lines.clear()
            if (`in`.exists()) {
                val br = BufferedReader(FileReader(`in`))
                var line: String? = br.readLine()
                while (line != null) {
                    lines.add(line)
                    line = br.readLine()
                }
                br.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        lineNr = 0
        colNr = 0
        return lines
    }

    override fun toString(): String {
        val line = if (lineNr < lines.size) lines[lineNr].substring(colNr) else ""
        return (if (token == null) "" else token!!.text) + " | " + line
    }

    fun tokenId(): Int {
        return token!!.id
    }

    companion object {
        private val symbolMap = HashMap<String, Int>()
        private val symbols = ArrayList<String>()
        private val tokenMap = HashMap<String, Int>()
        private val tokens = arrayOfNulls<String>(50)

        private fun addToken(id: Int, name: String) {
            tokens[id] = name
            tokenMap[name] = id
        }

        init {
            addToken(Token.TOK_AFFECT, "affect")
            addToken(Token.TOK_DELAY, "delay")
            addToken(Token.TOK_EVAL, "eval")
            addToken(Token.TOK_FUNC, "func")
            addToken(Token.TOK_IMPORT, "import")
            addToken(Token.TOK_JOIN, "join")
            addToken(Token.TOK_LIMIT, "limit")
            addToken(Token.TOK_LUT, "lut")
            addToken(Token.TOK_NULL, "null")
            addToken(Token.TOK_RETURN, "return")
            addToken(Token.TOK_STATE, "state")
            addToken(Token.TOK_TEMPLATE, "template")
            addToken(Token.TOK_TEST, "test")
            addToken(Token.TOK_TYPE, "type")
            addToken(Token.TOK_USE, "use")

            addToken(Token.TOK_ARRAY_OPEN, "[")
            addToken(Token.TOK_ARRAY_CLOSE, "]")
            addToken(Token.TOK_FUNC_OPEN, "(")
            addToken(Token.TOK_FUNC_CLOSE, ")")
            addToken(Token.TOK_TEMPL_OPEN, "<")
            addToken(Token.TOK_TEMPL_CLOSE, ">")
            addToken(Token.TOK_GROUP_OPEN, "{")
            addToken(Token.TOK_GROUP_CLOSE, "}")
            addToken(Token.TOK_PLUS, "+")
            addToken(Token.TOK_MINUS, "-")
            addToken(Token.TOK_MUL, "*")
            addToken(Token.TOK_DIV, "/")
            addToken(Token.TOK_MOD, "%")
            addToken(Token.TOK_EQUAL, "=")
            addToken(Token.TOK_DOT, ".")
            addToken(Token.TOK_COMMA, ",")
            addToken(Token.TOK_CONCAT, "&")
            addToken(Token.TOK_MERGE, "|")
            addToken(Token.TOK_QUESTION, "?")
            addToken(Token.TOK_COLON, ":")
        }
    }
}
