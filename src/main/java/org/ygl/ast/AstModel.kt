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
    //open fun getValue(): Any = Unit
    open val value: Any = Unit
    open val intValue = 0
    open val isConstant = false
}

object EmptyNode : AstNode() {
    override fun toString() = "(?)"
}

class ProgramNode(children: MutableList<AstNode>) : AstNode(children)

class ConstantNode(
        val lhs: String,
        val rhs: AstNode,
        sourceInfo: SourceInfo? = null
) : AstNode(mutableListOf(rhs), sourceInfo) {
    override fun toString() = "const $lhs ="
}

class GlobalVariableNode(
        val lhs: String,
        val rhs: AstNode,
        sourceInfo: SourceInfo? = null
) : AstNode(mutableListOf(rhs), sourceInfo) {
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
        children: MutableList<AstNode> = mutableListOf(),
        sourceInfo: SourceInfo? = null
) : AstNode(children, sourceInfo) {
    override fun toString() = "(stmt)"
//    override fun toString() = children[0].toString()
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
        val params: MutableList<AstNode>,
        sourceInfo: SourceInfo? = null
) : StatementNode(params, sourceInfo) {
    override fun toString() = "call $name()"
}

class DebugStatementNode(
        val params: List<String>
) : StatementNode()

open class StoreNode(
        val lhs: String,
        val rhs: AstNode,
        sourceInfo: SourceInfo? = null
) : StatementNode(mutableListOf(rhs), sourceInfo) {
    override fun toString() = "$lhs ="
}

class ArrayLiteralNode(
        val array: String,
        val storage: StorageType,
        val items: MutableList<AstNode>
) : StatementNode(items)

class ArrayConstructorNode(
        val array: String,
        val storage: StorageType,
        val size: Int
) : StatementNode()

// TODO: make this a store node
class ArrayWriteNode(
        val array: String,
        val idx: AstNode,
        val rhs: AstNode
) : StatementNode(mutableListOf(idx, rhs))

class DeclarationNode(
        val storage: StorageType,
        lhs: String,
        rhs: AstNode,
        sourceInfo: SourceInfo? = null
) : StoreNode(lhs, rhs, sourceInfo) {
    override fun toString() = "$storage $lhs ="
}

class AssignmentNode(
        lhs: String,
        rhs: AstNode,
        sourceInfo: SourceInfo? = null
) : StoreNode(lhs, rhs, sourceInfo) {
    override fun toString() = "$lhs ="
}

open class ExpNode(
        list: MutableList<AstNode> = mutableListOf(),
        sourceInfo: SourceInfo? = null
) : AstNode(list, sourceInfo)

class CallExpNode(
        val name: String,
        val params: MutableList<AstNode>,
        sourceInfo: SourceInfo? = null
) : ExpNode(params, sourceInfo) {
    override fun toString() = "call $name()"
}

class ConditionExpNode(
        val condition: AstNode,
        val trueExp: AstNode,
        val falseExp: AstNode
): ExpNode(mutableListOf(condition, trueExp, falseExp)) {
    override fun toString() = "condition exp"
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

open class AtomNode(
        sourceInfo: SourceInfo? = null
) : ExpNode(sourceInfo = sourceInfo)

data class AtomIntNode(
        override val value: Int
) : AtomNode() {
    override fun toString() = "($value)"
    override val isConstant = true
    override val intValue = value
}

class AtomIdNode(
        val identifier: String,
        sourceInfo: SourceInfo? = null
) : AtomNode(sourceInfo) {
    override fun toString() = "($identifier)"
    override val isConstant = true
}

data class AtomStrNode(
        override val value: String
) : AtomNode() {
    override fun toString() = "($value)"
    override val isConstant = true
    override val intValue = value.length
}