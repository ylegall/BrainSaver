package org.ygl.runtime

import org.ygl.ast.*
import org.ygl.model.NullValue
import org.ygl.model.StorageType
import org.ygl.model.Value

/**
 *
 */
class Scope(
        val node: AstNode,
        val startAddress: Int
)
{
    val symbols = mutableMapOf<String, Symbol>()
    val tempSymbols = mutableMapOf<String, Symbol>()

    var scopeSize = 0
        private set

    private var tempCounter = 0

    fun createSymbol(node: AstNode): Symbol {
        return when(node) {
            is GlobalVariableNode -> createSymbol(node.lhs, node.storage)
            is DeclarationNode -> createSymbol(node.lhs, node.storage)
            else -> throw Exception("not implemented")
        }
    }

    fun createSymbol(name: String, storageType: StorageType, value: Value = NullValue): Symbol {
        // TODO: check free slots
        val address = startAddress + scopeSize
        val symbol = Symbol(name, storageType, value, address)
        symbols[name] = symbol
        scopeSize += symbol.size
        return symbol
    }

    fun createSymbol(name: String, storageType: StorageType): Symbol {
        val address = startAddress + scopeSize
        val symbol = Symbol(name, storageType, NullValue, address)
        symbols[name] = symbol
        scopeSize += symbol.size
        return symbol
    }

    fun delete(symbol: Symbol) {
        assert(symbol.hasAddress(),
                { "symbol ${symbol.name} does not have an address" }
        )
        if (symbol.isTemp()) {
            assert(tempSymbols.remove(symbol.name) != null,
                    { "symbol ${symbol.name} was not a temp symbol" }
            )
        } else {
            assert(symbols.remove(symbol.name) != null,
                    { "symbol ${symbol.name} was not found for deletion" }
            )
        }
    }

    fun createTempSymbol(value: Value): Symbol {
        // TODO
        val address = startAddress + scopeSize
        val name = "\$t$tempCounter"
        tempCounter++
        val symbol = TempSymbol(name, value, address)
        scopeSize += symbol.size
        return symbol
    }
}
