package org.ygl.runtime

import org.ygl.model.IntType
import org.ygl.model.StrType

class IO(
        private val cg: CodeGen,
        private val runtime: Runtime
)
{
    fun readChar(symbol: Symbol): Symbol {
        with (cg) {
            moveTo(symbol)
            emit(",", "read char $symbol")
        }
        return symbol
    }

    fun print(symbol: Symbol) {
        cg.newline()
        if (symbol.isConstant) {
            val symbolValue = symbol.value
            when (symbolValue) {
                is Int -> printImmediate(symbolValue.toString())
                is String -> printImmediate(symbolValue)
            }
        } else {
            // TODO
            when (symbol.type) {
                IntType -> printInt(symbol)
                StrType -> printString(symbol)
            }
        }
    }

    private fun printInt(symbol: Symbol): Symbol {
        with(cg) {
            commentLine("print int $symbol")

            val cpy = runtime.createSymbol("cpy")
            copyInt(cpy, symbol)

            val asciiOffset = 48
            val ten = runtime.createSymbol("ten")
            load(ten, 10)

            val d3 = math.mod(cpy, ten)
            math.divideBy(cpy, ten)
            val d2 = math.mod(cpy, ten)
            math.divideBy(cpy, ten)

            commentLine("print 100s char")
            copyInt(ten, cpy)

            cg.cf.onlyIf(cpy, {
                incrementBy(cpy, asciiOffset)
                printChar(cpy)
            })

            commentLine("print 10s char")
            math.addTo(ten, d2)
            cg.cf.onlyIf(ten, {
                incrementBy(d2, asciiOffset)
                printChar(d2)
            })

            commentLine("print 1s char")
            moveTo(d3)
            incrementBy(d3, asciiOffset)
            printChar(d3)

            runtime.delete(d3)
            runtime.delete(d2)
            runtime.delete(ten)
            runtime.delete(cpy)
        }
        return symbol
    }

    private fun printString(symbol: Symbol): Symbol {
        with (cg) {
            commentLine("print str $symbol")
            for (i in 0 until symbol.size) {
                printChar(symbol.offset(i))
            }
        }
        return symbol
    }

    fun printImmediate(chars: String) {
        with (cg) {
            val tmp = runtime.createTempSymbol()
            moveTo(tmp, "print immediate '$chars'")

            var last = 0
            for (i in 0 until chars.length) {
                val intValue = chars[i].toInt()
                if (last == 0) {
                    load(tmp, intValue)
                } else {
                    incrementBy(tmp, intValue - last)
                }
                emit(".")
                last = intValue
            }
            runtime.delete(tmp)
        }
    }

    private fun printChar(symbol: Symbol): Symbol {
        with (cg) {
            moveTo(symbol)
            emit(".", "print char $symbol")
        }
        return symbol
    }
}