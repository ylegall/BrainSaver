package org.ygl

import java.io.File
import java.io.PrintWriter
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.*


class CodeGen(outputFile: File, var verbose:Boolean = true) : AutoCloseable {

    private val TABSIZE = 8
    private val MARGIN  = 80

    private var col = 0
    private var nestLevel = 0

    private var dataPointer = 0
    private var memorySize = 0

    private val scopes = ArrayList<Scope>()
    private val functions = HashMap<String, Function>()
    private val output: PrintWriter = PrintWriter(
            Files.newBufferedWriter(outputFile.toPath(), Charset.forName("UTF-8"))
    )
    private val reservedChars = Regex("""[\[\]<>+-,.]""")

    override fun close() {
        output.flush()
        output.close()
    }

    fun registerFunction(function: BrainLoveParser.FunctionContext) {
        if (functions.containsKey(function.name.text)) {
            throw Exception("duplicate function: ${function.name.text}")
        } else {
            val newFunction = Function(function.name.text, function)
            functions.put(function.name.text, newFunction)
        }
    }

    fun enterScope() {
        val newScope = Scope(memorySize)
        scopes.add(newScope)
    }

    fun exitScope(): Scope {
        // TODO
        return scopes.removeAt(scopes.size-1)
    }

    fun currentScope(): Scope {
        return scopes[scopes.size - 1]
    }

    fun assign(lhs: Symbol, rhs: Symbol) {
        if (rhs.isConstant() && lhs.type == rhs.type) {
            set(lhs, rhs.value)
        }

        val tmp = currentScope().getTempSymbol()

        commentLine("assign $rhs to $lhs")

        setZero(lhs)
        setZero(tmp)
        moveTo(rhs.address)

        enterLoop()
        emit("-")
        moveTo(lhs)
        emit("+")
        moveTo(tmp)
        emit("+")
        moveTo(rhs)
        exitLoop()

        moveTo(tmp)
        enterLoop()
        emit("-")
        moveTo(rhs)
        emit("+")
        moveTo(tmp)
        exitLoop()
    }

    fun add(s1: Symbol, s2: Symbol): Symbol {
        val tmp = currentScope().getTempSymbol()
        assign(tmp, s2)
        moveTo(tmp)
        enterLoop()
        emit("-")
        moveTo(s1)
        emit("+")
        moveTo(tmp)
        exitLoop()
        return s1
    }

    fun subtract(s1: Symbol, s2: Symbol): Symbol {
        val tmp = currentScope().getTempSymbol()
        assign(tmp, s2)
        moveTo(tmp)
        enterLoop()
        emit("-")
        moveTo(s1)
        emit("-")
        moveTo(tmp)
        exitLoop()
        return s1
    }

    fun multiply(s1: Symbol, s2: Symbol): Symbol {
        val t1 = currentScope().getTempSymbol()
        val t2 = currentScope().getTempSymbol()
        assign(t1, s1)
        assign(t2, s2)
        setZero(s1)
        moveTo(t2)
        enterLoop()
        emit("-")
        add(s1, t1)
        moveTo(t2)
        exitLoop()
        return s1
    }

    fun divide(s1: Symbol, s2: Symbol): Symbol {
        val cpy = currentScope().getTempSymbol()
        val div = currentScope().getTempSymbol()
        assign(cpy, s1)
        moveTo(cpy)
        enterLoop()
        subtract(cpy, s2)
        moveTo(div)
        emit("+")
        moveTo(cpy)
        exitLoop()

        val remainder = multiply(div, s2)
        subtract(remainder, s1)

        val flag = currentScope().getTempSymbol()
        moveTo(remainder)
        enterLoop()
        setZero(remainder)
        moveTo(flag)
        emit("+")
        moveTo(remainder)
        exitLoop()

        subtract(div, flag)
        assign(s1, div)
        return s1
    }

    fun mod(s1: Symbol, s2: Symbol): Symbol {
        val tmp = currentScope().getTempSymbol()
        assign(tmp, s1)
        divide(tmp, s2)
        subtract(s1, multiply(tmp, s2))
        return s1
    }

    fun print(symbol: Symbol): Symbol {
        newline()
        comment("print $symbol")
        moveTo(symbol)
        return if (symbol.type == Type.STRING) {
            printString(symbol)
        } else {
            printInt(symbol)
        }
    }

    // TODO
    fun printInt(symbol: Symbol): Symbol {

        val cpy = currentScope().getTempSymbol()
        assign(cpy, symbol)

        val hundred = currentScope().getTempSymbol()
        val ten = currentScope().getTempSymbol()
        val aaa = currentScope().getTempSymbol()
        val c1 = currentScope().getTempSymbol()
        val c2 = currentScope().getTempSymbol()

        set(hundred, 100)
        set(ten, 10)
        set(aaa, 48)

        assign(c1, divide(cpy, hundred))
        mod(cpy, hundred)
        assign(c2, divide(cpy, ten))
        mod(cpy, ten)

        add(c1, aaa)
        printChar(c1)
        add(c2, aaa)
        printChar(c2)
        add(cpy, aaa)
        printChar(cpy)
        return symbol
    }

    fun printString(symbol: Symbol): Symbol {
        for (i in 0 until symbol.size) {
            printChar(Offset(symbol, i))
        }
        return symbol
    }

    fun printChar(symbol: Addressable): Addressable {
        moveTo(symbol.address())
        emit(".")
        return symbol
    }

    fun loadString(symbol: Symbol, chars: String): Symbol {
        assert(symbol.size == chars.length, { "loadString() string size larger than symbol size" })
        commentLine("load string ${symbol.name} = $chars")
        moveTo(symbol)
        for (i in 0 until symbol.size) {
            val intValue = chars[i].toInt()
            set(Offset(symbol, i), intValue)
        }
        symbol.value = chars
        return symbol
    }

    fun setZero(symbol: Addressable) {
        moveTo(symbol.address())
        if (symbol.size() == 1) {
            emit("[-]")
        } else {
            for (i in 1..symbol.size()) {
                emit("[-]")
                emit(">")
                dataPointer += 1
            }
            moveTo(symbol.address())
        }
        comment("zero $symbol")
    }

    fun set(symbol: Addressable, value: Int): Addressable {
        setZero(symbol)
        emit("+".repeat(value))
        comment("set $symbol = $value")
        symbol.value = value
        return symbol
    }

    fun set(symbol: Addressable, value: String): Addressable {
        return loadString(symbol as Symbol, value)
    }

    fun moveTo(symbol: Addressable) {
        return moveTo(symbol.address())
    }

    fun moveTo(address: Int) {
        val diff = Math.abs(address - dataPointer)
        if (diff != 0) {
            val dir = if (address > dataPointer) ">" else "<"
            emit(dir.repeat(diff))
            //comment("move to <$address>")
            dataPointer = address
        }
    }

    fun enterLoop() {
        newline()
        emit("[")
        if (nestLevel < 10) nestLevel += 1
        newline()
    }

    fun exitLoop() {
        nestLevel -= 1
        assert(nestLevel >= 0)
        newline()
        emit("]")
        newline()
    }

    private fun commentLine(str: String) {
        if (col != 0) newline()
        newline()
        comment(str)
    }

    private fun comment(str: String) {
        if (verbose) {

            if (reservedChars.containsMatchIn(str)) {
                throw Exception("invalid comment string: '$str'")
            }

            var tab = 0
            if (col != 0) {
                tab = TABSIZE - (col % TABSIZE)
                if (tab == 1) TABSIZE + 1
                else if (tab == 0) TABSIZE
            }

            var str = " ".repeat(tab) + ";; $str"
            if (col + str.length > MARGIN) {
                newline()
                str = str.trim()
            }
            emit(str)
            newline()
        }
    }

    private fun newline() {
        emit(System.lineSeparator())
    }

    private fun getIndent() = "  ".repeat(nestLevel)

    private fun emit(code: String) {
        if (verbose) {
            if (code == System.lineSeparator()) {
                val indent = getIndent()
                col = indent.length
                write(code)
                write(indent)
            } else {
                emitPretty(code)
            }
        } else {
            write(code)
        }
    }

    private fun emitPretty(code: String) {
        var remaining = code
        while (remaining.length + col >= MARGIN) {
            val gap = MARGIN - col
            val prefix = remaining.substring(0, gap)
            write(prefix)
            remaining = remaining.substring(gap)

            write(System.lineSeparator())
            val indent = getIndent()
            write(indent)
            col = indent.length
        }
        write(remaining)
        col += remaining.length
    }

    private fun write(code: String) {
        output.print(code)
        print(code)
    }
}