package org.iota.qupla.helper

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

abstract class BaseContext : Indentable() {
    private var file: File? = null
    protected var out: BufferedWriter? = null
    var string: String? = null
    private var writer: FileWriter? = null

    override fun appendify(text: String) {
        if (string != null) {
            string += text
            return
        }

        if (out != null) {
            fileWrite(text)
        }
    }

    protected fun fileClose() {
        try {
            if (out != null) {
                out!!.close()
            }

            if (writer != null) {
                writer!!.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    protected fun fileOpen(fileName: String) {
        try {
            file = File(fileName)
            writer = FileWriter(file!!)
            out = BufferedWriter(writer!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun fileWrite(text: String) {
        try {
            out!!.write(text)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}
