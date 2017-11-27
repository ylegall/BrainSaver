package org.ygl

import org.antlr.v4.runtime.ParserRuleContext
import java.util.*


/**
 *
 */
class Scope(val startAddress: Int, val functionName: String = "") {

    private var tempCounter = 0

    var scopeSize = 0
        private set

    private val symbolMap = HashMap<String, Symbol>()
    private val freeSlots = ArrayDeque<Symbol>()
    val loopContexts = ArrayDeque<ParserRuleContext>()

    fun getSymbol(name: String): Symbol? = symbolMap[name]

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
        var address: Int? = null
        if (!freeSlots.isEmpty()) {
            val slot = freeSlots.find { it.size >= size }
            if (slot != null) {
                freeSlots.remove(slot)
                address = slot.address
                if (slot.size > size) {
                    freeSlots.addFirst(Symbol(
                            slot.name,
                            slot.size - size,
                            slot.address + size,
                            slot.type,
                            slot.value)
                    )
                }
                if (address + size >= (startAddress + scopeSize)) {
                    scopeSize = address + size
                }
            }
        }

        if (address == null) {
            address = startAddress + scopeSize
            scopeSize += size
        }

        val symbol = Symbol(name, size, address, type, value)
        symbolMap.put(name, symbol)
        return symbol
    }

    fun createSymbol(name: String, other: Symbol): Symbol {
        return createSymbol(name, other.size, other.type, other.value)
    }

    fun rename(symbol: Symbol, name: String) {
        symbolMap.remove(symbol.name)
        symbol.name = name
        symbolMap.put(name, symbol)
    }

    fun delete(symbol: Symbol) {
        symbolMap.remove(symbol.name) ?: throw Exception("undefined symbol: ${symbol.name}")
        if (symbol.address + symbol.size == startAddress + scopeSize) {
            scopeSize -= symbol.size
        } else {
            freeSlots.add(symbol)
        }
    }

    fun deleteTemps() {
        var maxAddress = startAddress + 1
        val garbageList = ArrayList<Symbol>()

        for (entry in symbolMap) {
            val symbol = entry.value
            if (symbol.name.startsWith("$")) {
                garbageList.add(symbol)
            } else {
                val sym = entry.value
                maxAddress = Math.max(maxAddress, sym.address + sym.size - 1)
            }
        }

        garbageList.forEach {
            symbolMap.remove(it.name)
            freeSlots.add(it)
        }

        scopeSize = maxAddress - startAddress + 1
        tempCounter = 0
    }
}