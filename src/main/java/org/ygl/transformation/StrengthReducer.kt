package org.ygl.transformation

import org.ygl.CompileException
import org.ygl.ast.*
import org.ygl.model.Op

class StrengthReducer: AstTransformer()
{
    override fun visit(node: BinaryExpNode): AstNode {
        return if (node.left.isConstant() && node.left is AtomIntNode) {
            strengthReduce(node.op, node.left.value, node.right)
        } else if (node.right.isConstant() && node.right is AtomIntNode) {
            strengthReduce(node.op, node.left, node.right.value)
        } else if (node.right is AtomIdNode && node.left is AtomIdNode){
            if (node.right.identifier == node.left.identifier) {
                when (node.op) {
                    Op.SUB -> AtomIntNode(0)
                    Op.DIV -> AtomIntNode(1)
                    Op.MOD -> AtomIntNode(0)
                    else -> node
                }
            } else {
                node
            }
        } else {
            node
        }
    }

    private fun strengthReduce(op: Op, left: AstNode, right: Int): AstNode {
        val result = when (op) {
            Op.ADD -> addReduce(left, right)
            Op.SUB -> subReduce(left, right)
            Op.MUL -> multReduce(left, right)
            Op.DIV -> divReduce(left, right)
            Op.MOD -> modReduce(right)
            Op.AND -> if (right == 0) AtomIntNode(0) else EmptyNode
            Op.OR  -> if (right != 0) AtomIntNode(1) else EmptyNode
            else -> EmptyNode
        }
        return if (result == EmptyNode) {
            BinaryExpNode(op, left, AtomIntNode(right))
        } else {
            result
        }
    }

    private fun strengthReduce(op: Op, left: Int, right: AstNode): AstNode {
        val result = when (op) {
            Op.ADD -> addReduce(right, left)
            Op.MUL -> multReduce(right, left)
            Op.AND -> if (left == 0) AtomIntNode(0) else EmptyNode
            Op.OR  -> if (left != 0) AtomIntNode(1) else EmptyNode
            else -> EmptyNode
        }
        return if (result == EmptyNode) {
            BinaryExpNode(op, AtomIntNode(left), right)
        } else {
            result
        }
    }

    private fun multReduce(left: AstNode, right: Int): AstNode {
        return when (right) {
            0 -> AtomIntNode(0)
            1 -> left
            2 -> BinaryExpNode(Op.ADD, left, left)
            else -> EmptyNode
        }
    }

    private fun divReduce(left: AstNode, right: Int): AstNode {
        return when (right) {
            0 -> throw CompileException("divide by 0")
            1 -> left
            else -> EmptyNode
        }
    }

    private fun modReduce(right: Int): AstNode {
        return when (right) {
            0 -> throw CompileException("divide by 0")
            1 -> AtomIntNode(0)
            else -> EmptyNode
        }
    }

    private fun subReduce(left: AstNode, right: Int): AstNode {
        return addReduce(left, right)
    }

    private fun addReduce(left: AstNode, right: Int): AstNode {
        return when (right) {
            0 -> left
            else -> EmptyNode
        }
    }

}
