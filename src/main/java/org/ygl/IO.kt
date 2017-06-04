package org.ygl

class IO(val cg: CodeGen)
{
    fun readChar(symbol: Symbol): Symbol {
        with (cg) {
            moveTo(symbol)
            emit(",", "read char $symbol")
        }
        return symbol
    }

    fun readInt(symbol: Symbol): Symbol {
        with (cg) {
            commentLine("read int $symbol")
            moveTo(symbol)
            emit(",")
            emit("-".repeat(48), "convert char to int")
        }
        return symbol
    }

    fun <T> print(t: T) {
        cg.newline()
        if (t is Symbol) {
            cg.moveTo(t)
            if (t.type == Type.STRING) {
                printString(t)
            } else {
                printInt(t)
            }
        } else if (t is String) {
            printImmediate(t)
        }
    }

    fun printInt(symbol: Symbol): Symbol {
        with(cg) {
            commentLine("print int $symbol")
            val cs = currentScope()

            val cpy = cs.createSymbol("cpy")
            assign(cpy, symbol)

            val asciiOffset = 48
            val ten = cs.createSymbol("ten")
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

            cs.delete(d3)
            cs.delete(d2)
            cs.delete(ten)
            cs.delete(cpy)
        }
        return symbol
    }

    fun printString(symbol: Symbol): Symbol {
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
            val cs = currentScope()
            val tmp = cs.getTempSymbol()
            moveTo(tmp)
            for (i in 0 until chars.length) {
                setZero(tmp)
                val intValue = chars[i].toInt()
                loadInt(tmp, intValue)
                emit(".")
            }
            cs.delete(tmp)
        }
    }

    fun printChar(symbol: Symbol): Symbol {
        with (cg) {
            moveTo(symbol)
            emit(".", "print char $symbol")
        }
        return symbol
    }
}