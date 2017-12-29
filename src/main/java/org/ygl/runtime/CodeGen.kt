package org.ygl.runtime

import org.ygl.CompilerOptions
import org.ygl.DEFAULT_COMPILE_OPTIONS
import org.ygl.MinifyingOutputStream
import org.ygl.model.NullValue
import org.ygl.model.StrValue
import org.ygl.model.Value
import java.io.OutputStream

class CodeGen(
        outputStream: OutputStream = System.`out`,
        val options: CompilerOptions = DEFAULT_COMPILE_OPTIONS,
        private val runtime: Runtime
): AutoCloseable {

    private var col = 0
    private var nestLevel = 0
    private var dataPointer = 0

    private val reservedChars = Regex("""[\[\]<>+\-,.]""")
    private val margin = options.margin
    private val commentMargin = margin + 4

    val math = Maths(this, runtime)

    private val output: OutputStream = if (options.minify) {
        MinifyingOutputStream(outputStream, options.margin)
    } else {
        outputStream
    }

    override fun close() {
        output.flush()
    }

    fun loadImmediate(symbol: Symbol, value: Int): Symbol {
        if (value <= 0) return setZero(symbol) // non wrapping
        setZero(symbol)
        emit("+".repeat(value), "load $symbol = $value")
        //newline()
        return symbol
    }

    fun loadImmediate(symbol: Symbol, value: String): Symbol {
        assert(symbol.size == value.length, { "loadString() string size larger than symbol size" })
        commentLine("load ${symbol.name} = \"$value\"")
        moveTo(symbol)
        for (i in 0 until symbol.size) {
            val intValue = value[i].toInt()
            loadImmediate(symbol.offset(i), intValue)
        }
        symbol.value = StrValue(value)
        return symbol
    }

    fun copyInt(lhs: Symbol, rhs: Symbol): Symbol {
        commentLine("assign $rhs to $lhs")

        val tmp = runtime.createTempSymbol()
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

        runtime.delete(tmp)
        commentLine("end assign $rhs to $lhs")
        return lhs
    }

    fun copyStr(lhs: Symbol, rhs: Symbol): Symbol {
        TODO("implement")
    }

    inline fun onlyIf(symbol: Symbol, body: () -> Unit, comment: String = "") {
        moveTo(symbol)
        startLoop(comment)
        setZero(symbol)
        body()
        moveTo(symbol)
        endLoop()
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

    private fun getIndent() = "  ".repeat(nestLevel)

    fun emit(code: String, comment: String = "") {
        if (!options.minify) {

            var text = code
            var cmt = comment
            while (!text.isEmpty()) {
                if (col == margin) newline()
                if (text.length + col > margin) {
                    val prefix = margin - col
                    write(text.substring(0, prefix))
                    text = text.substring(prefix)
                    col = margin
                } else {
                    write(text)
                    col += text.length
                    text = ""
                }

                if (!cmt.isEmpty()) {
                    assert(!cmt.contains(reservedChars))
                    val commentPadding = commentMargin - col
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