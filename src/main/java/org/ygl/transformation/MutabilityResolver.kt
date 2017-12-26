package org.ygl.transformation

import org.ygl.ast.*
import org.ygl.runtime.ScopeContext

/**
 *
 */
class SymbolInfo(
        val modifiedSymbols: MutableSet<String> = mutableSetOf(),
        val deadStores: MutableMap<String, MutableSet<AstNode>> = mutableMapOf()
) {
    fun isDeadStore(node: StoreNode): Boolean {
        return deadStores[node.lhs]?.contains(node) == true
    }
}

private typealias ModifiedSymbols = MutableSet<String>

/**
 *
 */
internal class MutabilityResolver : AstWalker<ModifiedSymbols>()
{
    private val scopeContext = ScopeContext()
    private val scopeSymbolInfo = mutableMapOf<AstNode, SymbolInfo>()

    fun getSymbolMutabilityInfo(ast: AstNode): Map<AstNode, SymbolInfo> {
        visit(ast)
        return scopeSymbolInfo
    }

    override fun visit(node: FunctionNode): ModifiedSymbols {
        return visitScope(node)
    }

    override fun visit(node: IfStatementNode): ModifiedSymbols {
        return visitScope(node)
    }

    override fun visit(node: ForStatementNode): ModifiedSymbols {
        val children = mutableListOf<AstNode>(node.start, node.start, node.inc)
        children.addAll(node.statements)
        return visitScope(node, children)
    }

    override fun visit(node: WhileStatementNode): ModifiedSymbols {
        val children = mutableListOf<AstNode>(node.condition)
        children.addAll(node.statements)
        children.add(node.condition)
        return visitScope(node, children)
    }

    private fun visitScope(node: AstNode, children: Iterable<AstNode> = node.children): ModifiedSymbols {
        scopeContext.enterScope(node)
        val symbolInfo = SymbolInfo()
        scopeSymbolInfo.put(node, symbolInfo)


        val result = visit(children)
        symbolInfo.modifiedSymbols.addAll(result)

        // remove empty dead store entries
        val it = symbolInfo.deadStores.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (entry.value.isEmpty()) {
                it.remove()
            }
        }

        scopeContext.exitScope()
        return result
    }

    override fun visit(node: DeclarationNode): ModifiedSymbols {
        scopeContext.createSymbol(node.lhs)
        val result = visit(node.rhs)
        recordSymbolWrite(node, node.lhs)
        return result
    }

    override fun visit(node: AssignmentNode): ModifiedSymbols {
        val result = visit(node.rhs)
        result.add(node.lhs)
        recordSymbolWrite(node, node.lhs)
        return result
    }

    override fun visit(node: ArrayConstructorNode): ModifiedSymbols {
        recordSymbolWrite(node, node.array)
        return mutableSetOf(node.array)
    }

    override fun visit(node: ArrayLiteralNode): ModifiedSymbols {
        val result = visit(node.items)
        result.add(node.array)
        recordSymbolWrite(node, node.array)
        return result
    }

    override fun visit(node: ArrayReadExpNode): ModifiedSymbols {
        recordSymbolRead(node.array)
        return visit(node.idx)
    }

    override fun visit(node: ArrayWriteNode): ModifiedSymbols {
        val result = visit(node.rhs)
        result.add(node.array)
        recordSymbolWrite(node, node.array)
        return result
    }

    override fun visit(node: AtomIdNode): ModifiedSymbols {
        recordSymbolRead(node.identifier)
        return mutableSetOf()
    }

    private fun recordSymbolWrite(node: AstNode, name: String) {
        val scope = scopeContext.findScopeWithSymbol(name)
        if (scope != null) {
            scopeSymbolInfo[scope.node]
                    ?.deadStores
                    ?.getOrPut(name, { mutableSetOf() })
                    ?.add(node)
        }
    }

    private fun recordSymbolRead(name: String) {
        val scope = scopeContext.findScopeWithSymbol(name)
        if (scope != null) {
            scopeSymbolInfo[scope.node]?.deadStores?.get(name)?.clear()
        }
    }

    override fun aggregateResult(agg: ModifiedSymbols, next: ModifiedSymbols): ModifiedSymbols {
        agg.addAll(next)
        return agg
    }

    override fun defaultValue() = mutableSetOf<String>()
}
