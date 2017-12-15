package org.ygl.ast

import org.antlr.v4.runtime.ParserRuleContext
import org.ygl.BrainSaverParser.*
import kotlin.reflect.KClass

open class AstNode(
        val type: KClass<out ParserRuleContext>,
        var children: List<AstNode> = emptyList()
)

class FunctionNode(
        val name: String,
        val params: List<String>,
        val statements: MutableList<AstNode>
) : AstNode(FunctionContext::class, statements)

class StatementNode : AstNode(StatementContext::class)

class ReturnNode(
        val exp: AstNode
) : AstNode(ReturnStatementContext::class, mutableListOf(exp))

class IfStatementNode(
        val condition: ExpNode,
        val trueStatements: MutableList<AstNode>,
        val falseStatements: MutableList<AstNode>
) : AstNode(
        IfStatementContext::class,
        mutableListOf<AstNode>(condition).apply {
            addAll(trueStatements)
            addAll(falseStatements)
        }
)

class WhileStatementNode(
        val condition: ExpNode,
        val statements: MutableList<AstNode>
) : AstNode(
        WhileStatementContext::class,
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
) : AstNode(WhileStatementContext::class, statements)

class CallStatementNode(
        val name: String,
        val params: MutableList<ExpNode>
) : AstNode(CallStatementContext::class, params)

class DebugStatementNode(
        val params: List<String>
) : AstNode(DebugStatementContext::class)

class ArrayLiteralNode(
        val array: String,
        val items: List<Int>
) : AstNode(ArrayLiteralContext::class)

class ArrayConstructorNode(
        val array: String,
        val size: Int
) : AstNode(ArrayConstructorContext::class)

class ArrayWriteNode(
        val array: String,
        val idx: AstNode,
        val value: AstNode
) : AstNode(ArrayWriteStatementContext::class, mutableListOf(idx, value))

class ReadStatementNode(
        val name: String
) : AstNode(ReadStatementContext::class)

class PrintStatementNode(
        val exp: AstNode
) : AstNode(PrintStatementContext::class, mutableListOf(exp))

class AssignmentNode(
        val op: String,
        val lhs: String,
        val rhs: AstNode
) : AstNode(AssignmentStatementContext::class, mutableListOf(rhs))

open class ExpNode(
        type: KClass<out ExpContext>,
        list: List<AstNode> = emptyList()
) : AstNode(type, list)

class CallExpNode(
        val name: String,
        val params: MutableList<ExpNode>
) : ExpNode(CallExpContext::class, params)

class BinaryExpNode(
        val op: String,
        val left: AstNode,
        val right: AstNode
) : ExpNode(OpExpContext::class, mutableListOf(left, right))

class NotExpNode(
        val right: AstNode
) : ExpNode(NotExpContext::class, mutableListOf(right))

class ArrayReadExpNode(
        val array: String,
        val idx: AstNode
) : ExpNode(ArrayReadExpContext::class, mutableListOf(idx))

open class AtomNode(
        type: KClass<out AtomContext>
) : AstNode(type)

class AtomIntNode(
        val value: Int
) : AtomNode(AtomIntContext::class)

class AtomIdNode(
        val identifier: String
) : AtomNode(AtomIntContext::class)

class AtomStringNode(
        val value: String
) : AtomNode(AtomIntContext::class)
