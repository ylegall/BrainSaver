package org.ygl.transformation

import org.ygl.CompileException
import org.ygl.ast.*
import org.ygl.model.Op

/**
 * TODO: strength reductions
 */
class ExpressionEvaluator : AstWalker<AstNode>()
{
    private var symbols: Map<String, AstNode> = mutableMapOf()

    fun evaluate(node: AstNode, symbols: Map<String, AstNode> = mutableMapOf()): AstNode {
        this.symbols = symbols
        val result = visit(node)
        return if (result == EmptyNode) {
            EmptyNode
        } else {
            result
        }
    }

    override fun visit(node: ArrayReadExpNode): AstNode {
        val idx = visit(node.idx)
        return if (idx is AtomIntNode) {
            val key = "${node.array}[${idx.value}]"
            symbols.getOrDefault(key, EmptyNode)
        } else {
            EmptyNode
        }
    }

    override fun visit(node: ConditionExpNode): AstNode {
        val condition = visit(node.condition)
        return when (condition) {
            is AtomIntNode -> when (condition.value) {
                0 -> visit(node.falseExp)
                else -> visit(node.trueExp)
            }
            else -> EmptyNode
        }
    }

    override fun visit(node: NotExpNode): AstNode {
        val result = visit(node.right)
        return if (result is AtomIntNode) {
            AtomIntNode(evalUnaryExpression(Op.NOT, result.value))
        } else {
            EmptyNode
            //throw CompileException("unsupported negation operation: $result")
        }
    }

    override fun visit(node: BinaryExpNode): AstNode {
        val left = visit(node.left)
        val right = visit(node.right)
        return evalConstantBinaryExp(node.op, left, right)
    }

    private fun evalConstantBinaryExp(op: Op, left: AstNode, right: AstNode): AstNode {
        return when (left) {
            is AtomStrNode -> when (right) {
                is AtomStrNode -> AtomStrNode(evalStringExpression(op, left.value, right.value))
                is AtomIntNode -> AtomStrNode(evalStringExpression(op, left.value, right.value.toString()))
                else -> EmptyNode
            }
            is AtomIntNode -> when (right) {
                is AtomStrNode -> AtomStrNode(evalStringExpression(op, left.value.toString(), right.value))
                is AtomIntNode -> AtomIntNode(evalIntExpression(op, left.value, right.value))
                else -> EmptyNode
            }
            else -> EmptyNode
        }
    }

    private fun evalStringExpression(op: Op, left: String, right: String): String {
        return when (op) {
            Op.ADD -> left + right
            else -> throw CompileException("unsupported operator: $op")
        }
    }

    private fun evalUnaryExpression(op: Op, right: Int): Int {
        return when (op) {
            Op.NOT -> if (right == 0) 1 else 0
            else -> throw CompileException("unsupported unary operator: $op")
        }
    }

    private fun evalIntExpression(op: Op, left: Int, right: Int): Int {
        return when (op) {
            Op.ADD -> left + right
            Op.SUB -> left - right
            Op.MUL -> left * right
            Op.DIV -> left / right
            Op.MOD -> left % right
            Op.EQ -> if (left == right) 1 else 0
            Op.NEQ -> if (left == right) 0 else 1
            Op.GEQ -> if (left >= right) 1 else 0
            Op.LEQ -> if (left <= right) 1 else 0
            Op.GT -> if (left > right) 1 else 0
            Op.LT -> if (left < right) 1 else 0
            Op.AND -> if (left != 0 && right != 0) 1 else 0
            Op.OR -> if (left != 0 || right != 0) 1 else 0
            else -> throw CompileException("unsupported operator: $op")
        }
    }

    // TODO: constant function evaluation
    override fun visit(node: CallExpNode) = EmptyNode

    override fun visit(node: AtomIdNode): AstNode {
        return symbols.getOrDefault(node.identifier, EmptyNode)
    }

    override fun visit(node: AtomIntNode) = node
    override fun visit(node: AtomStrNode) = node
    override fun defaultValue(node: AstNode) = EmptyNode
}