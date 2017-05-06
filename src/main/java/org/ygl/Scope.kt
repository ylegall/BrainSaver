package org.ygl

import java.util.*

//typealias IfElseFlags = Pair<Symbol, Symbol>

val returnSymbolName = "#return"
val zeroSymbolName = "#0"

class Scope(val startAddress: Int) {

    private var tempCounter = 0
    var returnCount = 0

    var scopeSize = 0
        private set

    private val symbolMap = HashMap<String, Symbol>()
    private val freeSlots = PriorityQueue<Symbol>()
    //private val conditionFlags = ArrayDeque<IfElseFlags>()
    private val conditionFlags = ArrayDeque<Symbol>()

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
            address = freeSlots.remove().address
            if (address >= (startAddress + scopeSize)) {
                scopeSize = address + size
            }
        } else {
            address = startAddress + scopeSize
            scopeSize += size
        }

        var symbol = Symbol(name, size, address, type, value)
        symbolMap.put(name, symbol)
        return symbol
    }

    fun createSymbol(name: String, other: Symbol): Symbol {
        return createSymbol(name, other.size, other.type, other.value)
    }

    fun pushConditionFlag(): Symbol {
        val flag = createSymbol("&" + conditionFlags.size)
        conditionFlags.push(flag)
        return flag
    }

    fun getConditionFlag(): Symbol {
        return conditionFlags.peek() ?: throw Exception("condition flags empty")
    }

    fun popConditionFlag() {
        val flag = conditionFlags.pop()
        delete(flag)
    }

    fun delete(symbol: Symbol) {
        if (!symbolMap.containsKey(symbol.name)) throw Exception("undefined symbol: ${symbol.name}")
        freeSlots.add(symbol)
        symbolMap.remove(symbol.name)
        if (symbol.address == scopeSize - symbol.size) {
            scopeSize -= symbol.size
        }
    }

    fun deleteTemps() {
        var maxAddress = startAddress + 2
        val garbageList = ArrayList<Symbol>()

        for (entry in symbolMap) {
            val symbol = entry.value
            if (symbol.name.startsWith("$")) {
                garbageList.add(symbol)
            } else {
                maxAddress = Math.max(maxAddress, entry.value.address)
            }
        }

        //garbageList.addAll(symbolMap.filterKeys { it.startsWith("$") }.values)
        garbageList.forEach {
            symbolMap.remove(it.name)
            freeSlots.add(it)
        }

        scopeSize = maxAddress - startAddress + 1
    }
}