package org.ygl

import java.io.OutputStream
import java.util.*

typealias BinaryOp = (Symbol, Symbol) -> Symbol

// https://esolangs.org/wiki/Brainfuck_algorithms
// https://www.codeproject.com/Articles/558979/BrainFix-the-language-that-translates-to-fluent-Br
class CodeGen(
        outputStream: OutputStream = System.`out`,
        val options: CompilerOptions = DEFAULT_COMPILE_OPTIONS,
        globals: Map<String, Symbol> = mapOf()
): AutoCloseable {

    private var col = 0
    private var nestLevel = 0
    private var dataPointer = 0
    private val scopes = ArrayList<Scope>()

    private val output: OutputStream = if (options.minify) {
        MinifyingOutputStream(outputStream, options.margin)
    } else {
        outputStream
    }

    init {
        val globalScope = Scope(0, "")
        for ((name, global) in globals) {
            globalScope.createSymbol(name, global)
        }
        scopes.add(globalScope)
    }

    val io = IO(this)
    val math = Maths(this)


    private val reservedChars = Regex("""[\[\]<>+\-,.]""")

    private val MARGIN = options.margin
    private val COMMENT_MARGIN = MARGIN + 4

    override fun close() {
        output.flush()
    }

    fun enterScope(functionName: String) {
        val memorySize = if (scopes.isEmpty()) {
            0
        } else {
            val lastScope = scopes[scopes.size - 1]
            lastScope.startAddress + lastScope.scopeSize
        }
        val newScope = Scope(memorySize, functionName)
        scopes.add(newScope)
    }

    fun exitScope(): Scope {
        return scopes.removeAt(scopes.size-1)
    }

    fun currentScope() = scopes[scopes.size - 1]
    private fun globalScope() = scopes[0]
    private fun getTempSymbol() = currentScope().getTempSymbol()

    fun getSymbol(name: String): Symbol? {
        return globalScope().getSymbol(name) ?: currentScope().getSymbol(name)
    }

    /**
     * moves rhs to lhs. rhs is not preserved
     */
    fun move(lhs: Symbol, rhs: Symbol): Symbol {
        if (rhs.address == lhs.address) return lhs
        commentLine("move $rhs to $lhs")

        setZero(lhs)
        loop(rhs, {
            inc(lhs)
            dec(rhs)
        })

        return lhs
    }

    fun assign(lhs: Symbol, rhs: Symbol): Symbol {
        when (rhs.type) {
            Type.STRING -> copyString(lhs, rhs)
            Type.INT    -> assignInt(lhs, rhs)
        }
        return lhs
    }

    private fun assignInt(lhs: Symbol, rhs: Symbol): Symbol {
        commentLine("assign $rhs to $lhs")

        val tmp = getTempSymbol()
        setZero(lhs)
        setZero(tmp)

        loop(rhs, {
            emit("-")
            inc(lhs)
            inc(tmp)
        })

        loop(tmp, {
            emit("-")
            inc(rhs)
        })

        currentScope().delete(tmp)
        commentLine("end assign $rhs to $lhs")
        return lhs
    }

    fun startIf(condition: Symbol) {
        commentLine("if $condition")
        moveTo(condition)
        startLoop()
        setZero(condition)
    }

    // is passed the tmp variable from the startIf
    fun endIf(condition: Symbol) {
        moveTo(condition)
        endLoop()
        commentLine("end if")
    }

    fun startElse(elseFlag: Symbol) {
        commentLine("else $elseFlag")
        moveTo(elseFlag)
        startLoop()
        setZero(elseFlag)
    }

    fun endElse(elseFlag: Symbol) {
        moveTo(elseFlag)
        endLoop()
        commentLine("end else $elseFlag")
    }

    fun startWhile(condition: Symbol) {
        commentLine("start while $condition")
        moveTo(condition)
        startLoop()
    }

    fun endWhile(condition: Symbol) {
        moveTo(condition, comment="move to $condition")
        endLoop()
        commentLine("end while $condition")
    }

    fun startFor(loopVar: Symbol, start: Symbol, stop: Symbol, condition: Symbol) {
        commentLine("start for $loopVar = $start to $stop")
        assign(loopVar, start)
        assign(condition, math.lessThanEqual(loopVar, stop))
        moveTo(condition, comment="move to $condition")
        startLoop()
    }

    fun endFor(loopVar: Symbol, stop: Symbol, step: Symbol, condition: Symbol) {
        math.addTo(loopVar, step)
        assign(condition, math.lessThanEqual(loopVar, stop))
        moveTo(condition)
        endLoop()
        commentLine("end for $loopVar")
    }

    fun debug(symbol: Symbol, comment: String = "") {
        newline()
        moveTo(symbol)
        emit("@$comment@")
    }

    // TODO: implement string copy function
    fun copyString(lhs: Symbol, rhs: Symbol): Symbol {
        throw Exception("not implemented")
    }

    fun setZero(symbol: Symbol): Symbol {
        moveTo(symbol)
        emit("[-]", "zero $symbol")
        if (symbol.size > 1) {
            for (i in 1 until symbol.size) {
                moveTo(symbol.offset(i))
                emit("[-]")
            }
        }
        return symbol
    }

    fun loadString(symbol: Symbol, chars: String): Symbol {
        assert(symbol.size == chars.length, { "loadString() string size larger than symbol size" })
        commentLine("load ${symbol.name} = \"$chars\"")
        moveTo(symbol)
        for (i in 0 until symbol.size) {
            val intValue = chars[i].toInt()
            loadInt(symbol.offset(i), intValue)
        }
        symbol.value = chars
        return symbol
    }

    fun loadInt(symbol: Symbol, value: Int): Symbol {
        if (value <= 0) return setZero(symbol) // non wrapping
        setZero(symbol)
        emit("+".repeat(value), "load $symbol = $value")
        //newline()
        return symbol
    }

    fun incrementBy(symbol: Symbol, value: Int): Symbol {
        if (value == 0) return symbol
        val ch = if (value < 0) "-" else "+"
        moveTo(symbol)
        emit(ch.repeat(Math.abs(value)))
        return symbol
    }

    fun inc(symbol: Symbol, comment: String = "") {
        moveTo(symbol)
        emit("+", comment)
    }

    fun dec(symbol: Symbol, comment: String = "") {
        moveTo(symbol)
        emit("-", comment)
    }

    fun moveTo(symbol: Symbol, comment: String = "") {
        return moveToAddress(symbol.address, comment)
    }

    private fun moveToAddress(address: Int, comment: String = "") {
        val diff = Math.abs(address - dataPointer)
        if (diff != 0) {
            val dir = if (address > dataPointer) ">" else "<"
            emit(dir.repeat(diff), comment)
            dataPointer = address
        }
    }

    fun startLoop(comment: String = "") {
        newline()
        emit("[", comment)
        nestLevel = Math.min(nestLevel + 1, 10)
        newline()
    }

    fun endLoop(comment: String = "") {
        nestLevel -= 1
        assert(nestLevel >= 0, {"negative nest level"})
        newline()
        emit("]", comment)
        newline()
    }

    inline fun loop(symbol: Symbol, body: () -> Unit, comment: String = "") {
        moveTo(symbol)
        startLoop(comment)
            body()
            moveTo(symbol)
        endLoop()
    }

    inline fun onlyIf(symbol: Symbol, body: () -> Unit, comment: String = "") {
        moveTo(symbol)
        startLoop(comment)
            setZero(symbol)
            body()
            moveTo(symbol)
        endLoop()
    }

    // TODO
    fun readArray(array: Symbol, index: Symbol): Symbol {
        commentLine("read array $array($index)")

        val ret = getTempSymbol()
        val data = array.offset(3)

        assignInt(array.offset(2), index)  // copy idx to readIdx
        assignInt(array.offset(1), index)  // copy idx to writeIdx
        setZero(data)                   // zero dataIdx

        moveTo(array)
        newline()
        emit(">[>>>[-<<<<+>>>>]<<[->+<]<[->+<]>-]", "move read head to $index")
        emit(">>>[-<+<<+>>>]<<<[->>>+<<<]>", "")
        emit("[[-<+>]>[-<+>]<<<<[->>>>+<<<<]>>-]<<", "restore read head")

        assign(ret, data)

        commentLine("end read array $array($index)")
        return ret
    }

    fun writeArray(array: Symbol, index: Symbol, value: Symbol) {
        commentLine("write array $array($index) = $value")

        assignInt(array.offset(2), index) // copy idx to readIdx
        assignInt(array.offset(1), index) // copy idx to writeIdx
        assignInt(array.offset(3), value) // copy value to dataIdx

        moveTo(array)
        newline()
        emit(">[>>>[-<<<<+>>>>]<[->+<]<[->+<]<[->+<]>-]", "move read head to $index")
        emit(">>>[-]<[->+<]<", "move $value to $index")
        emit("[[-<+>]<<<[->>>>+<<<<]>>-]<<", "restore read head")

        commentLine("end write array $array($index) = $value")
    }

    fun commentLine(str: String) {
        if (col != 0) newline()
        assert(!str.contains(reservedChars))
        write("${options.commentChar} $str")
        newline()
    }

    fun newline() {
        write(System.lineSeparator())
        val indent = getIndent()
        col = indent.length
        write(indent)
    }

    private inline fun getIndent() = "  ".repeat(nestLevel)

    fun emit(code: String, comment: String = "") {
        if (!options.minify) {

            var text = code
            var cmt = comment
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

                if (!cmt.isEmpty()) {
                    assert(!cmt.contains(reservedChars))
                    val commentPadding = COMMENT_MARGIN - col
                    write(" ".repeat(commentPadding) + "${options.commentChar} $cmt")
                    cmt = ""
                    newline()
                }
            }

            if (!text.isEmpty()) emit(text)
        } else {
            write(code)
        }
    }

    private fun write(code: String) {
        output.write(code.toByteArray(Charsets.UTF_8))
    }
}
