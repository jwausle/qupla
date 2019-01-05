package org.iota.qupla.exception

import org.iota.qupla.qupla.parser.Token

class CodeException(message: String, val token: Token? = null) : RuntimeException (message) {
}
