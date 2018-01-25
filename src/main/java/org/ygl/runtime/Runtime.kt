package org.ygl.runtime

import org.ygl.ast.AstNode
import org.ygl.model.IntType
import org.ygl.model.StorageType
import org.ygl.model.Type
import org.ygl.model.getType
import java.util.*

/**
 *
 */
class Runtime
{
    private val scopes = ArrayDeque<Scope>()

    fun enterScope(node: AstNode) {
        if (scopes.isEmpty()) {
            scopes.push(Scope(node, 0))
        } else {
            val top = scopes.peek()
            scopes.push(Scope(node, top.headPointer))
        }
    }

    fun currentScope() = scopes.peek()

    fun exitScope() {
        scopes.pop()
    }

    fun createSymbol(
            name: String,
            storageType: StorageType = StorageType.VAL,
            type: Type = IntType,
            size: Int = 1
    ): Symbol {
        if (scopes.isEmpty()) throw Exception("addSymbol(): no current scope")

        return scopes.peek().createSymbol(name, storageType, size, type)
    }

    fun <T: Any> createSymbol(
            name: String,
            storageType: StorageType = StorageType.VAL,
            value: T
    ): Symbol {
        if (scopes.isEmpty()) throw Exception("addSymbol(): no current scope")

        return scopes.peek().createSymbol(name, storageType, getSize(value), getType(value))
    }

    fun createTempSymbol(
            size: Int = 1,
            type: Type = IntType
    ): Symbol {
        if (scopes.isEmpty()) throw Exception("addSymbol(): no current scope")

        return scopes.peek().createTempSymbol(size, type )
    }

    fun createSymbol(node: AstNode): Symbol {
        if (scopes.isEmpty()) throw Exception("addSymbol(): no current scope")
        return scopes.peek().createSymbol(node)
    }

    fun rename(symbol: Symbol, newName: String): Symbol {
        if (symbol.isTemp) {
            currentScope().tempSymbols.remove(symbol.name)
        } else {
            currentScope().symbols.remove(symbol.name)
        }
        val newSymbol = Symbol.new(newName, symbol.storage, symbol.size, symbol.type, symbol.address)
        currentScope().symbols[newName] = newSymbol
        return symbol
    }

    fun delete(symbol: Symbol) {
        scopes.peek()?.delete(symbol)
    }

    fun deleteTempSymbols() {
        scopes.peek()?.deleteTempSymbols()
    }

    fun resolveSymbol(name: String): Symbol? {
        return scopes.find { name in it.symbols }?.symbols?.get(name)
    }

    fun resolveLocalSymbol(name: String): Symbol? {
        return scopes.peek()?.symbols?.get(name)
    }

    operator fun contains(name: String): Boolean {
        return scopes.find { name in it.symbols } != null
    }
}