package org.ygl

class IO(val codegen: CodeGen)
{
    fun readChar(symbol: Symbol): Symbol {
        with (codegen) {
            moveTo(symbol)
            emit(",", "read char $symbol")
        }
        return symbol
    }

    fun readInt(symbol: Symbol): Symbol {
        with (codegen) {
            commentLine("read int $symbol")
            moveTo(symbol)
            emit(",")
            emit("-".repeat(48), "convert char to int")
        }
        return symbol
    }

    fun printInt(symbol: Symbol): Symbol {
        with(codegen) {
            commentLine("print int $symbol")

            val cpy = currentScope().createSymbol("cpy")
            assign(cpy, symbol)

            val asciiOffset = 48
            val ten = currentScope().createSymbol("ten")
            loadInt(ten, 10)

            val d3 = math.mod(cpy, ten)
            math.divideBy(cpy, ten)
            val d2 = math.mod(cpy, ten)
            math.divideBy(cpy, ten)

            commentLine("print 100s char")
            assign(ten, cpy)
            moveTo(cpy)
            startLoop()
            incrementBy(cpy, asciiOffset)
            printChar(cpy)
            setZero(cpy)
            endLoop()

            commentLine("print 10s char")
            math.addTo(ten, d2)
            moveTo(ten)
            startLoop()
            incrementBy(d2, asciiOffset)
            printChar(d2)
            setZero(ten)
            endLoop()

            commentLine("print 1s char")
            moveTo(d3)
            incrementBy(d3, asciiOffset)
            printChar(d3)

            currentScope().delete(d3)
            currentScope().delete(d2)
            currentScope().delete(ten)
            currentScope().delete(cpy)
        }
        return symbol
    }

    fun printString(symbol: Symbol): Symbol {
        with (codegen) {
            commentLine("print str $symbol")
            for (i in 0 until symbol.size) {
                printChar(symbol, i)
            }
        }
        return symbol
    }

    fun printImmediate(chars: String) {
        with (codegen) {
            val tmp = currentScope().getTempSymbol()
            moveTo(tmp)
            for (i in 1..chars.length) {
                setZero(tmp)
                val intValue = chars[i].toInt()
                loadInt(tmp, intValue)
                emit(".")
            }
            currentScope().delete(tmp)
        }
    }

    fun printChar(symbol: Symbol, offset: Int = 0): Symbol {
        with (codegen) {
            moveTo(symbol, offset)
            emit(".", "print char $symbol")
        }
        return symbol
    }
}