package org.ygl

class Scope(val startAddress: Int) {

    private var tempCounter = 0
    private var scopeSize = 0
    private val symbolMap = HashMap<String, Symbol>()

    fun addSymbol(symbol: Symbol) {
        if (!symbolMap.containsKey(symbol.name)) {
            symbolMap.put(symbol.name, symbol)
            scopeSize += symbol.size
        } else {
            throw Exception("scope.addSymbol(): duplicate symbol: " + symbol.name)
        }
    }

    fun getSymbol(name: String): Symbol? {
        return symbolMap[name]
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
        var symbol = Symbol(name, size, startAddress + scopeSize, type, value)
        symbolMap.put(name, symbol)
        scopeSize += size
        return symbol
    }
}