package org.ygl.runtime

import org.ygl.ast.AstNode
import org.ygl.ast.DeclarationNode
import org.ygl.ast.GlobalVariableNode
import org.ygl.model.IntType
import org.ygl.model.StorageType
import org.ygl.model.Type
import org.ygl.util.orElse
import java.util.*

/**
 *
 */
class Scope(
        val node: AstNode,
        private val startAddress: Int
)
{
    val symbols = mutableMapOf<String, Symbol>()
    val tempSymbols = mutableMapOf<String, Symbol>()
    private val freeSlots = ArrayDeque<Symbol>()

    var headPointer = startAddress
        private set

    private var tempCounter = 0

    fun createSymbol(node: AstNode): Symbol {
        return when(node) {
            is GlobalVariableNode -> createSymbol(node.lhs, StorageType.VAR)
            is DeclarationNode -> createSymbol(node.lhs, node.storage)
            else -> throw Exception("not implemented")
        }
    }

    fun createSymbol(name: String, storageType: StorageType, size: Int = 1, type: Type = IntType): Symbol {
        if (symbols.containsKey(name)) throw Exception("duplicate symbol: $name")

        // check for freed slots
        var address: Int? = null
        if (freeSlots.isNotEmpty()) {
            val slot = freeSlots.find { it.size >= size }
            if (slot != null) {
                freeSlots.remove(slot)
                address = slot.address
                if (slot.size > size) {
                    freeSlots.addFirst(Symbol.new(
                            slot.name,
                            slot.storage,
                            slot.size - size,
                            slot.type,
                            slot.address + size
                    ))
                }
                if (address + size >= headPointer) {
                    headPointer = address + size
                }
            }
        }

        if (address == null) {
            address = headPointer
            headPointer += size
        }

        val symbol = Symbol.new(name, storageType, size, type, address)
        symbols[name] = symbol
        return symbol
    }

    fun delete(symbol: Symbol) {
        val oldSymbol = tempSymbols.remove(symbol.name)
                .orElse { symbols.remove(symbol.name) } ?: throw Exception("unknown symbol ${symbol.name}")
        deleteInternal(oldSymbol)
    }

    fun deleteTempSymbols() {
        tempSymbols.forEach { _, tempSymbol ->
            deleteInternal(tempSymbol)
        }
        tempSymbols.clear()
        tempCounter = 0
    }

    private fun deleteInternal(symbol: Symbol) {
        if (symbol.address + symbol.size == headPointer) {
            assert(symbol.size > 0)
            headPointer -= symbol.size
            assert(headPointer >= startAddress)
        } else {
            freeSlots.add(symbol)
        }
    }

    fun createTempSymbol(size: Int = 1, type: Type = IntType): Symbol {
        val address = headPointer
        val name = "\$t$tempCounter"
        tempCounter++
        val symbol = Symbol.temp(name, StorageType.VAR, size, type, address)
        headPointer += symbol.size
        tempSymbols[name] = symbol
        return symbol
    }
}
