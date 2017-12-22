package org.ygl.transformation

import org.ygl.ast.*
import java.util.*

/**
 * information returned from walking tree nodes
 */
internal class SymbolResult (
    val readSymbols: MutableSet<String> = mutableSetOf(),
    val writtenSymbols: MutableSet<String> = mutableSetOf()
)

internal class SymbolAnalysis() : AstWalker<SymbolResult>()
{
    private val emptyResult = SymbolResult()
    private val scopeInfo = ArrayDeque<ScopeInfo>()
    //private val functionStack = ArrayDeque<String>()

    private class ScopeInfo(val function: FunctionNode)
    {
        val declaredSymbols = mutableSetOf<String>()
        val loopSymbolsWritten = mutableMapOf<AstNode, MutableSet<String>>()
        val lastSymbolsUsedMap = mutableMapOf<AstNode, MutableSet<String>>()
        val symbolUseMap = mutableMapOf<String, AstNode>()
    }

    override fun visit(node: ArrayConstructorNode) = recordWriteSymbol(node.array)
    override fun visit(node: ArrayLiteralNode) = recordWriteSymbol(node.array)
    override fun visit(node: ArrayWriteNode) = recordWriteSymbol(node.array, node)

    override fun visit(node: AssignmentNode): SymbolResult {
        val result = visit(node.rhs)
        result.writtenSymbols.add(node.lhs)
        return result
    }

    override fun visit(node: AtomIdNode): SymbolResult {
        return SymbolResult(readSymbols = mutableSetOf(node.identifier))
    }

    override fun visit(node: DeclarationNode): SymbolResult {
        scopeInfo.peek().declaredSymbols.add(node.lhs)
        val result = visit(node.rhs)
        result.writtenSymbols.add(node.lhs)
        return result
    }

    override fun visit(node: FunctionNode): SymbolResult {
        scopeInfo.push(ScopeInfo(node))
        val result = visitChildren(node)
        scopeInfo.pop()
        return result
    }

    override fun visit(node: StatementNode): SymbolResult {
        val result = visitChildren(node)
        node.recordSymbolUsed(result)
        return result
    }

    override fun visit(node: ForStatementNode) = visitConditional(node)
    override fun visit(node: IfStatementNode) = visitConditional(node)
    override fun visit(node: WhileStatementNode) = visitConditional(node)

    private fun visitConditional(node: AstNode): SymbolResult {
        val result = visitChildren(node)
        scopeInfo.peek().loopSymbolsWritten.put(node, result.writtenSymbols)
        return result
    }

    private fun AstNode.recordSymbolUsed(result: SymbolResult) {
        val info = scopeInfo.peek()
        result.readSymbols.forEach { symbol ->
            val oldStatement = info.symbolUseMap[symbol]
            if (oldStatement != null) {
                val lastUseSet = info.lastSymbolsUsedMap[oldStatement]
                if (lastUseSet?.isEmpty() == true) {
                    info.lastSymbolsUsedMap.remove(oldStatement)
                }
            }
            val symbolSet = info.lastSymbolsUsedMap.getOrDefault(this, hashSetOf())
            symbolSet.add(symbol)
            info.lastSymbolsUsedMap[this] = symbolSet
            info.symbolUseMap[symbol] = this
        }
    }

    private fun recordWriteSymbol(symbolName: String, node: AstNode? = null): SymbolResult {
        return if (node != null)
            visitChildren(node).apply{ writtenSymbols.add(symbolName) }
        else
            SymbolResult()
    }

    override fun aggregateResult(agg: SymbolResult, next: SymbolResult): SymbolResult {
        agg.readSymbols.addAll(next.readSymbols)
        agg.writtenSymbols.addAll(next.writtenSymbols)
        return agg
    }

    override fun defaultValue() = emptyResult
}