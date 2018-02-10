package org.ygl.transformation

import org.ygl.ast.*
import java.util.*

/**
 *
 */
class DeadStoreRemover(
        private val deadStores: Set<AstNode>
): AstTransformer()
{

    override fun visit(node: StatementNode): AstNode {
        return if (node.children.size == 1 && node.children[0] in deadStores) {
            val child = node.children[0]
            // leave declaration nodes so that the symbol is still created in the runtime
            if (child is DeclarationNode) {
                node.children[0] = DeclarationNode(child.storage, child.lhs, EmptyNode, child.sourceInfo)
                node
            } else {
                EmptyNode
            }
        } else {
            node.children = visitList(node.children)
            node
        }
    }
}

/**
 *
 */
class DeadStoreResolver: AstWalker<Unit>()
{
    private val deadStores = mutableSetOf<AstNode>()
    private val tempStores = ArrayDeque<MutableMap<String, AstNode>>()
    private val globals = mutableSetOf<String>()

    fun getDeadStores(ast: AstNode): Set<AstNode> {
        visit(ast)
        return deadStores
    }

    override fun visit(node: ProgramNode) {
        tempStores.push(mutableMapOf())

        node.children.filterIsInstance<GlobalVariableNode>()
                .forEach {
                    globals.add(it.lhs)
                    visit(it)
                }

        node.children.filterIsInstance<FunctionNode>()
                .forEach { visit(it) }

        tempStores.pop()
    }

    override fun visit(node: FunctionNode) {
        wrapInScope {
            node.statements.forEach { visit(it) }
            node.ret?.let { visit(it) }
        }
        deadStores.addAll(tempStores.peek().values)
        tempStores.peek().clear()
    }

    override fun visit(node: ForStatementNode) {
        visit(node.start)
        visit(node.stop)
        visit(node.inc)
        wrapInScope {
            visit(node.statements)
        }
    }

    override fun visit(node: WhileStatementNode) {
        visit(node.condition)

        wrapInScope {
            visit(node.statements)
            visit(node.condition)
        }
    }

    override fun visit(node: IfStatementNode) {
        visit(node.condition)

        tempStores.push(mutableMapOf())
        visit(node.trueStatements)
        val trueStores = tempStores.pop()

        tempStores.push(mutableMapOf())
        visitList(node.falseStatements)
        val falseStores = tempStores.pop()

        trueStores.keys.intersect(falseStores.keys)
                .forEach { recordSymbolWrite(EmptyNode, it) }
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

    override fun visit(node: GlobalVariableNode) {
        visit(node.rhs)
        recordSymbolWrite(node, node.lhs)
    }

    private inline fun wrapInScope(body: () -> Unit) {
        tempStores.push(mutableMapOf())
        body()

        tempStores.pop().filter { it.key !in tempStores.peek() }
                .filter { it.key !in globals }
                .forEach { tempStores.peek()[it.key] = it.value }
    }

    private fun recordSymbolWrite(node: AstNode, name: String) {
        val oldStore = if (node == EmptyNode) {
            tempStores.peek().remove(name)
        } else {
            tempStores.peek().put(name, node)
        }
        oldStore?.let { deadStores.add(it) }
    }

    private fun recordSymbolRead(name: String) {
        tempStores.find { name in it }?.remove(name)
    }

    override fun defaultValue(node: AstNode) = Unit
}
