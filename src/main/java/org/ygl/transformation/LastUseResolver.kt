package org.ygl.transformation

import org.ygl.ast.*
import org.ygl.runtime.Runtime

/**
 * TODO: test
 */
class LastUseResolver : AstWalker<Set<String>>()
{
    private val stmtSymbolsMap = mutableMapOf<AstNode, MutableSet<String>>()
    private val previousUseMap = mutableMapOf<String, AstNode>()
    private val scopeContext = Runtime()

    fun getSymbolLastUseInfo(astNode: AstNode): Map<AstNode, Set<String>> {
        visit(astNode)
        return stmtSymbolsMap
    }

    override fun visit(node: ProgramNode): Set<String> {
        node.children.filterIsInstance<FunctionNode>().forEach { visit(it) }
        return defaultValue(node)
    }

    override fun visit(node: FunctionNode): Set<String> {
        scopeContext.enterScope(node)
        val result = visitChildren(node)
        scopeContext.exitScope()
        return result
    }

    override fun visit(node: StatementNode): Set<String> {
        return recordSymbolsUsed(node, visit(node.children))
    }

    override fun visit(node: ForStatementNode): Set<String> {
        return recordScopeSymbols(node)
    }

    override fun visit(node: IfStatementNode): Set<String> {
        return recordScopeSymbols(node)
    }

    override fun visit(node: WhileStatementNode): Set<String> {
        return recordScopeSymbols(node)
    }

    private fun recordScopeSymbols(node: AstNode): Set<String> {
        scopeContext.enterScope(node)
        val result = visit(node.children)
        scopeContext.exitScope()
        return result
    }

    private fun recordSymbolsUsed(node: AstNode, symbols: Set<String>): Set<String> {
        if (symbols.isEmpty()) return symbols
        symbols.filter { scopeContext.resolveLocalSymbol(it) != null }
                .forEach { symbol ->
                    // remove old stmt mapping:
                    previousUseMap[symbol]?.let {
                        stmtSymbolsMap[it]?.remove(symbol)
                        if (stmtSymbolsMap[it]?.isEmpty() == true) {
                            stmtSymbolsMap.remove(it)
                        }
                    }
                    stmtSymbolsMap.getOrPut(node, { mutableSetOf() }).add(symbol)
                    previousUseMap[symbol] = node
                }
        return symbols
    }

    override fun visit(node: DeclarationNode): Set<String> {
        scopeContext.createSymbol(node)
        return visit(node.rhs)
    }

    override fun visit(node: AtomIdNode): Set<String> {
        return setOf(node.identifier)
    }

    override fun aggregateResult(agg: Set<String>, next: Set<String>): Set<String> {
        return agg + next
    }

    override fun defaultValue(node: AstNode): Set<String> {
        return emptySet()
    }
}