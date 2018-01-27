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
    private val removedDeclarations = mutableMapOf<String, DeclarationNode>()

    override fun visit(node: StatementNode): AstNode {
        assert(node.children.size == 1)
        val child = node.children[0]
        return if (child in deadStores) {
            if (child is DeclarationNode) {
                removedDeclarations[child.lhs] = child
            }
            EmptyNode
        } else {
            if (child is AssignmentNode && child.lhs in removedDeclarations) {
                val oldDecl = removedDeclarations.remove(child.lhs)!!
                node.children[0] = DeclarationNode(oldDecl.storage, child.lhs, child.rhs)
            } else {
                node.children[0] = visit(node.children[0])
            }
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

    fun getDeadStores(ast: AstNode): Set<AstNode> {
        visit(ast)
        return deadStores
    }

    override fun visit(node: ProgramNode) {
        tempStores.push(mutableMapOf())

        node.children.filterIsInstance<GlobalVariableNode>()
                .forEach { visit(it) }

        node.children.filterIsInstance<FunctionNode>()
                .forEach { visit(it) }

        tempStores.pop()
    }

    override fun visit(node: FunctionNode) {
        wrapInScope {
            node.statements.forEach { visit(it) }
        }
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

    override fun visit(node: GlobalVariableNode) {
        visit(node.rhs)
        recordSymbolWrite(node, node.lhs)
    }

    private inline fun wrapInScope(body: () -> Unit) {
        tempStores.push(mutableMapOf())
        body()
        val stores = tempStores.pop()
        stores.forEach { deadStores.add(it.value) }
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
