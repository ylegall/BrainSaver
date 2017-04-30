package org.ygl

import java.nio.file.Files
import java.nio.file.Paths


class Interpreter
{
    private var pc = 0
    private var dp = 0
    private var data: ByteArray = ByteArray(30000)
    private val text = ArrayList<Char>()

    fun growArray() {
        val newArray = ByteArray((data.size * 1.7) as Int)
        System.arraycopy(data, 0, newArray, 0, data.size)
        data = newArray
    }

    fun readChar(): Byte {
        val newChar: Char? = readLine()?.get(0)
        return newChar?.toByte() ?: throw Exception("null input")
    }

    fun eval(str: String?): String {
        val buff = StringBuilder()
        if (str != null) {
            for (c in str.toCharArray()) {
                text.add(c)
            }
        }
        while (hasNextStep()) {
            val output = step()
            if (output != null) buff.append(output)
        }
        return buff.toString()
    }

    private fun step(): Char? {
        val c = text[pc]
        var output: Char? = null
        when (c) {
            '>' -> {
                dp++
                if (dp >= data.size) growArray()
            }
            '<' -> {
                dp--
                if (dp < 0) dp = 0
            }
            '+' -> data[dp] = (data[dp] + 1).toByte()
            '-' -> data[dp] = (data[dp] - 1).toByte()
            '.' -> output = data[dp].toChar()
            ',' -> data[dp] = readChar()
            '[' -> {
                if (data[dp] == 0.toByte()) {
                    var stack = 1
                    while (stack > 0) {
                        pc++
                        when (text[pc]) {
                            '[' -> stack++
                            ']' -> stack--
                        }
                    }
                }
            }
            ']' -> {
                if (data[dp] != 0.toByte()) {
                    var stack = 1
                    while (stack > 0) {
                        pc--
                        when (text[pc]) {
                            '[' -> stack--
                            ']' -> stack++
                        }
                    }
                }
            }
            else -> {
            }
        }
        pc++
        return output
    }

    fun hasNextStep(): Boolean {
        return pc < text.size
    }
}

fun main(args: Array<String>) {
    if (args.size < 1) {
        println("missing input file")
        return
    }
    val interpreter = Interpreter()
    val code = String(Files.readAllBytes(Paths.get("output.txt")))
//    val code = String(Files.readAllBytes(Paths.get("bf-test.txt")))
    println(interpreter.eval(code))
}

//fun run(text: CharArray) {
//
//    while (true) {
//        val c = text[pc]
//        when (c) {
//            '>' -> {
//                dp++
//                if (dp >= data.size) growArray()
//            }
//            '<' -> {
//                dp--
//                if (dp < 0) dp = 0
//            }
//            '+' -> data[dp] = (data[dp] + 1).toByte()
//            '-' -> data[dp] = (data[dp] - 1).toByte()
//            '.' -> print(data[dp].toChar())
//            ',' -> data[dp] = readChar()
//            '[' -> {
//                if (data[dp] == 0.toByte()) {
//                    var stack = 1
//                    while (stack > 0) {
//                        pc++
//                        when (text[pc]) {
//                            '[' -> stack++
//                            ']' -> stack--
//                        }
//                    }
//                }
//            }
//            ']' -> {
//                if (data[dp] != 0.toByte()) {
//                    var stack = 1
//                    while (stack > 0) {
//                        pc--
//                        when (text[pc]) {
//                            '[' -> stack--
//                            ']' -> stack++
//                        }
//                    }
//                }
//            }
//            else -> {
//            }
//        }
//        pc++
//
//        if (pc >= text.size) break
//    }
//}

