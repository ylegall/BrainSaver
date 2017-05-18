package org.ygl

import java.io.OutputStream
import java.io.PrintWriter
import java.util.*


typealias ConstFunction = (Int, Int) -> Int
typealias SymbolFunction = (Symbol, Symbol) -> Symbol
typealias BinaryOp = (Symbol, Symbol) -> Symbol

const val MARGIN  = 40
const val COMMENT_MARGIN  = 44

// https://esolangs.org/wiki/Brainfuck_algorithms
// https://www.codeproject.com/Articles/558979/BrainFix-the-language-that-translates-to-fluent-Br
class CodeGen(
        outputStream: OutputStream,
        val options: CompileOptions = DEFAULT_COMPILE_OPTIONS
) : AutoCloseable
{

    private var col = 0
    private var nestLevel = 0
    private var dataPointer = 0

    val io = IO(this)
    val math = Maths(this)

    val functions = HashMap<String, Function>()
    private val scopes = ArrayList<Scope>()
    private val output: PrintWriter = PrintWriter(outputStream)
    private val reservedChars = Regex("""[\[\]<>+-,.]""")

    override fun close() {
        output.flush()
        output.close()
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

    /**
     * moves rhs to lhs. rhs is not preserved
     */
    fun move(lhs: Symbol, rhs: Symbol): Symbol {
        commentLine("move $rhs to $lhs")

        setZero(lhs)
        moveTo(rhs)
        startLoop()
            inc(lhs)
            dec(rhs)
        endLoop()

        return lhs
    }

    fun assign(lhs: Symbol, rhs: Symbol): Symbol {
        when (rhs.type) {
            Type.STRING -> copyString(lhs, rhs)
            Type.INT    -> assignInt(lhs, rhs)
        }
        return lhs
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
        commentLine("end assign $rhs to $lhs")
        return lhs
    }

    fun startIf(condition: Symbol): Symbol {
        commentLine("if $condition")
        val elseFlag = currentScope().pushConditionFlag()
        loadInt(elseFlag, 1)    // loadInt else to 1

        val tmp = currentScope().getTempSymbol()
        assign(tmp, condition)
        moveTo(tmp)
        startLoop("zero else flag if $tmp is true")
            setZero(tmp)
            setZero(elseFlag)
            moveTo(tmp)
        endLoop()

        assign(tmp, condition)

        moveTo(tmp)
        startLoop()
        setZero(tmp)
        return tmp
    }

    // is passed the tmp variable from the startIf
    fun endIf(tmp: Symbol) {
        moveTo(tmp)
        endLoop()
        commentLine("end if")
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
        moveTo(condition)
        startLoop()
    }

    fun endWhile(condition: Symbol) {
        currentScope().popConditionFlag()
        moveTo(condition, comment="move to $condition")
        endLoop()
        commentLine("end while $condition")
    }

    fun print(symbol: Symbol): Symbol {
        newline()
        moveTo(symbol)
        return if (symbol.type == Type.STRING) {
            io.printString(symbol)
        } else {
            io.printInt(symbol)
        }
    }

    private fun debug(symbol: Symbol, comment: String) {
        moveTo(symbol)
        emit("\n`$comment`\n")
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
        return symbol
    }

    fun incrementBy(symbol: Symbol, value: Int): Symbol {
        if (value == 0) return symbol
        val ch = if (value < 0) "-" else "+"
        moveTo(symbol)
        emit(ch.repeat(Math.abs(value)), "increment $symbol by $value")
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

    // TODO
    fun readArray(array: Symbol, index: Symbol): Symbol {
        commentLine("read array $array($index)")

        val ret = currentScope().getTempSymbol()
        val data = array.offset(3)

        assign(array.offset(2), index)  // copy idx to readIdx
        assign(array.offset(1), index)  // copy idx to writeIdx
        setZero(data)                   // zero dataIdx

        moveTo(array)
        emit(">[>>>[-<<<<+>>>>]<<[->+<]<[->+<]>-]", "move read head to $index")
        emit(">>>[-<+<<+>>>]<<<[->>>+<<<]>", "")
        emit("[[-<+>]>[-<+>]<<<<[->>>>+<<<<]>>-]<<", "restore read head")

        assign(ret, data)

        commentLine("end read array $array($index)")
        return ret
    }

    fun writeArray(array: Symbol, index: Symbol, value: Symbol) {
        commentLine("write array $array($index) = $value")

        assign(array.offset(2), index) // copy idx to readIdx
        assign(array.offset(1), index) // copy idx to writeIdx
        assign(array.offset(3), value) // copy value to dataIdx

        moveTo(array)
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

    private inline fun newline() {
        write(System.lineSeparator())
        val indent = getIndent()
        col = indent.length
        write(indent)
    }

    private inline fun getIndent() = "  ".repeat(nestLevel)

    fun emit(code: String, comment: String = "") {
        if (options.verbose) {

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

    private inline fun write(code: String) {
        output.print(code)
        if (options.verbose) print(code)
    }
}
