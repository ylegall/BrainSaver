package org.ygl.transformation

import org.ygl.CompileException
import org.ygl.ast.*
import org.ygl.model.Op

// TODO mod
fun strengthReduce(op: Op, left: AstNode, right: Int): AstNode {
    val result = when (op) {
        Op.ADD -> addReduce(left, right)
        Op.SUB -> subReduce(left, right)
        Op.MUL -> multReduce(left, right)
        Op.DIV -> divReduce(left, right)
        Op.MOD -> modReduce(left, right)
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

fun strengthReduce(op: Op, left: Int, right: AstNode): AstNode {
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

fun multReduce(left: AstNode, right: Int): AstNode {
    return when (right) {
        0 -> AtomIntNode(0)
        1 -> left
        2 -> BinaryExpNode(Op.ADD, left, left)
        else -> EmptyNode
    }
}

fun divReduce(left: AstNode, right: Int): AstNode {
    return when (right) {
        0 -> throw CompileException("divide by 0")
        1 -> left
        else -> EmptyNode
    }
}

fun modReduce(left: AstNode, right: Int): AstNode {
    return when (right) {
        0 -> throw CompileException("divide by 0")
        1 -> AtomIntNode(0)
        else -> EmptyNode
    }
}

fun subReduce(left: AstNode, right: Int): AstNode {
    return addReduce(left, right)
}

fun addReduce(left: AstNode, right: Int): AstNode {
    return when (right) {
        0 -> left
        else -> CallExpNode("inc", mutableListOf(left, AtomIntNode(right)))
    }
}



