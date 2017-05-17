package org.ygl

import java.io.*
import java.util.*
import java.util.stream.Collectors

const val MAX_BYTE = 255.toByte()
const val ZERO_BYTE = 0.toByte()

class InterpreterOptions
(
    val isWrapping: Boolean = false,
    val isVerbose: Boolean = false,
    val predefinedInput: String = "",
    val memorySize: Int = 30000
)

val DEFAULT_OPTIONS = InterpreterOptions()

/**
 *
 */
class BFInterpreter(
        val text: String,
        val outputStream: OutputStream = System.out,
        val options: InterpreterOptions = DEFAULT_OPTIONS
) : AutoCloseable
{
    constructor(inputStream: InputStream, outputStream: OutputStream = System.out, options: InterpreterOptions) : this(
            inputStream.use {
                BufferedReader(InputStreamReader(it))
                    .lines()
                    .parallel()
                    .collect(Collectors.joining("\n"))
            },
            outputStream,
            options
    )

    private var pc = 0
    private var col = 0
    private var memory = ByteArray(options.memorySize)
    private var userInput = ArrayDeque<Char>(options.predefinedInput.length)

    init {
        userInput.addAll(options.predefinedInput.asIterable())
    }

    // stats
    private var dp = 0
    private var line = 0
    private var opCount = 0

    override fun close() {
        outputStream.close()
    }

    fun eval() {
        while (pc < text.length) {
            val c = text[pc]
            updateCounts(c)
            evalOp(c)
            pc++
        }

        if (options.isVerbose) {
            println("\nprogram stats:")
            println("opCount:    $opCount")
        }
    }

    private fun evalOp(c: Char) {
        when (c) {
            '+' -> inc()
            '-' -> dec()
            '>' -> dp++
            '<' -> dp--
            '.' -> printChar()
            ',' -> readChar()
            '[' -> {
                if (memory[dp] == ZERO_BYTE) {
                    jumpForward()
                }
            }
            ']' -> {
                if (memory[dp] != ZERO_BYTE) {
                    jumpBack()
                }
            }
            '`' -> {
                val sb = StringBuilder()
                pc++
                while (text[pc] != '`') {
                    sb.append(text[pc])
                    pc++
                }
                print(sb.toString())
                println(memory[dp].toString())
            }
            else -> {
                opCount--
            }
        }
        opCount++
    }

    private fun updateCounts(c: Char) {
        when (c) {
            '\n' -> {
                line++
                col = 0
            } else -> {
                col++
            }
        }
    }

    private fun jumpForward() {
        var nest = 1
        while (nest > 0) {
            pc++
            val c = text[pc]
            updateCounts(c)
            when (c) {
                '[' -> nest++
                ']' -> nest--
            }
        }
    }

    private fun jumpBack() {
        var nest = 1
        while (nest > 0) {
            pc--
            val c = text[pc]
            updateCounts(c)
            when (c) {
                ']' -> nest++
                '[' -> nest--
            }
        }
    }

    private fun readChar() {
        if (!userInput.isEmpty()) {
            val c = userInput.pop()
            //println("read value '$c'")
            memory[dp] = c?.toByte() ?: throw Exception("null input char")
        } else {
            print("waiting for input: ")
            val c = System.`in`.read().toByte()
            memory[dp] = c
            println()
        }
    }

    private fun printChar() {
        this.outputStream.write(memory[dp].toInt())
    }

    private fun inc() {
        if (options.isWrapping) {
            memory[dp] = if (memory[dp] == MAX_BYTE) {
                0.toByte()
            } else {
                (memory[dp] + 1).toByte()
            }
        } else {
            memory[dp] = Math.max(MAX_BYTE.toInt(), (memory[dp] + 1)).toByte()
        }
    }

    private fun dec() {
        if (options.isWrapping) {
            memory[dp] = if (memory[dp] == ZERO_BYTE) {
                MAX_BYTE
            } else {
                (memory[dp] - 1).toByte()
            }
        } else {
            memory[dp] = if (memory[dp] == ZERO_BYTE) {
                ZERO_BYTE
            } else {
                (memory[dp] - 1).toByte()
            }
        }
    }
}

fun bfInterpreter(infile: File? = null, outfile: File? = null, options: InterpreterOptions = DEFAULT_OPTIONS): BFInterpreter {
    val inputStream = if (infile == null) System.`in` else FileInputStream(infile)
    val outputStream = if (outfile == null) System.out else FileOutputStream(outfile)
    return BFInterpreter(inputStream, outputStream, options)
}

fun bfInterpreter(input: String = "", outputStream: OutputStream, options: InterpreterOptions = DEFAULT_OPTIONS): BFInterpreter {
    val inputStream = if (input.isEmpty()) System.`in` else ByteArrayInputStream(input.toByteArray(Charsets.UTF_8))
    return BFInterpreter(inputStream, outputStream, options)
}

fun main(args: Array<String>) {
    val inputFile = if (args.size > 0) File(args[0]) else null
    val outputFile = if (args.size > 1) File(args[1]) else null

    val interpreter = bfInterpreter(inputFile, outputFile)
    interpreter.eval()
}