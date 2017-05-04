package org.ygl

import java.io.File
import java.io.PrintWriter
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.*


typealias ConstFunction = (Int, Int) -> Int
typealias SymbolFunction = (Symbol, Symbol) -> Symbol
class CodeGen(outputFile: File, var verbose:Boolean = true) : AutoCloseable {

    private val TABSIZE = 8
    private val MARGIN  = 80

    private var col = 0
    private var nestLevel = 0

    private var dataPointer = 0

    val functions = HashMap<String, Function>()
    private val scopes = ArrayList<Scope>()
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
        val memorySize = if (scopes.isEmpty()) {
            0
        } else {
            val lastScope = scopes[scopes.size - 1]
            lastScope.startAddress + lastScope.scopeSize
        }
        val newScope = Scope(memorySize)
        scopes.add(newScope)
        setZero(newScope.getZeroSymbol())
    }

    fun exitScope(): Scope {
        return scopes.removeAt(scopes.size-1)
    }

    fun currentScope(): Scope {
        return scopes[scopes.size - 1]
    }

    fun assign(lhs: String, rhs: Symbol): Symbol {
        val lhsSymbol = currentScope().getOrCreateSymbol(lhs, rhs.size, rhs.type)
        return assign(lhsSymbol, rhs)
    }

    fun assign(lhs: Symbol, rhs: Symbol): Symbol {
        if (rhs.isConstant()) {
            // TODO: if sizes don't match, reallocate
            when (rhs.type) {
                Type.STRING -> loadString(lhs, rhs.value as String)
                Type.INT    -> loadInt(lhs, rhs.value as Int)
            }
            lhs.value = rhs.value
        } else {
            lhs.value = null
            when (rhs.type) {
                Type.STRING -> copyString(lhs, rhs)
                Type.INT    -> assignInt(lhs, rhs)
            }
        }
        return lhs
    }

    /**
     * Handles op-assign operations like x += y
     * throws exception if lhs identifier is undefined
     */
    fun opAssign(lhs: String, rhs: Symbol, func: SymbolFunction): Symbol {
        val lhsSymbol = currentScope().getSymbol(lhs) ?:
                throw Exception("undefined identifier $lhs")
        return func(lhsSymbol, rhs)
    }

    fun assignInt(lhs: Symbol, rhs: Symbol): Symbol {
        commentLine("assign $rhs to $lhs")

        val tmp = currentScope().getTempSymbol()
        setZero(lhs)
        setZero(tmp)
        moveTo(rhs)

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
        currentScope().delete(tmp)
        return lhs
    }

    fun divide(s1: Symbol, s2: Symbol): Symbol {
        return binaryOp(s1, s2, { a, b -> a / b }, this::divideBy)
    }

    fun multiply(s1: Symbol, s2: Symbol): Symbol {
        return binaryOp(s1, s2, { a, b -> a * b }, this::multiplyBy)
    }

    fun mod(s1: Symbol, s2: Symbol): Symbol {
        return binaryOp(s1, s2, { a, b -> a % b }, this::modBy)
    }

    fun subtract(s1: Symbol, s2: Symbol): Symbol {
        return binaryOp(s1, s2, { a, b -> a - b }, this::subtractFrom)
    }

    fun add(s1: Symbol, s2: Symbol): Symbol {
        return binaryOp(s1, s2, { a, b -> a + b }, this::addTo)
    }

    fun binaryOp(s1: Symbol, s2: Symbol, constFunc: ConstFunction, fallBack: BinaryOp): Symbol {
        val result = currentScope().getTempSymbol()
        if (s1.isConstant() && s2.isConstant()) {
            result.value = constFunc.invoke(s1.value as Int, s2.value as Int)
        } else {
            assign(result, s1)
            fallBack(result, s2)
        }
        return result
    }

    fun addTo(s1: Symbol, s2: Symbol): Symbol {
        if (s1.isConstant() && s2.isConstant()) {
            return loadInt(s1, s1.value as Int + s2.value as Int)
        }
        val tmp = currentScope().getTempSymbol()
        assign(tmp, s2)
        moveTo(tmp)
        enterLoop()
        emit("-")
        moveTo(s1)
        emit("+")
        moveTo(tmp)
        exitLoop()
        currentScope().delete(tmp)
        return s1
    }

    fun subtractFrom(s1: Symbol, s2: Symbol): Symbol {
        if (s1.isConstant() && s2.isConstant()) {
            return loadInt(s1, s1.value as Int - s2.value as Int)
        }
        val tmp = currentScope().getTempSymbol()
        assign(tmp, s2)
        moveTo(tmp)
        enterLoop()
        emit("-")
        moveTo(s1)
        emit("-")
        moveTo(tmp)
        exitLoop()
        currentScope().delete(tmp)
        return s1
    }

    fun multiplyBy(s1: Symbol, s2: Symbol): Symbol {
        if (s1.isConstant() && s2.isConstant()) {
            return loadInt(s1, s1.value as Int * s2.value as Int)
        }
        val t1 = currentScope().getTempSymbol()
        val t2 = currentScope().getTempSymbol()
        assign(t1, s1)
        assign(t2, s2)
        setZero(s1)
        moveTo(t2)
        enterLoop()
        emit("-")
        addTo(s1, t1)
        moveTo(t2)
        exitLoop()
        currentScope().delete(t1)
        currentScope().delete(t2)
        return s1
    }

    fun divideBy(s1: Symbol, s2: Symbol): Symbol {
        if (s1.isConstant() && s2.isConstant()) {
            return loadInt(s1, s1.value as Int / s2.value as Int)
        }
        val cpy = currentScope().getTempSymbol()
        val div = currentScope().getTempSymbol()
        assign(cpy, s1)
        moveTo(cpy)
        enterLoop()
        subtractFrom(cpy, s2)
        moveTo(div)
        emit("+")
        moveTo(cpy)
        exitLoop()
        currentScope().delete(cpy)

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

        subtractFrom(div, flag)
        currentScope().delete(flag)
        assign(s1, div)
        currentScope().delete(div)

        return s1
    }

    // TODO: optimize
    fun modBy(s1: Symbol, s2: Symbol): Symbol {
//        if (s1.isConstant() && s2.isConstant()) {
//            return loadInt(s1, s1.value as Int % s2.value as Int)
//        }
//        val cpy = currentScope().getTempSymbol()
//        val div = currentScope().getTempSymbol()
//        assign(cpy, s1)
//        moveTo(cpy)
//        enterLoop()
//        subtractFrom(cpy, s2)
//        moveTo(div)
//        emit("+")
//        moveTo(cpy)
//        exitLoop()
//        currentScope().delete(cpy)
//
//        val remainder = multiply(div, s2)

        if (s1.isConstant() && s2.isConstant()) {
            return loadInt(s1, s1.value as Int % s2.value as Int)
        }
        val tmp = currentScope().getTempSymbol()
        assign(tmp, s1)
        divideBy(tmp, s2)
        subtractFrom(s1, multiply(tmp, s2))
        currentScope().delete(tmp)
        return s1
    }

    fun readInt(symbol: Symbol): Symbol {
        moveTo(symbol)
        emit(",")
        comment("read $symbol")
        return symbol
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

        val ten = currentScope().getTempSymbol()
        val asciiOffset = currentScope().getTempSymbol()
        val d2 = currentScope().getTempSymbol()
        val d3 = currentScope().getTempSymbol()

        loadInt(ten, 10)
        loadInt(asciiOffset, 48)

        assign(d3, mod(cpy, ten))
        divideBy(cpy, ten)
        assign(d2, mod(cpy, ten))
        divideBy(cpy, ten)

        addTo(cpy, asciiOffset)
        printChar(cpy)
        addTo(d2, asciiOffset)
        printChar(d2)
        addTo(d3, asciiOffset)
        printChar(d3)

        return symbol
        // ------------------------------------
//        val cpy = currentScope().getTempSymbol()
//        assign(cpy, symbol)
//
//        val hundred = currentScope().getTempSymbol()
//        val ten = currentScope().getTempSymbol()
//        val aaa = currentScope().getTempSymbol()
//        val c1 = currentScope().getTempSymbol()
//        val c2 = currentScope().getTempSymbol()
//
//        loadInt(hundred, 100)
//        loadInt(ten, 10)
//        loadInt(aaa, 48)
//
//        assign(c1, divide(cpy, hundred))
//        modBy(cpy, hundred)
//        currentScope().delete(hundred)
//        assign(c2, divide(cpy, ten))
//        modBy(cpy, ten)
//        currentScope().delete(ten)
//
//        addTo(c1, aaa)
//        printChar(c1)
//        addTo(c2, aaa)
//        printChar(c2)
//        addTo(cpy, aaa)
//        printChar(cpy)
//
//        currentScope().delete(cpy)
//        currentScope().delete(aaa)
//        currentScope().delete(c1)
//        currentScope().delete(c2)
//
//        return symbol
    }

    fun printString(symbol: Symbol): Symbol {
        for (i in 0 until symbol.size) {
            printChar(symbol, i)
        }
        return symbol
    }

    fun printImmediate(chars: String) {
        val tmp = currentScope().getTempSymbol()
        moveTo(tmp)
        for (i in 1 .. chars.length) {
            setZero(tmp)
            val intValue = chars[i].toInt()
            loadInt(tmp, intValue)
            emit(".")
        }
        currentScope().delete(tmp)
    }

    fun printChar(symbol: Symbol, offset: Int = 0): Symbol {
        moveTo(symbol, offset)
        emit(".")
        return symbol
    }

    fun loadString(symbol: Symbol, chars: String): Symbol {
        assert(symbol.size == chars.length, { "loadString() string size larger than symbol size" })
        commentLine("load ${symbol.name} = \"$chars\"")
        moveTo(symbol)
        for (i in 0 until symbol.size) {
            val intValue = chars[i].toInt()
            loadInt(symbol, intValue, offset=i)
        }
        symbol.value = chars
        return symbol
    }

    // TODO: implement string copy function
    fun copyString(lhs: Symbol, rhs: Symbol): Symbol {
        throw Exception("not implemented")
    }

    fun setZero(symbol: Symbol, offset: Int = 0): Symbol {
        moveTo(symbol, offset)
        if (symbol.size == 1) {
            emit("[-]")
        } else {
            for (i in 1..symbol.size) {
                emit("[-]")
                emit(">")
                dataPointer += 1
            }
            moveTo(symbol, offset)
        }
        comment("zero $symbol")
        return symbol
    }

    fun loadInt(symbol: Symbol, value: Int, offset: Int = 0): Symbol {
        setZero(symbol, offset)
        emit("+".repeat(value))
        comment("load $symbol = $value")
        symbol.value = value
        return symbol
    }

    fun moveTo(symbol: Symbol, offset: Int = 0) {
        return moveToAddress(symbol.address + offset)
    }

    fun moveToAddress(address: Int) {
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
        nestLevel = Math.min(nestLevel + 1, 10)
        newline()
    }

    fun exitLoop() {
        nestLevel -= 1
        assert(nestLevel >= 0, {-> "negative nest level"})
        newline()
        emit("]")
        newline()
    }

    fun emitReturn(sym: Symbol?) {
        currentScope().returnCount += 1
        if (sym != null) {
            val returnSymbol = currentScope().getReturnSymbol()
            assign(returnSymbol, sym)
        }
        moveTo(currentScope().getZeroSymbol())
        emit("[")
        comment("return $sym")
        nestLevel = Math.min(nestLevel + 1, 10)
    }

    fun closeFunction(func: String = "") {
        val count = currentScope().returnCount
        nestLevel -= count
        emit("]".repeat(count))
        currentScope().returnCount = 0
        comment("end function $func\n")
    }

    fun commentLine(str: String) {
        if (col != 0) newline()
        newline()
        comment(str)
    }

    fun comment(str: String) {
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
typealias BinaryOp = (Symbol, Symbol) -> Symbol

