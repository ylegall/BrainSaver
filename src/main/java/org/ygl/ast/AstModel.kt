package org.ygl.ast

import org.ygl.model.Op
import org.ygl.model.StorageType

enum class NodeType {
    ARRAY_CTOR,
    ARRAY_LITERAL,
    ARRAY_READ_EXP,
    ARRAY_WRITE,
    ASSIGN,
    ATOM_ID,
    ATOM_INT,
    ATOM_STR,
    BINARY_EXP,
    CALL,
    CALL_EXP,
    CONSTANT,
    DECLARE,
    FOR,
    FUNCTION,
    GLOBAL,
    IF,
    IF_EXP,
    NONE,
    PROGRAM,
    RETURN,
    STATEMENT,
    UNARY_EXP,
    WHILE;
}

/**
 *
 */
abstract class AstNode(
        var children: MutableList<AstNode> = mutableListOf(),
        var sourceInfo: SourceInfo? = null
) {
    abstract val nodeType: NodeType
    fun isConstant() = this.nodeType == NodeType.ATOM_INT || this.nodeType == NodeType.ATOM_STR
    override fun toString() = "(Node)"
}

object EmptyNode : AstNode() {
    override val nodeType = NodeType.NONE
    override fun toString() = "(?)"
}

class ProgramNode(
        children: MutableList<AstNode>
): AstNode(children) {
    override val nodeType = NodeType.PROGRAM
    override fun toString() = "program"
}

class ConstantNode(
        val lhs: String,
        val rhs: AstNode,
        sourceInfo: SourceInfo? = null
) : AstNode(mutableListOf(rhs), sourceInfo) {
    override val nodeType = NodeType.CONSTANT
    override fun toString() = "const $lhs ="
}

class GlobalVariableNode(
        val lhs: String,
        val rhs: AstNode,
        sourceInfo: SourceInfo? = null
) : AstNode(mutableListOf(rhs), sourceInfo) {
    override val nodeType = NodeType.GLOBAL
    override fun toString() = "global $lhs ="
}

class FunctionNode(
        val name: String,
        val params: List<String>,
        val statements: MutableList<AstNode>,
        val ret: ReturnNode? = null
) : AstNode(
        mutableListOf<AstNode>().apply {
            addAll(statements)
            ret?.let { add(it) }
        }
)
{
    override val nodeType = NodeType.FUNCTION
    override fun toString() = "fn $name()"
}

class StatementNode(
        children: MutableList<AstNode> = mutableListOf(),
        sourceInfo: SourceInfo? = null
) : AstNode(children, sourceInfo) {
    override val nodeType = NodeType.STATEMENT
    override fun toString() = "(stmt)"
//    override fun toString() = "stmt: " + children[0].toString()
}

class ReturnNode(
        val rhs: AstNode
) : AstNode(mutableListOf(rhs)) {
    override val nodeType = NodeType.RETURN
    override fun toString() = "return"
}

class IfStatementNode(
        val condition: AstNode,
        val trueStatements: MutableList<AstNode>,
        val falseStatements: MutableList<AstNode>
) : AstNode(
        mutableListOf(condition).apply {
            addAll(trueStatements)
            addAll(falseStatements)
        }
) {
    override val nodeType = NodeType.IF
    override fun toString() = "if"
}

class WhileStatementNode(
        val condition: AstNode,
        val statements: MutableList<AstNode>
) : AstNode(
        mutableListOf(condition).apply {
            addAll(statements)
        }
) {
    override val nodeType = NodeType.WHILE
    override fun toString() = "while"
}

class ForStatementNode(
        val counter: String,
        val start: AstNode,
        val stop: AstNode,
        val inc: AstNode,
        val statements: MutableList<AstNode>
) : AstNode(
        mutableListOf(start, stop, inc).apply {
            addAll(statements)
        }
) {
    override val nodeType = NodeType.FOR
    override fun toString() = "for ($counter in $start to $stop by $inc)"
}

class CallStatementNode(
        val name: String,
        val params: MutableList<AstNode>,
        sourceInfo: SourceInfo? = null
) : AstNode(params, sourceInfo) {
    override val nodeType = NodeType.CALL
    override fun toString() = "call $name()"
}

class ArrayLiteralNode(
        val array: String,
        val storage: StorageType,
        val items: MutableList<AstNode>
) : AstNode(items) {
    override val nodeType = NodeType.ARRAY_LITERAL
}

class ArrayConstructorNode(
        val array: String,
        val storage: StorageType,
        val size: Int
) : AstNode() {
    override val nodeType = NodeType.ARRAY_CTOR
    override fun toString() = "$array = array($size)"
}

class ArrayWriteNode(
        val array: String,
        val idx: AstNode,
        val rhs: AstNode
) : AstNode(mutableListOf(idx, rhs)) {
    override val nodeType = NodeType.ARRAY_WRITE
    override fun toString() = "$array[] = "
}

class DeclarationNode(
        val storage: StorageType,
        val lhs: String,
        val rhs: AstNode,
        sourceInfo: SourceInfo? = null
) : AstNode(
        mutableListOf(rhs),
        sourceInfo
) {
    override val nodeType = NodeType.DECLARE
    override fun toString() = "$storage $lhs ="
}

class AssignmentNode(
        val lhs: String,
        val rhs: AstNode,
        sourceInfo: SourceInfo? = null
) : AstNode(
        mutableListOf(rhs),
        sourceInfo
) {
    override val nodeType = NodeType.ASSIGN
    override fun toString() = "$lhs ="
}

class CallExpNode(
        val name: String,
        val params: MutableList<AstNode>,
        sourceInfo: SourceInfo? = null
) : AstNode(params, sourceInfo) {
    override val nodeType = NodeType.CALL_EXP
    override fun toString() = "call $name()"
}

class ConditionExpNode(
        val condition: AstNode,
        val trueExp: AstNode,
        val falseExp: AstNode
): AstNode(mutableListOf(condition, trueExp, falseExp)) {
    override val nodeType = NodeType.IF_EXP
    override fun toString() = "condition exp"
}

class BinaryExpNode(
        val op: Op,
        val left: AstNode,
        val right: AstNode
) : AstNode(mutableListOf(left, right)) {
    override val nodeType = NodeType.BINARY_EXP
    override fun toString() = "exp($op)"
}

// TODO: rename to unary
class NotExpNode(
        val right: AstNode
) : AstNode(mutableListOf(right)) {
    override val nodeType = NodeType.UNARY_EXP
}

class ArrayReadExpNode(
        val array: String,
        val idx: AstNode
) : AstNode(mutableListOf(idx)) {
    override val nodeType = NodeType.ARRAY_READ_EXP
}

data class AtomIntNode(
        val value: Int
) : AstNode() {
    override val nodeType = NodeType.ATOM_INT
    override fun toString() = "($value)"
}

class AtomIdNode(
        val identifier: String,
        sourceInfo: SourceInfo? = null
) : AstNode(sourceInfo = sourceInfo) {
    override val nodeType = NodeType.ATOM_ID
    override fun toString() = "($identifier)"
}

data class AtomStrNode(
        val value: String
) : AstNode() {
    override val nodeType = NodeType.ATOM_STR
    override fun toString() = "($value)"
}
