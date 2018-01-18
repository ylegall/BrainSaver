package org.ygl.transformation

import org.ygl.ast.*

/**
 * TODO: handle if-else
 * TODO: record final dead stores
 */
class DeadStoreResolver: AstWalker<Unit>()
{
    private val deadStores = mutableSetOf<AstNode>()
    private val tempStores = mutableMapOf<String, AstNode>()

    fun getDeadStores(ast: AstNode): Set<AstNode> {
        visit(ast)
        return deadStores
    }

    override fun visit(node: ProgramNode) {
        node.children.filterIsInstance<FunctionNode>()
                .forEach { visit(it) }
    }

    override fun visit(node: FunctionNode) {
        tempStores.clear()
        node.statements.forEach { visit(it) }
    }

    override fun visit(node: ArrayConstructorNode) {
        recordSymbolWrite(node, node.array)
    }

    override fun visit(node: ArrayLiteralNode) {
        visit(node.items)
        recordSymbolWrite(node, node.array)
    }

    override fun visit(node: ArrayReadExpNode) {
        visit(node.idx)
        recordSymbolRead(node.array)
    }

    override fun visit(node: ArrayWriteNode) {
        visit(node.rhs)
        visit(node.idx)
        recordSymbolWrite(node, node.array)
    }

    override fun visit(node: AssignmentNode) {
        visit(node.rhs)
        recordSymbolWrite(node, node.lhs)
    }

    override fun visit(node: AtomIdNode) {
        recordSymbolRead(node.identifier)
    }

    override fun visit(node: DeclarationNode) {
        visit(node.rhs)
        recordSymbolWrite(node, node.lhs)
    }

    private fun recordSymbolWrite(node: AstNode, name: String) {
        val oldStore = tempStores.put(name, node)
        oldStore?.let { deadStores.add(it) }
    }

    private fun recordSymbolRead(name: String) {
        tempStores.remove(name)
    }

    override fun defaultValue(node: AstNode) = Unit
}

///**
// */
//class SymbolInfo(
//        val modifiedSymbols: MutableSet<String> = mutableSetOf(),
//        val deadStores: MutableMap<String, MutableSet<AstNode>> = mutableMapOf()
//) {
//    fun isDeadStore(node: StoreNode): Boolean {
//        return deadStores[node.lhs]?.contains(node) == true
//    }
//}
//
//private typealias ModifiedSymbols = MutableSet<String>
//
///**
// *
// */
//internal class MutabilityResolver : AstWalker<ModifiedSymbols>()
//{
//    private val scopeContext = Runtime()
//    private val scopeSymbolInfo = mutableMapOf<AstNode, SymbolInfo>()
//    private val tempStores = mutableMapOf<AstNode, MutableMap<String, AstNode>>()
//
//    fun getSymbolMutabilityInfo(ast: AstNode): Map<AstNode, SymbolInfo> {
//        visit(ast)
//        return scopeSymbolInfo
//    }
//
//    override fun visit(node: FunctionNode): ModifiedSymbols {
//        return visitScope(node)
//    }
//
//    override fun visit(node: IfStatementNode): ModifiedSymbols {
//        visit(node.condition)
//
//        val symbolInfo = enterScope(node)
//
//        val result1 = visit(node.trueStatements)
//        symbolInfo.modifiedSymbols.addAll(result1)
//        tempStores[node]!!.clear()
//
//        val result2 = visit(node.falseStatements)
//        symbolInfo.modifiedSymbols.addAll(result2)
//        tempStores[node]!!.clear()
//
//        exitScope(node)
//
//        return aggregateResult(result1, result2)
//    }
//
//    override fun visit(node: ForStatementNode): ModifiedSymbols {
//        val children = mutableListOf<AstNode>(node.start, node.start, node.inc)
//        children.addAll(node.statements)
//        return visitScope(node, children)
//    }
//
//    override fun visit(node: WhileStatementNode): ModifiedSymbols {
//        val children = mutableListOf<AstNode>(node.condition)
//        children.addAll(node.statements)
//        children.add(node.condition)
//        return visitScope(node, children)
//    }
//
//    private fun visitScope(node: AstNode, children: Iterable<AstNode> = node.children): ModifiedSymbols {
//        val symbolInfo = enterScope(node)
//
//        val result = visit(children)
//        symbolInfo.modifiedSymbols.addAll(result)
//
//        // find any remaining writes and record dead stores:
//        tempStores[node]!!.entries.forEach {
//            symbolInfo.deadStores.getOrPut(it.key, { mutableSetOf() }).add(it.value)
//        }
//
//        exitScope(node)
//        return result
//    }
//
//    override fun visit(node: DeclarationNode): ModifiedSymbols {
//        scopeContext.createSymbol(node.lhs, node.storage)
//        val result = visit(node.rhs)
//        recordSymbolWrite(node, node.lhs)
//        return result
//    }
//
//    override fun visit(node: AssignmentNode): ModifiedSymbols {
//        val result = visit(node.rhs)
//        result.add(node.lhs)
//        recordSymbolWrite(node, node.lhs)
//        return result
//    }
//
//    override fun visit(node: ArrayConstructorNode): ModifiedSymbols {
//        recordSymbolWrite(node, node.array)
//        return mutableSetOf(node.array)
//    }
//
//    override fun visit(node: ArrayLiteralNode): ModifiedSymbols {
//        val result = visit(node.items)
//        result.add(node.array)
//        recordSymbolWrite(node, node.array)
//        return result
//    }
//
//    override fun visit(node: ArrayReadExpNode): ModifiedSymbols {
//        recordSymbolRead(node.array)
//        return visit(node.idx)
//    }
//
//    override fun visit(node: ArrayWriteNode): ModifiedSymbols {
//        val result = visit(node.rhs)
//        result.add(node.array)
//        recordSymbolWrite(node, node.array)
//        return result
//    }
//
//    override fun visit(node: AtomIdNode): ModifiedSymbols {
//        recordSymbolRead(node.identifier)
//        return mutableSetOf()
//    }
//
//    private fun recordSymbolWrite(node: AstNode, name: String) {
//        val scope = scopeContext.findScopeWithSymbol(name)
//        if (scope != null) {
//            val deadStores = scopeSymbolInfo[scope.node]!!.deadStores
//            val tempStores = tempStores[scope.node]!!
//
//            // any existing entry for this variable is a dead store (write without a read):
//            val previousWrite = tempStores[name]
//            previousWrite?.let { deadStores.getOrPut(name, { mutableSetOf() }).add(it) }
//            tempStores[name] = node
//        }
//    }
//
//    private fun recordSymbolRead(name: String) {
//        val scope = scopeContext.findScopeWithSymbol(name)
//        if (scope != null) {
//            tempStores[scope.node]!!.remove(name)
//        }
//    }
//
//    private fun enterScope(node: AstNode): SymbolInfo {
//        scopeContext.enterScope(node)
//        val symbolInfo = SymbolInfo()
//        scopeSymbolInfo[node] = symbolInfo
//        tempStores[node] = mutableMapOf()
//        return symbolInfo
//    }
//
//    private fun exitScope(node: AstNode) {
//        scopeContext.exitScope()
//        tempStores.remove(node)
//    }
//
//    override fun aggregateResult(agg: ModifiedSymbols, next: ModifiedSymbols): ModifiedSymbols {
//        agg.addAll(next)
//        return agg
//    }
//
//    override fun defaultValue(node: AstNode) = mutableSetOf<String>()
//}
