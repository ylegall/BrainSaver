package org.ygl.runtime

import org.ygl.ast.AstNode
import org.ygl.ast.DeclarationNode
import org.ygl.ast.GlobalVariableNode
import org.ygl.model.IntType
import org.ygl.model.StorageType
import org.ygl.model.Type
import org.ygl.util.orElse
import java.lang.Math.max
import java.util.*

/**
 *
 */
class Scope(
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
        var address = checkFreeSlots(size)
        if (address == null) {
            address = headPointer
            assert(size > 0)
            headPointer += size
        }

        val symbol = Symbol.new(name, storageType, size, type, address)
        symbols[name] = symbol
        return symbol
    }

    private fun checkFreeSlots(size: Int = 1): Int? {
        return if (freeSlots.isNotEmpty()) {
            freeSlots.find { it.size >= size }
                ?.let { slot ->
                    freeSlots.remove(slot)
                    if (slot.size > size) {
                        freeSlots.add(Symbol.new(
                                slot.name,
                                slot.storage,
                                slot.size - size,
                                slot.type,
                                slot.address + size
                        ))
                    }
                    headPointer = max(headPointer, slot.address + size)
                    slot
                }?.address
        } else {
            null
        }
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
        val name = "\$t$tempCounter"
        tempCounter++
        assert(size > 0)
        val address = checkFreeSlots(size) ?: headPointer.also { headPointer += size }
        val symbol = Symbol.temp(name, StorageType.VAR, size, type, address)
        tempSymbols[name] = symbol
        return symbol
    }

}
