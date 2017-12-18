package org.ygl.analysis

import org.ygl.CompilationException
import org.ygl.ast.*
import org.ygl.model.StorageType

/**
 * TODO: function evaluation
 * TODO: local constants
 * TODO: transitive closure
 */
class ConstantResolver : AstWalker<AstNode>() {

    private val emptyValue = AstNode()
    private val constants = mutableMapOf<String, AstNode>()
    private val globals = mutableSetOf<String>()

    fun resolveConstants(tree: AstNode): Map<String, AstNode> {
        visit(tree)
        return constants
    }

    override fun visit(node: ConstantNode): AstNode {
        val rhs = visit(node.rhs)
        if (node.lhs in constants || node.lhs in globals) {
            throw CompilationException("${node.lhs} redefined")
        }
        if (rhs !is AtomIntNode && rhs !is AtomStrNode) {
            throw CompilationException("${node.lhs} cannot be evaluated at compile time")
        }
        constants.put(node.lhs, rhs)
        return emptyValue
    }

    override fun visit(node: GlobalVariableNode): AstNode {
        if (node.lhs in constants) {
            throw CompilationException("${node.lhs} redefined")
        }
        if (node.storage == StorageType.VAL) {
            val rhs = visit(node.rhs)
            if (rhs !is AtomIntNode && rhs !is AtomStrNode) {
                globals.add(node.lhs)
            } else {
                constants.put(node.lhs, rhs)
            }
        } else {
            globals.add(node.lhs)
        }
        return emptyValue
    }

    override fun visit(node: NotExpNode): AstNode {
        val result = visit(node.right)
        return if (result is AtomIntNode) {
            AtomIntNode(if (result.value == 0) 1 else 0)
        } else {
            throw CompilationException("unsupported negation operation: $result")
        }
    }

    override fun visit(node: BinaryExpNode): AstNode {
        val left = visit(node.left)
        val right = visit(node.right)

        return when (left) {
            is AtomStrNode -> when (right) {
                is AtomStrNode -> AtomStrNode(left.value + right.value)
                is AtomIntNode -> AtomStrNode(left.value + right.value.toString())
                else -> emptyValue
            }
            is AtomIntNode -> when (right) {
                is AtomStrNode -> AtomStrNode(left.value.toString() + right.value)
                is AtomIntNode -> AtomIntNode(computeResult(node.op, left.value, right.value))
                else -> emptyValue
            }
            else -> emptyValue
        }
    }

    private fun computeResult(op: String, left: Int, right: Int): Int {
        return when (op) {
            "+" -> left + right
            "-" -> left - right
            "*" -> left * right
            "/" -> left / right
            "%" -> left % right
            "==" -> if (left == right) 1 else 0
            "!=" -> if (left == right) 0 else 1
            ">=" -> if (left >= right) 1 else 0
            "<=" -> if (left <= right) 1 else 0
            ">" -> if (left > right) 1 else 0
            "<" -> if (left < right) 1 else 0
            "&&" -> if (left != 0 && right != 0) 1 else 0
            "||" -> if (left != 0 || right != 0) 1 else 0
            else -> throw CompilationException("unsupported operator: $op")
        }
    }

    override fun visit(node: AtomIdNode): AstNode {
        return constants.getOrDefault(node.identifier, emptyValue)
    }

    override fun visit(node: AtomIntNode) = node
    override fun visit(node: AtomStrNode) = node

    override fun aggregateResult(agg: AstNode, next: AstNode): AstNode {
        return defaultValue()
    }

    override fun defaultValue(): AstNode {
        return emptyValue
    }
}