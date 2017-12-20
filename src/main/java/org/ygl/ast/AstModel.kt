package org.ygl.ast

import org.ygl.model.StorageType
import kotlin.reflect.KClass

open class AstNode(
        val type: KClass<out AstNode> = AstNode::class,
        var children: MutableList<out AstNode> = mutableListOf()
) {
    override fun toString() = "(${type.java.simpleName})"
}

class ConstantNode(
        val lhs: String,
        val rhs: AstNode
) : AstNode(ConstantNode::class, mutableListOf(rhs)) {
    override fun toString() = "const $lhs ="
}

class GlobalVariableNode(
        val storage: StorageType,
        val lhs: String,
        val rhs: AstNode
) : AstNode(GlobalVariableNode::class, mutableListOf(rhs)) {
    override fun toString() = "global $lhs ="
}

class FunctionNode(
        val name: String,
        val params: List<String>,
        val statements: MutableList<StatementNode>
) : AstNode(FunctionNode::class, statements) {
    override fun toString() = "fn $name()"
}

open class StatementNode(
        type: KClass<out StatementNode> = StatementNode::class,
        children: MutableList<AstNode> = mutableListOf()
) : AstNode(type, children)

class ReturnNode(
        val exp: AstNode
) : StatementNode(ReturnNode::class, mutableListOf(exp))

class IfStatementNode(
        val condition: ExpNode,
        val trueStatements: MutableList<AstNode>,
        val falseStatements: MutableList<AstNode>
) : StatementNode(
        IfStatementNode::class,
        mutableListOf<AstNode>(condition).apply {
            addAll(trueStatements)
            addAll(falseStatements)
        }
)

class WhileStatementNode(
        val condition: ExpNode,
        val statements: MutableList<AstNode>
) : StatementNode(
        WhileStatementNode::class,
        mutableListOf<AstNode>(condition).apply {
            addAll(statements)
        }
)

class ForStatementNode(
        val counter: String,
        val start: AtomNode,
        val stop: AtomNode,
        val inc: AtomNode? = null,
        val statements: MutableList<AstNode>
) : StatementNode(ForStatementNode::class, statements)

class CallStatementNode(
        val name: String,
        val params: MutableList<AstNode>
) : StatementNode(CallStatementNode::class, params) {
    override fun toString() = "call $name()"
}

class DebugStatementNode(
        val params: List<String>
) : StatementNode(DebugStatementNode::class)

class ArrayLiteralNode(
        val array: String,
        val items: List<Int>
) : StatementNode(ArrayLiteralNode::class)

class ArrayConstructorNode(
        val array: String,
        val size: Int
) : StatementNode(ArrayConstructorNode::class)

class ArrayWriteNode(
        val array: String,
        val idx: AstNode,
        val rhs: AstNode
) : StatementNode(ArrayWriteNode::class, mutableListOf(idx, rhs))

class ReadStatementNode(
        val name: String
) : StatementNode(ReadStatementNode::class)

class PrintStatementNode(
        val exp: AstNode
) : StatementNode(PrintStatementNode::class, mutableListOf(exp))

class DeclarationNode(
        val storage: StorageType,
        val lhs: String,
        var rhs: AstNode
) : StatementNode(DeclarationNode::class, mutableListOf(rhs)) {
    override fun toString() = "$storage $lhs ="
}

class AssignmentNode(
        val lhs: String,
        var rhs: AstNode
) : StatementNode(AssignmentNode::class, mutableListOf(rhs)) {
    override fun toString() = "$lhs ="
}

open class ExpNode(
        type: KClass<out ExpNode>,
        list: MutableList<AstNode> = mutableListOf()
) : AstNode(type, list)

class CallExpNode(
        val name: String,
        val params: MutableList<AstNode>
) : ExpNode(CallExpNode::class, params)

class BinaryExpNode(
        val op: String,
        val left: AstNode,
        val right: AstNode
) : ExpNode(BinaryExpNode::class, mutableListOf(left, right)) {
    override fun toString() = "exp($op)"
}

class NotExpNode(
        val right: AstNode
) : ExpNode(NotExpNode::class, mutableListOf(right))

class ArrayReadExpNode(
        val array: String,
        val idx: AstNode
) : ExpNode(ArrayReadExpNode::class, mutableListOf(idx))

open class AtomNode(
        type: KClass<out AtomNode>
) : ExpNode(type)

class AtomIntNode(
        val value: Int
) : AtomNode(AtomIntNode::class) {
    override fun toString() = "($value)"
}

class AtomIdNode(
        val identifier: String
) : AtomNode(AtomIdNode::class) {
    override fun toString() = "($identifier)"
}

class AtomStrNode(
        val value: String
) : AtomNode(AtomStrNode::class) {
    override fun toString() = "($value)"
}
