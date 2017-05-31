package org.ygl

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.io.*
import java.util.*


const val MAX = 255
const val ZERO = 0

/**
 *
 */
class Interpreter(
        inputStream: InputStream,
        val outputStream: OutputStream = System.out,
        val options: InterpreterOptions = DEFAULT_INTERPRETER_OPTIONS
) {
    constructor(
            str: String,
            outputStream: OutputStream = System.out,
            options: InterpreterOptions = DEFAULT_INTERPRETER_OPTIONS
    ) : this(
            ByteArrayInputStream(str.toByteArray(Charsets.UTF_8)),
            outputStream,
            options
    )

    private var pc = 0
    private var dp = 0

    private val memory = IntArray(options.memorySize)
    private var userInput = loadUserInput()
    private val jumpTable = HashMap<Int, Int>()
    private var text = BufferedInputStream(inputStream).use {
        processInput(it)
    }

    private fun loadUserInput(): ArrayDeque<Char> {
        val q = ArrayDeque<Char>(options.predefinedInput.length)
        q.addAll(options.predefinedInput.asIterable())
        return q
    }

    fun reset(str: String) {
        reset(ByteArrayInputStream(str.toByteArray()))
    }

    fun reset(stream: InputStream) {
        jumpTable.clear()
        text = processInput(stream)
        userInput = loadUserInput()
        pc = 0
        dp = 0
        memory.fill(0)
    }

    private fun processInput(stream: InputStream): String {
        val jumps = ArrayDeque<Int>()
        val sb = StringBuilder()

        var ch = stream.read()
        while (ch != -1) {
            val c = ch.toChar()
            when (c) {
                ',' -> {
                    sb.append(c)
                }
                '.' -> {
                    sb.append(c)
                }
                '[' -> {
                    jumps.push(sb.length)
                    sb.append(c)
                }
                ']' -> {
                    if (options.optimize && (sb.last() == '-') && (sb[sb.length - 2] == '[')) {
                        sb.setLength(sb.length - 2)
                        sb.append('z')
                        jumps.pop()
                    } else {
                        val matchIdx = jumps.pop()
                        jumpTable[matchIdx] = sb.length
                        jumpTable[sb.length] = matchIdx
                        sb.append(c)
                    }
                }
                '+' -> {
                    sb.append(c)
                }
                '-' -> {
                    sb.append(c)
                }
                '<' -> {
                    sb.append(c)
                }
                '>' -> {
                    sb.append(c)
                }
            }
            ch = stream.read()
        }
        return sb.append('\n').toString()
    }

    fun eval() {
        val sb = StringBuilder()
        while (pc < text.length) {
            val c = text[pc]
            when (c) {
                'z' -> if (options.optimize) memory[dp] = 0
                '<' -> {
                    if (options.optimize) {
                        var n = pc + 1
                        while (text[n] == '<') {
                            n++
                        }
                        move(-(n - pc))
                        pc = n - 1
                    } else {
                        move(-1)
                    }
                }
                '>' -> {
                    if (options.optimize) {
                        var n = pc + 1
                        while (text[n] == '>') {
                            n++
                        }
                        move(n - pc)
                        pc = n - 1
                    } else {
                        move(1)
                    }
                }
                '.' -> printChar()
                ',' -> readChar()
                '+' -> {
                    if (options.optimize) {
                        var n = pc + 1
                        while (text[n] == '+') {
                            n++
                        }
                        add(n - pc)
                        pc = n - 1
                    } else {
                        add(1)
                    }
                }
                '-' -> {
                    if (options.optimize) {
                        var n = pc + 1
                        while (text[n] == '-') {
                            n++
                        }
                        add(-(n - pc))
                        pc = n - 1
                    } else {
                        add(-1)
                    }
                }
                '[' -> {
                    if (memory[dp] == ZERO) {
                        pc = jumpTable[pc] ?: throw Exception("unmatched loop")
                    }
                }
                ']' -> {
                    if (memory[dp] != ZERO) {
                        pc = jumpTable[pc] ?: throw Exception("unmatched loop")
                    }
                }
                '@' -> {
                    if (options.debug) {
                        sb.setLength(0)
                        pc++
                        while (text[pc] != '@') {
                            sb.append(text[pc])
                        }
                        println("[debug]: $sb")
                    }
                }
            }
            pc++
        }
        outputStream.flush()
    }

    private inline fun move(diff: Int) {
        dp += diff
    }

    private inline fun add(diff: Int) {
        val v = memory[dp] + diff
        if (v < 0) {
            if (options.wrap) {
                memory[dp] = v + MAX
            } else {
                memory[dp] = ZERO
            }
        } else if (v > MAX) {
            if (options.wrap) {
                dp %= MAX
            } else {
                memory[dp] = MAX
            }
        } else {
            memory[dp] = v
        }
    }

    private fun readChar() {
        if (!userInput.isEmpty()) {
            val c = userInput.pop()
            memory[dp] = c?.toInt() ?: throw Exception("null input char")
        } else {
            print("waiting for input: ")
            val c = System.`in`.read()
            memory[dp] = c
            println()
        }
    }

    private fun printChar() {
        this.outputStream.write(memory[dp])
    }

    fun getCellValue(idx: Int): Int {
        return memory[idx]
    }

    fun getCellChar(idx: Int): Char {
        return memory[idx].toChar()
    }
}

fun interpreter(
        input: String = "",
        outputStream: OutputStream = System.`out`,
        options: InterpreterOptions = DEFAULT_INTERPRETER_OPTIONS
): Interpreter {
    val inputStream = if (input.isEmpty()) System.`in` else ByteArrayInputStream(input.toByteArray(Charsets.UTF_8))
    return Interpreter(inputStream, outputStream, options)
}

/**
 *
 */
fun eval(inFile: File? = null,
         options: InterpreterOptions = DEFAULT_INTERPRETER_OPTIONS)
{
    if (inFile != null) {
        FileInputStream(inFile).use { input ->
            val interpreter = Interpreter(input, options = options)
            interpreter.eval()
        }
    } else {
        val interpreter = Interpreter(System.`in`, options = options)
        interpreter.eval()
    }
}

private inline fun printUsageAndHalt(options: Options) {
    HelpFormatter().printHelp("brainsaver", options, true)
    System.exit(1)
}

fun main(args: Array<String>) {

    val opts = configureInterpreterOptions()
    val commandLine = DefaultParser().parse(opts, args)

    val remainingArgs = commandLine.argList
    if (remainingArgs.isEmpty()) {
        printUsageAndHalt(opts)
    }

    val options = InterpreterOptions(
            debug = commandLine.hasOption("debug"),
            wrap = commandLine.hasOption("wrap"),
            memorySize = commandLine.getOptionValue("memory")?.toInt() ?: 30000,
            predefinedInput = commandLine.getOptionValue("input") ?: "",
            optimize = !commandLine.hasOption("no-opt")
    )

    val start = System.currentTimeMillis()
    eval(File(remainingArgs[0]), options)
    val elapsed = System.currentTimeMillis() - start
    println("elapsed: ${formatElapsed(elapsed)}")
}