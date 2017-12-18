package org.ygl.ast

import org.ygl.model.StorageType
import kotlin.reflect.KClass

open class AstNode(
        val type: KClass<out AstNode> = AstNode::class,
        var children: List<AstNode> = emptyList()
)

class ConstantNode(
        val lhs: String,
        val rhs: AstNode
) : AstNode(ConstantNode::class, mutableListOf(rhs)) {
    override fun toString() = lhs + rhs.toString()
}

class GlobalVariableNode(
        val storage: StorageType,
        val lhs: String,
        val rhs: AstNode
) : AstNode(GlobalVariableNode::class, mutableListOf(rhs))

class FunctionNode(
        val name: String,
        val params: List<String>,
        val statements: MutableList<AstNode>
) : AstNode(FunctionNode::class, statements)

open class StatementNode(
        type: KClass<out StatementNode>,
        list: List<AstNode> = emptyList()
) : AstNode(type, list)

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
        val params: MutableList<ExpNode>
) : StatementNode(CallStatementNode::class, params)

class DebugStatementNode(
        val params: List<String>
) : AstNode(DebugStatementNode::class)

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
        val value: AstNode
) : StatementNode(ArrayWriteNode::class, mutableListOf(idx, value))

class ReadStatementNode(
        val name: String
) : StatementNode(ReadStatementNode::class)

class PrintStatementNode(
        val exp: AstNode
) : StatementNode(PrintStatementNode::class, mutableListOf(exp))

class DeclarationNode(
        val stoarge: StorageType,
        val lhs: String,
        var rhs: AstNode
) : StatementNode(DeclarationNode::class, mutableListOf(rhs))

class AssignmentNode(
        val lhs: String,
        var rhs: AstNode
) : StatementNode(AssignmentNode::class, mutableListOf(rhs)) {
    override fun toString() = lhs + rhs.toString()
}

open class ExpNode(
        type: KClass<out ExpNode>,
        list: List<AstNode> = emptyList()
) : AstNode(type, list)

class CallExpNode(
        val name: String,
        val params: MutableList<ExpNode>
) : ExpNode(CallExpNode::class, params)

class BinaryExpNode(
        val op: String,
        val left: AstNode,
        val right: AstNode
) : ExpNode(BinaryExpNode::class, mutableListOf(left, right))

class NotExpNode(
        val right: AstNode
) : ExpNode(NotExpNode::class, mutableListOf(right))

class ArrayReadExpNode(
        val array: String,
        val idx: AstNode
) : ExpNode(ArrayReadExpNode::class, mutableListOf(idx))

open class AtomNode(
        type: KClass<out AtomNode>
) : AstNode(type)

class AtomIntNode(
        val value: Int
) : AtomNode(AtomIntNode::class) {
    override fun toString() = value.toString()
}

class AtomIdNode(
        val identifier: String
) : AtomNode(AtomIdNode::class) {
    override fun toString() = identifier
}

class AtomStrNode(
        val value: String
) : AtomNode(AtomStrNode::class) {
    override fun toString() = value
}
