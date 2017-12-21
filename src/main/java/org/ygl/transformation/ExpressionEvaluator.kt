package org.ygl.transformation

import org.ygl.CompileException
import org.ygl.ast.*

/**
 *
 */
class ExpressionEvaluator : AstWalker<AstNode>()
{
    // TODO: use a shared class for resolving scope symbols?
    private var symbols: Map<String, AstNode> = mutableMapOf()

    fun evaluate(node: AstNode, symbols: Map<String, AstNode>): AstNode {
        this.symbols = symbols
        val result = visit(node)
        return if (result == emptyNode) {
            node
        } else {
            result
        }
    }

    override fun visit(node: NotExpNode): AstNode {
        val result = visit(node.right)
        return if (result is AtomIntNode) {
            AtomIntNode(evaluateUnaryOp("!", result.value))
        } else {
            throw CompileException("unsupported negation operation: $result")
        }
    }

    override fun visit(node: BinaryExpNode): AstNode {
        val left = visit(node.left)
        val right = visit(node.right)

        return when (left) {
            is AtomStrNode -> when (right) {
                is AtomStrNode -> AtomStrNode(left.value + right.value)
                is AtomIntNode -> AtomStrNode(left.value + right.value.toString())
                else -> emptyNode
            }
            is AtomIntNode -> when (right) {
                is AtomStrNode -> AtomStrNode(left.value.toString() + right.value)
                is AtomIntNode -> AtomIntNode(evaluateBinaryOp(node.op, left.value, right.value))
                else -> emptyNode
            }
            else -> emptyNode
        }
    }

    fun evaluateUnaryOp(op: String, right: Int): Int {
        return when (op) {
            "!" -> if (right == 0) 1 else 0
            else -> throw CompileException("unsupported unary operator: $op")
        }
    }

    fun evaluateBinaryOp(op: String, left: Int, right: Int): Int {
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
            else -> throw CompileException("unsupported operator: $op")
        }
    }

    override fun visit(node: AtomIdNode): AstNode {
        return symbols.getOrDefault(node.identifier, emptyNode)
    }

    override fun visit(node: AtomIntNode) = node
    override fun visit(node: AtomStrNode) = node

    override fun defaultValue(): AstNode {
        return emptyNode
    }
}