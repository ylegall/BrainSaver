package org.ygl.runtime

import org.ygl.ast.AstNode
import org.ygl.model.StorageType
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
            val startAddress = top.startAddress + top.scopeSize
            scopes.push(Scope(node, startAddress))
        }
    }

    fun currentScope() = scopes.peek()

    fun exitScope() {
        scopes.pop()
    }

    fun createSymbol(
            name: String,
            storageType: StorageType = StorageType.VAL,
            value: Any = Unit
    ): Symbol {
        if (scopes.isEmpty()) throw Exception("addSymbol(): no current scope")

        return scopes.peek().createSymbol(name, storageType, value)
    }

    fun createTempSymbol(
            value: Any = Unit
    ): Symbol {
        if (scopes.isEmpty()) throw Exception("addSymbol(): no current scope")

        return scopes.peek().createTempSymbol(value)
    }

    fun createSymbol(node: AstNode): Symbol {
        if (scopes.isEmpty()) throw Exception("addSymbol(): no current scope")
        return scopes.peek().createSymbol(node)
    }

    fun rename(symbol: Symbol, newName: String): Symbol {
        // TODO: add comment in source output
        currentScope().symbols.remove(symbol.name)
        val newSymbol = Symbol(newName, symbol.storage, symbol.value, symbol.address)
        currentScope().symbols[newName] = newSymbol
        return symbol
    }

    fun delete(symbol: Symbol) {
        // TODO
    }

    fun resolveSymbol(name: String): Symbol? {
        return scopes.find { name in it.symbols }?.symbols?.get(name)
    }

    fun findScopeWithSymbol(name: String): Scope? {
        return scopes.find { name in it.symbols }
    }

    fun resolveLocalSymbol(name: String): Symbol? {
        return scopes.peek()?.symbols?.get(name)
    }

    operator fun contains(name: String): Boolean {
        return scopes.find { name in it.symbols } != null
    }
}