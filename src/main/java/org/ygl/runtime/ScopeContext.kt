package org.ygl.runtime

import org.ygl.ast.AstNode
import java.util.*

/**
 *
 */
class ScopeContext<SymbolType: NamedSymbol>
{
    private val scopes = ArrayDeque<Scope<AstNode, SymbolType>>()

    init {
    }

    fun enterScope(node: AstNode) {
        scopes.push(Scope(node))
    }

    fun exitScope() {
        scopes.pop()
    }

    fun addSymbol(symbol: SymbolType) {
        scopes.peek().symbols.put(symbol.getKey(), symbol)
    }

    fun resolveSymbol(name: String): SymbolType? {
        return scopes.find { name in it.symbols }?.symbols?.get(name)
    }

    operator fun contains(symbol: SymbolType): Boolean {
        return contains(symbol.getKey())
    }

    operator fun contains(name: String): Boolean {
        return scopes.find { name in it.symbols } != null
    }
}