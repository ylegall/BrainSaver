package org.ygl.transformation

import org.ygl.ast.*
import org.ygl.runtime.NamedSymbol
import org.ygl.runtime.ScopeContext

/**
 *
 */
internal class SymbolInfo(
        val symbolsRead: MutableSet<String> = mutableSetOf(),
        val symbolsWritten: MutableSet<String> = mutableSetOf(),
        val symbolsDeclared: MutableSet<String> = mutableSetOf()
)

/**
 *
 */
class ScopeSymbols
(
        val symbolsRead: Set<String>,
        val symbolsWritten: Set<String>,
        val symbolsDeclared: Set<String>
) {
    val unusedSymbols = symbolsDeclared.minus(symbolsRead)
}

/**
 *
 */
internal class MutabilityResolver() : AstWalker<SymbolInfo>()
{

    private val scopeContext = ScopeContext<NamedSymbol>()
    private val symbolInfo = mutableMapOf<AstNode, ScopeSymbols>()

    fun getSymbolMutabilityInfo(ast: AstNode): Map<AstNode, ScopeSymbols> {
        visit(ast)
        return symbolInfo
    }

    override fun visit(node: FunctionNode): SymbolInfo {
        return visitScope(node)
    }

    override fun visit(node: IfStatementNode): SymbolInfo {
        return visitScope(node)
    }

    override fun visit(node: ForStatementNode): SymbolInfo {
        return visitScope(node)
    }

    override fun visit(node: WhileStatementNode): SymbolInfo {
        return visitScope(node)
    }

    private fun visitScope(node: AstNode): SymbolInfo {
        scopeContext.enterScope(node)
        val result = visitChildren(node)
        symbolInfo.put(node, ScopeSymbols(
                result.symbolsRead,
                result.symbolsWritten,
                result.symbolsDeclared)
        )
        scopeContext.exitScope()
        return result
    }

    override fun visit(node: DeclarationNode): SymbolInfo {
        scopeContext.addSymbol(NamedSymbol(node.lhs))
        val result = visit(node.rhs)
        result.symbolsDeclared.add(node.lhs)
        result.symbolsWritten.add(node.lhs)
        return result
    }

    override fun visit(node: AssignmentNode): SymbolInfo {
        val result = visit(node.rhs)
        result.symbolsWritten.add(node.lhs)
        return result
    }

    override fun visit(node: ArrayConstructorNode): SymbolInfo {
        return SymbolInfo(symbolsWritten = mutableSetOf(node.array))
    }

    override fun visit(node: ArrayLiteralNode): SymbolInfo {
        val result = visit(node.items)
        result.symbolsWritten.add(node.array)
        return result
    }

    override fun visit(node: ArrayReadExpNode): SymbolInfo {
        val result = visit(node.idx)
        result.symbolsRead.add(node.array)
        return result
    }

    override fun visit(node: ArrayWriteNode): SymbolInfo {
        val result = visit(node.rhs)
        result.symbolsRead.addAll(visit(node.idx).symbolsRead)
        result.symbolsWritten.add(node.array)
        return result
    }

    override fun visit(node: AtomIdNode): SymbolInfo {
        return SymbolInfo(symbolsRead = mutableSetOf(node.identifier))
    }

    override fun aggregateResult(agg: SymbolInfo, next: SymbolInfo): SymbolInfo {
        agg.symbolsRead.addAll(next.symbolsRead)
        agg.symbolsWritten.addAll(next.symbolsWritten)
        agg.symbolsDeclared.addAll(next.symbolsDeclared)
        return agg
    }

    override fun defaultValue() = SymbolInfo()
}
