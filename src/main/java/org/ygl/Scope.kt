package org.ygl

import java.util.*

val returnSymbolName = "#return"
val zeroSymbolName = "#0"

class Scope(val startAddress: Int) {

    private var tempCounter = 0
    var returnCount = 0

    var scopeSize = 0
        private set

    private val symbolMap = HashMap<String, Symbol>()
    private val freeSlots = ArrayDeque<Symbol>()

    init {
        createSymbol(zeroSymbolName, size = 1, type = Type.INT, value = 0)
        createSymbol(returnSymbolName, size = 1, type = Type.INT, value = 0)
    }

    fun getSymbol(name: String): Symbol? {
        return symbolMap[name]
    }

    fun getReturnSymbol(): Symbol {
        return symbolMap[returnSymbolName]!!
    }

    fun getZeroSymbol(): Symbol {
        return symbolMap[zeroSymbolName]!!
    }

    fun getOrCreateSymbol(name: String, size: Int = 1, type: Type = Type.INT): Symbol {
        return symbolMap[name] ?: createSymbol(name, size, type)
    }

    fun getTempSymbol(type: Type = Type.INT, size: Int = 1): Symbol {
        var name = "\$t" + tempCounter
        while (symbolMap[name] != null) {
            tempCounter += 1
            name = "\$t" + tempCounter
        }
        return this.createSymbol(name, size, type)
    }

    fun createSymbol(name: String, size: Int = 1, type: Type = Type.INT, value: Any? = null): Symbol {
        if (symbolMap.containsKey(name)) throw Exception("duplicate symbol: $name")

        // check for freed slots
        var address: Int
        if (!freeSlots.isEmpty()) {
            address = freeSlots.pop().address
        } else {
            address = startAddress + scopeSize
            scopeSize += size
        }

        var symbol = Symbol(name, size, address, type, value)
        symbolMap.put(name, symbol)
        return symbol
    }

    fun createSymbol(name: String, other: Symbol): Symbol {
        if (symbolMap.containsKey(name)) throw Exception("duplicate symbol: $name")

        // check for freed slots
        var address: Int
        if (!freeSlots.isEmpty()) {
            address = freeSlots.pop().address
        } else {
            address = startAddress + scopeSize
            scopeSize += other.size
        }

        var symbol = Symbol(name, other.size, address, other.type, other.value)
        symbolMap.put(name, symbol)
        return symbol
    }

    fun delete(symbol: Symbol) {
        if (!symbolMap.containsKey(symbol.name)) throw Exception("undefined symbol: ${symbol.name}")
        freeSlots.add(symbol)
        symbolMap.remove(symbol.name)
    }

    fun deleteTemps() {
        val garbageList = ArrayList<Symbol>()
        garbageList.addAll(symbolMap.filterKeys { it.startsWith("$") }.values)
        garbageList.forEach {
            symbolMap.remove(it.name)
            freeSlots.add(it)
        }
    }
}