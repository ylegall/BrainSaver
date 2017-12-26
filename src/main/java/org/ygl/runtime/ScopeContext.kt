package org.ygl.runtime

import org.ygl.ast.AstNode
import org.ygl.model.NullValue
import org.ygl.model.StorageType
import org.ygl.model.Value
import java.util.*

/**
 *
 */
class ScopeContext
{
    private val scopes = ArrayDeque<Scope>()

    init {
    }

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
            value: Value = NullValue
    ): Symbol {
        if (scopes.isEmpty()) throw Exception("addSymbol(): no current scope")

        return scopes.peek().createSymbol(name, storageType, value)
    }

    fun createSymbol(node: AstNode): Symbol {
        if (scopes.isEmpty()) throw Exception("addSymbol(): no current scope")
        return scopes.peek().createSymbol(node)
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

    operator fun contains(symbol: Symbol): Boolean {
        return contains(symbol.name)
    }

    operator fun contains(name: String): Boolean {
        return scopes.find { name in it.symbols } != null
    }
}