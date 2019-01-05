package org.iota.qupla.helper

import java.util.HashSet

class Verilog {
    var mergefuncs = HashSet<Int>()
    val prefix = "merge__"

    fun addMergeFuncs(context: BaseContext) {
        for (size in mergefuncs) {
            val funcName = prefix + size
            context.newline().append("function [" + (size * 2 - 1) + ":0] ").append(funcName).append("(").newline().indent()
            context.append("  input [" + (size * 2 - 1) + ":0] input1").newline()
            context.append(", input [" + (size * 2 - 1) + ":0] input2").newline()
            context.append(");").newline()
            context.append("begin").newline().indent()
            context.append(funcName).append(" = {").newline().indent()
            var first = true
            for (i in 0 until size) {
                val from = i * 2 + 1
                val to = i * 2
                context.append(if (first) "" else ": ").append("merge_lut_(input1[$from:$to], input2[$from:$to])").newline()
                first = false
            }
            context.undent()
            context.append("};").newline().undent()
            context.append("end").newline().undent()
            context.append("endfunction").newline()
        }
    }

    fun addMergeLut(context: BaseContext) {
        context.newline().append("x reg [0:0];").newline()
        context.newline().append("function [1:0] merge_lut_(").newline().indent()
        context.append("  input [1:0] input1").newline()
        context.append(", input [1:0] input2").newline()
        context.append(");").newline()
        context.append("begin").newline().indent()
        context.append("case ({input1, input2})").newline()
        context.append("4'b0000: merge_lut_ = 2'b00;").newline()
        context.append("4'b0001: merge_lut_ = 2'b01;").newline()
        context.append("4'b0010: merge_lut_ = 2'b10;").newline()
        context.append("4'b0011: merge_lut_ = 2'b11;").newline()
        context.append("4'b0100: merge_lut_ = 2'b01;").newline()
        context.append("4'b1000: merge_lut_ = 2'b10;").newline()
        context.append("4'b1100: merge_lut_ = 2'b11;").newline()
        context.append("4'b0101: merge_lut_ = 2'b01;").newline()
        context.append("4'b1010: merge_lut_ = 2'b10;").newline()
        context.append("4'b1111: merge_lut_ = 2'b11;").newline()
        context.append("default: merge_lut_ = 2'b00;").newline()
        context.append("         x <= 1;").newline()
        context.append("endcase").newline().undent()
        context.append("end").newline().undent()
        context.append("endfunction").newline()
    }

    fun appendVector(context: BaseContext, trits: String): BaseContext {
        val size = trits.length * 2
        context.append(size.toString() + "'b")
        for (i in 0 until trits.length) {
            when (trits[i]) {
                '0' -> context.append("01")
                '1' -> context.append("10")
                '-' -> context.append("11")
                '@' -> context.append("00")
            }
        }

        return context
    }
}
