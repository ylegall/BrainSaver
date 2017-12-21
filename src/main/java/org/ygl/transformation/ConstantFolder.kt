package org.ygl.transformation

import org.ygl.ast.*
import org.ygl.model.IntValue
import org.ygl.model.StrValue
import org.ygl.model.Value
import org.ygl.runtime.ScopeContext
import org.ygl.runtime.ValuedSymbol

/**
 *
 */
class ConstantFolder(

): AstWalker<AstNode>()
{
    private val scopeSymbols = ScopeContext<ValuedSymbol>()
    // TODO: add expression evaluator

    private fun AstNode.isConstant(): Boolean {
        return (this is AtomIntNode) || (this is AtomStrNode)
    }

    private fun AstNode.getValue(): Value {
        return when (this) {
            is AtomIntNode -> IntValue(this.value)
            is AtomStrNode -> StrValue(this.value)
            else -> throw Exception("not a node with a known value")
        }
    }

    override fun visit(node: AssignmentNode): AstNode {
        val rhs = visit(node.rhs)
        return if (rhs.isConstant()) {
            AssignmentNode(node.lhs, rhs)
        } else {
            node
        }
    }

    // TODO:
    override fun visit(node: AtomIdNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: AtomIntNode) = node
    override fun visit(node: AtomStrNode) = node

    override fun visit(node: BinaryExpNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: CallExpNode) = node

    override fun visit(node: DeclarationNode): AstNode {
        val rhs = visit(node.rhs)
        return if (rhs.isConstant()) {
            val value = rhs.getValue()
            scopeSymbols.addSymbol(ValuedSymbol(node.lhs, node.storage, value))
            DeclarationNode(node.storage, node.lhs, rhs)
        } else {
            node
        }
    }

    override fun visit(node: ForStatementNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: IfStatementNode): AstNode {
        val condition = visit(node.condition)
        if (condition.isConstant()) {
            val intVal = condition.getValue() as? IntValue ?: throw Exception("invalid condition type for $node")
            return if (intVal.value == 0) {
                AstNode(children = node.falseStatements)
            } else {
                AstNode(children = node.trueStatements)
            }
        } else {
            // TODO reverse negated condition

            return node
        }
    }

    override fun visit(node: NotExpNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: WhileStatementNode): AstNode {
        return super.visit(node)
    }

    override fun visitChildren(node: AstNode): AstNode {
        val newChildren = MutableList(node.children.size, { idx -> visit(node.children[idx]) })
        node.children = newChildren.filter { it != emptyNode }.toCollection(mutableListOf())
        return node
    }

    override fun defaultValue(): AstNode {
        return emptyNode
    }
}