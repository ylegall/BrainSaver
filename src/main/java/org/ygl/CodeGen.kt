package org.ygl

import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.util.*


typealias ConstFunction = (Int, Int) -> Int
typealias SymbolFunction = (Symbol, Symbol) -> Symbol
typealias BinaryOp = (Symbol, Symbol) -> Symbol

const val MARGIN  = 40
const val COMMENT_MARGIN  = 44

// https://esolangs.org/wiki/Brainfuck_algorithms
// https://www.codeproject.com/Articles/558979/BrainFix-the-language-that-translates-to-fluent-Br
class CodeGen(outputFile: File, var verbose:Boolean = true) : AutoCloseable {

    private var col = 0
    private var nestLevel = 0

    private var dataPointer = 0

    val functions = HashMap<String, Function>()
    private val scopes = ArrayList<Scope>()
    private val output: PrintWriter = PrintWriter(
            Files.newBufferedWriter(outputFile.toPath(), Charsets.UTF_8)
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

    // TODO: if rhs is constant and lhs is undefined, call loadConstant()
    fun assign(lhs: String, rhs: Symbol): Symbol {
        val lhsSymbol = currentScope().getOrCreateSymbol(lhs, rhs.size, rhs.type)
        return assign(lhsSymbol, rhs)
    }

    fun isConstant(symbol: Symbol): Boolean {
        return symbol.isConstant() && !currentScope().hasConditions()
    }

    fun assign(lhs: Symbol, rhs: Symbol): Symbol {
        if (isConstant(rhs)) {
            // TODO: if sizes don't match, reallocate
            when (rhs.type) {
                Type.STRING -> loadString(lhs, rhs.value as String)
                Type.INT    -> loadConstant(lhs, rhs.value as Int)
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

        startLoop()
            emit("-")
            inc(lhs)
            inc(tmp)
            moveTo(rhs)
        endLoop()

        moveTo(tmp)
        startLoop()
            emit("-")
            inc(rhs)
            moveTo(tmp)
        endLoop()
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
            return incrementBy(s1, s2.value as Int)
        }
        val tmp = currentScope().getTempSymbol()
        assign(tmp, s2)
        moveTo(tmp)
        startLoop()
            emit("-")
            inc(s1)
            moveTo(tmp)
        endLoop()
        currentScope().delete(tmp)
        return s1
    }

    fun subtractFrom(s1: Symbol, s2: Symbol): Symbol {
        commentLine("subtract $s2 from $s1")
        if (s1.isConstant() && s2.isConstant()) {
            return incrementBy(s1, -(s2.value as Int))
        }
        val tmp = currentScope().getTempSymbol()
        assign(tmp, s2)
        moveTo(tmp)
        startLoop()
            emit("-")
            dec(s1)
            moveTo(tmp)
        endLoop()
        currentScope().delete(tmp)
        return s1
    }

    fun multiplyBy(s1: Symbol, s2: Symbol): Symbol {
        if (isConstant(s1) && isConstant(s2)) {
            // TODO
            return loadInt(s1, s1.value as Int * s2.value as Int)
        }
        val t1 = currentScope().getTempSymbol()
        val t2 = currentScope().getTempSymbol()
        assign(t1, s1)
        assign(t2, s2)
        setZero(s1)
        moveTo(t2)
        startLoop()
            emit("-")
            addTo(s1, t1)
            moveTo(t2)
        endLoop()
        currentScope().delete(t1)
        currentScope().delete(t2)
        return s1
    }

    fun divideBy(s1: Symbol, s2: Symbol): Symbol {
        if (s1.isConstant() && s2.isConstant()) {
            // TODO
            return loadInt(s1, s1.value as Int / s2.value as Int)
        }
        val cpy = currentScope().getTempSymbol()
        val div = currentScope().getTempSymbol()
        assign(cpy, s1)
        moveTo(cpy)
        startLoop()
            subtractFrom(cpy, s2)
            inc(div)
            moveTo(cpy)
        endLoop()
        currentScope().delete(cpy)

        val remainder = multiply(div, s2)
        subtract(remainder, s1)

        val flag = currentScope().getTempSymbol()
        moveTo(remainder)
        startLoop()
            setZero(remainder)
            inc(flag)
            moveTo(remainder)
        endLoop()

        subtractFrom(div, flag)
        currentScope().delete(flag)
        assign(s1, div)
        currentScope().delete(div)

        return s1
    }

    // TODO: optimize
    fun modBy(s1: Symbol, s2: Symbol): Symbol {

        if (s1.isConstant() && s2.isConstant()) {
            // TODO
            return loadInt(s1, s1.value as Int % s2.value as Int)
        }
        val tmp = currentScope().getTempSymbol()
        assign(tmp, s1)
        divideBy(tmp, s2)
        subtractFrom(s1, multiply(tmp, s2))
        currentScope().delete(tmp)
        return s1
    }

    fun equal(lhs: Symbol, rhs: Symbol): Symbol {
        commentLine("$lhs == $rhs")

        val x = currentScope().createSymbol("diff")
        val z = currentScope().getTempSymbol()

        loadInt(z, 1)

        assign(x, lhs)
        subtractFrom(x, rhs)
        moveTo(x)
        startLoop()
            setZero(z)
            setZero(x)
        endLoop()

        assign(x, rhs)
        subtractFrom(x, lhs)
        moveTo(x)
        startLoop()
            setZero(z)
            setZero(x)
        endLoop()

        currentScope().delete(x)

        return z
    }

    fun notEqual(lhs: Symbol, rhs: Symbol): Symbol {
        commentLine("$lhs not equal $rhs")
        val x = equal(lhs, rhs)
        val result = currentScope().getTempSymbol()

        loadInt(result, 1)

        moveTo(x)
        startLoop()
            setZero(result)
            setZero(x)
        endLoop()

        currentScope().delete(x)
        return result
    }

    // TODO
    fun lessThan(lhs: Symbol, rhs: Symbol): Symbol {
        commentLine("${lhs.name} less than ${rhs.name}")

        val x = currentScope().getTempSymbol()
        val y = currentScope().getTempSymbol()

        val t0 = currentScope().getTempSymbol()

        // TODO: ensure these are consecutive
        val t1 = currentScope().getTempSymbol()
        val t2 = currentScope().getTempSymbol()
        val t3 = currentScope().getTempSymbol()

        assign(x, lhs)
        assign(y, rhs)

        setZero(t0)
        setZero(t1)
        loadInt(t2, 1)
        setZero(t3)

        // copy t to t0
        moveTo(y)
        startLoop()
            inc(t0)
            inc(t1)
            dec(y)
        endLoop()

        moveTo(t1)
        startLoop()
            inc(y)
            dec(t1)
        endLoop()

        // copy x to t1
        moveTo(x)
        startLoop()
            inc(t1)
            dec(x)
        endLoop()

        // temp1[>-]> [< x+ temp0[-] temp1>->]<+<
        moveTo(t1)
        startLoop()
            emit(">-")
        endLoop()

        startLoop()
            emit("<")
            inc(x)
            setZero(t0)
            moveTo(t1)
            emit(">->")
        endLoop()
        emit("<+<")

        // temp0[temp1- [>-]> [< x+ temp0[-]+ temp1>->]<+< temp0-]
        moveTo(t0)
        startLoop()
            dec(t1)
            emit("[>-]>")
            startLoop()
                emit("<")
                inc(x)
                loadInt(t0, 1)
                moveTo(t1)
                emit(">->")
            endLoop()
            emit("<+<")
            dec(t0)
        endLoop()

        currentScope().delete(t0)
        currentScope().delete(t1)
        currentScope().delete(t2)
        currentScope().delete(t3)
        currentScope().delete(y)
        return x
    }

    fun lessThanEqual(lhs: Symbol, rhs: Symbol): Symbol {
        commentLine("${lhs.name} less than or equal ${rhs.name}")
        return greaterThan(rhs, lhs)
    }

    fun greaterThanEqual(lhs: Symbol, rhs: Symbol): Symbol {
        commentLine("${lhs.name} less than or equal ${rhs.name}")
        return lessThan(rhs, lhs)
    }

    fun greaterThan(lhs: Symbol, rhs: Symbol): Symbol {
        // z = x > y

        commentLine("${lhs.name} greater than ${rhs.name}")
        val z = currentScope().getTempSymbol()
        val x = currentScope().getTempSymbol()
        val y = currentScope().getTempSymbol()
        val t0 = currentScope().getTempSymbol()
        val t1 = currentScope().getTempSymbol()

        assign(x, lhs)
        assign(y, rhs)

        setZero(t0)
        setZero(t1)
        setZero(z)

        moveTo(x)
        startLoop()
            inc(t0)

            moveTo(y)
            startLoop()
                emit("-")
                setZero(t0)
                inc(t1)
                moveTo(y)
            endLoop()

            moveTo(t0)
            startLoop()
                emit("-")
                inc(z)
                moveTo(t0)
            endLoop()

            moveTo(t1)
            startLoop()
                emit("-")
                inc(y)
                moveTo(t1)
            endLoop()

            dec(y)
            dec(x)
        endLoop()

        currentScope().delete(x)
        currentScope().delete(y)
        currentScope().delete(t0)
        currentScope().delete(t1)

        return z
    }

    fun and(lhs: Symbol, rhs: Symbol): Symbol {
        commentLine("${lhs.name} && ${rhs.name}")
        val x = currentScope().getTempSymbol()
        val y = currentScope().getTempSymbol()
        val ret = currentScope().getTempSymbol()

        assign(x, lhs)
        assign(y, rhs)

        moveTo(x)
        startLoop()
            moveTo(y)
            startLoop()
                loadInt(ret, 1)
                setZero(y)
            endLoop()
            setZero(x)
        endLoop()

        currentScope().delete(x)
        currentScope().delete(y)

        return ret
    }

    fun or(lhs: Symbol, rhs: Symbol): Symbol {
        commentLine("${lhs.name} || ${rhs.name}")
        val x = currentScope().getTempSymbol()
        val ret = currentScope().getTempSymbol()

        assign(x, lhs)
        moveTo(x)
        startLoop()
            loadInt(ret, 1)
            setZero(x)
        endLoop()

        assign(x, rhs)
        moveTo(x)
        startLoop()
            loadInt(ret, 1)
            setZero(x)
        endLoop()

        currentScope().delete(x)

        return ret
    }

    fun not(rhs: Symbol): Symbol {
        commentLine("not $rhs")
        val tmp = currentScope().getTempSymbol()
        val ret = currentScope().getTempSymbol()
        assign(tmp, rhs)
        loadInt(ret, 1)

        moveTo(tmp)
        startLoop()
            setZero(ret)
            setZero(tmp)
        endLoop()

        currentScope().delete(tmp)

        return ret
    }

    fun startIf(condition: Symbol) {
        commentLine("if $condition")
        val elseFlag = currentScope().pushConditionFlag()
        loadInt(elseFlag, 1)    // loadInt else to 1

        // TODO
        // if condition is non-zero
        // loadInt the else flag to zero and execute the if clause
        val tmp = currentScope().getTempSymbol()
        assign(tmp, condition)
        moveTo(tmp)
        startLoop()
            setZero(tmp)
            setZero(elseFlag)
            moveTo(tmp)
        endLoop()

        currentScope().delete(tmp)

        moveTo(condition)
        startLoop()
        setZero(condition)
    }

    fun endIf(condition: Symbol) {
        moveTo(condition)
        endLoop()
        commentLine("end if $condition")
    }

    fun startElse(condition: Symbol) {
        val elseFlag = currentScope().getConditionFlag()
        commentLine("else $condition")
        moveTo(elseFlag)
        startLoop()
        setZero(elseFlag)
    }

    fun endElse(condition: Symbol) {
        val elseFlag = currentScope().getConditionFlag()
        moveTo(elseFlag)
        endLoop()
        commentLine("end else $condition")
    }

    fun startWhile(condition: Symbol) {
        commentLine("start while $condition")
        val flag = currentScope().pushConditionFlag()
        assign(flag, condition)
        moveTo(flag)
        startLoop()
    }

    fun endWhile(condition: Symbol) {
        val flag = currentScope().getConditionFlag()
        assign(flag, condition)
        moveTo(flag)
        endLoop()
        commentLine("end while $condition")
        currentScope().popConditionFlag()
    }

    fun readChar(symbol: Symbol): Symbol {
        moveTo(symbol)
        emit(",", "read char $symbol")
        //comment("read char $symbol")
        return symbol
    }

    fun readInt(symbol: Symbol): Symbol {
        commentLine("read int $symbol")
        moveTo(symbol)
        emit(",")
        emit("-".repeat(48), "convert char to int")
        return symbol
    }

    fun print(symbol: Symbol): Symbol {
        newline()
        commentLine("print $symbol")
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

        moveTo(cpy)
        startLoop()
        addTo(cpy, asciiOffset)
        printChar(cpy)
        endLoop()

        moveTo(d2)
        startLoop()
        addTo(d2, asciiOffset)
        printChar(d2)
        endLoop()

        moveTo(d3)
        addTo(d3, asciiOffset)
        printChar(d3)

        return symbol

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

    // TODO: implement string copy function
    fun copyString(lhs: Symbol, rhs: Symbol): Symbol {
        throw Exception("not implemented")
    }

    fun setZero(symbol: Symbol, offset: Int = 0): Symbol {
        moveTo(symbol, offset)
        if (symbol.size == 1) {
            emit("[-]", "zero $symbol")
        } else {
            for (i in 1..symbol.size) {
                emit("[-]")
                emit(">")
                dataPointer += 1
            }
            moveTo(symbol, offset)
        }
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

    fun loadInt(symbol: Symbol, value: Int, offset: Int = 0): Symbol {
        if (value == 0) return setZero(symbol, offset)
        setZero(symbol, offset)
        emit("+".repeat(value), "load $symbol = $value")
        return symbol
    }

    fun incrementBy(symbol: Symbol, value: Int, offset: Int = 0): Symbol {
        if (value == 0) return symbol
        val ch = if (value < 0) "-" else "+"
        if (symbol.isConstant()) {
            symbol.value = symbol.value as Int + value
        }
        emit(ch.repeat(Math.abs(value)), "increment $symbol by $value")
        return symbol
    }

    fun loadConstant(symbol: Symbol, value: Int, offset: Int = 0): Symbol {
        symbol.value = value
        return loadInt(symbol, value, offset)
    }

    fun inc(symbol: Symbol, comment: String = "") {
        moveTo(symbol)
        emit("+", comment)
    }

    fun dec(symbol: Symbol, comment: String = "") {
        moveTo(symbol)
        emit("-", comment)
    }

    fun moveTo(symbol: Symbol, offset: Int = 0, comment: String = "") {
        return moveToAddress(symbol.address + offset, comment)
    }

    fun moveToAddress(address: Int, comment: String = "") {
        val diff = Math.abs(address - dataPointer)
        if (diff != 0) {
            val dir = if (address > dataPointer) ">" else "<"
            emit(dir.repeat(diff), comment)
            dataPointer = address
        }
    }

    fun startLoop() {
        newline()
        emit("[")
        nestLevel = Math.min(nestLevel + 1, 10)
        newline()
    }

    fun endLoop() {
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
        emit("[", "return $sym")
        nestLevel = Math.min(nestLevel + 1, 10)
    }

    fun closeFunction(func: String = "") {
        val count = currentScope().returnCount
        nestLevel -= count
        emit("]".repeat(count))
        currentScope().returnCount = 0
        commentLine("end function $func\n")
    }

    fun commentLine(str: String) {
        if (col != 0) newline()
        assert(!str.contains(reservedChars))
        write("# " + str)
        newline()
    }

    private fun newline() {
        write(System.lineSeparator())
        val indent = getIndent()
        col = indent.length
        write(indent)
    }

    private fun getIndent() = "  ".repeat(nestLevel)

    private fun emit(code: String, comment: String = "") {
        if (verbose) {

            var text = code
            while (!text.isEmpty()) {
                if (col == MARGIN) newline()
                if (text.length + col > MARGIN) {
                    val prefix = MARGIN - col
                    write(text.substring(0, prefix))
                    text = text.substring(prefix)
                    col = MARGIN
                } else {
                    write(text)
                    col += text.length
                    text = ""
                }

                if (!comment.isEmpty()) {
                    assert(!comment.contains(reservedChars))
                    val commentPadding = COMMENT_MARGIN - col
                    write(" ".repeat(commentPadding) + "# " + comment)
                    newline()
                }
            }

            if (!text.isEmpty()) emit(text)
        } else {
            write(code)
        }
    }

    private fun write(code: String) {
        output.print(code)
        print(code)
    }
}
