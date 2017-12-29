package org.ygl.ast

import org.ygl.model.Op
import org.ygl.model.StorageType

/**
 *
 */
open class AstNode(
        var children: MutableList<AstNode> = mutableListOf(),
        var sourceInfo: SourceInfo? = null
) {
    override fun toString() = "(Node)"
}

object EmptyNode : AstNode()

fun AstNode.isConstant(): Boolean {
    return (this is AtomIntNode) || (this is AtomStrNode)
}

class ProgramNode(children: MutableList<AstNode>) : AstNode(children)

class ConstantNode(
        val lhs: String,
        val rhs: AstNode
) : AstNode(mutableListOf(rhs)) {
    override fun toString() = "const $lhs ="
}

class GlobalVariableNode(
        val storage: StorageType,
        val lhs: String,
        val rhs: AstNode
) : AstNode(mutableListOf(rhs)) {
    override fun toString() = "global $lhs ="
}

class FunctionNode(
        val name: String,
        val params: List<String>,
        val statements: MutableList<AstNode>,
        val ret: AstNode? = null
) : AstNode(
        statements.apply { ret?.let { add(it) } }
)
{
    override fun toString() = "fn $name()"
}

open class StatementNode(
        children: MutableList<AstNode> = mutableListOf()
) : AstNode(children) {
    override fun toString() = "(stmt)"
}

class ReturnNode(
        val exp: AstNode
) : StatementNode(mutableListOf(exp)) {
    override fun toString() = "ret"
}

class IfStatementNode(
        val condition: ExpNode,
        val trueStatements: MutableList<AstNode>,
        val falseStatements: MutableList<AstNode>
) : StatementNode(
        mutableListOf<AstNode>(condition).apply {
            addAll(trueStatements)
            addAll(falseStatements)
        }
) {
    override fun toString() = "if"
}

class WhileStatementNode(
        val condition: ExpNode,
        val statements: MutableList<AstNode>
) : StatementNode(
        mutableListOf<AstNode>(condition).apply {
            addAll(statements)
        }
) {
    override fun toString() = "while"
}

class ForStatementNode(
        val counter: String,
        val start: AtomNode,
        val stop: AtomNode,
        val inc: AtomNode,
        val statements: MutableList<AstNode>
) : StatementNode(
        mutableListOf<AstNode>(start, stop, inc).apply {
            addAll(statements)
        }
) {
    override fun toString() = "for ($counter in $start to $stop by $inc)"
}

class CallStatementNode(
        val name: String,
        val params: MutableList<AstNode>
) : StatementNode(params) {
    override fun toString() = "call $name()"
}

class DebugStatementNode(
        val params: List<String>
) : StatementNode()

class ArrayLiteralNode(
        val array: String,
        val items: MutableList<AstNode>
) : StatementNode(items)

class ArrayConstructorNode(
        val array: String,
        val size: Int
) : StatementNode()

class ArrayWriteNode(
        val array: String,
        val idx: AstNode,
        val rhs: AstNode
) : StatementNode(mutableListOf(idx, rhs))

open class StoreNode(
        val lhs: String,
        val rhs: AstNode
) : StatementNode(mutableListOf(rhs))

class DeclarationNode(
        val storage: StorageType,
        lhs: String,
        rhs: AstNode
) : StoreNode(lhs, rhs) {
    override fun toString() = "$storage $lhs ="
}

class AssignmentNode(
        lhs: String,
        rhs: AstNode
) : StoreNode(lhs, rhs) {
    override fun toString() = "$lhs ="
}

open class ExpNode(
        list: MutableList<AstNode> = mutableListOf()
) : AstNode(list)

class CallExpNode(
        val name: String,
        val params: MutableList<AstNode>
) : ExpNode(params) {
    override fun toString() = "call $name()"
}

class BinaryExpNode(
        val op: Op,
        val left: AstNode,
        val right: AstNode
) : ExpNode(mutableListOf(left, right)) {
    override fun toString() = "exp($op)"
}

class NotExpNode(
        val right: AstNode
) : ExpNode(mutableListOf(right))

class ArrayReadExpNode(
        val array: String,
        val idx: AstNode
) : ExpNode(mutableListOf(idx))

open class AtomNode : ExpNode()

class AtomIntNode(
        val value: Int
) : AtomNode() {
    override fun toString() = "($value)"
}

class AtomIdNode(
        val identifier: String
) : AtomNode() {
    override fun toString() = "($identifier)"
}

class AtomStrNode(
        val value: String
) : AtomNode() {
    override fun toString() = "($value)"
}
