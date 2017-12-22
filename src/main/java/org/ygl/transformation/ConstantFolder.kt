package org.ygl.transformation

import org.ygl.CompileException
import org.ygl.ast.*
import org.ygl.model.*
import org.ygl.runtime.ScopeContext
import org.ygl.runtime.ValuedSymbol

/**
 *
 */
class ConstantFolder(

): AstWalker<AstNode>()
{
    private val scopeSymbols = ScopeContext<ValuedSymbol>()
    private val unknownSymbol = ValuedSymbol("", StorageType.VAR, NullValue)
    private val expEval = ExpressionEvaluator()

    private fun AstNode.getValue(): Value {
        return when (this) {
            is AtomIntNode -> IntValue(this.value)
            is AtomStrNode -> StrValue(this.value)
            else -> NullValue
        }
    }

    override fun visit(node: ProgramNode): AstNode {
        // add global symbols to scope
        scopeSymbols.enterScope(node)
        node.children.filterIsInstance<GlobalVariableNode>()
                .forEach {
                    val result = visit(it) as GlobalVariableNode
                    scopeSymbols.addSymbol(ValuedSymbol(it.lhs, it.storage, result.rhs.getValue()))
                }

        node.children.filterIsInstance<FunctionNode>().forEach { visit(it) }
        scopeSymbols.exitScope()
        return node
    }

    override fun visit(node: FunctionNode): AstNode {
        scopeSymbols.enterScope(node)
        visitChildren(node)
        scopeSymbols.exitScope()
        return node
    }

    override fun visit(node: GlobalVariableNode): AstNode {
        return GlobalVariableNode(node.storage, node.lhs, visit(node.rhs))
    }

    override fun visit(node: DeclarationNode): AstNode {
        val rhs = visit(node.rhs)
        return if (rhs.isLiteral()) {
            val value = rhs.getValue()
            scopeSymbols.addSymbol(ValuedSymbol(node.lhs, node.storage, value))
            DeclarationNode(node.storage, node.lhs, rhs)
        } else {
            scopeSymbols.addSymbol(ValuedSymbol(node.lhs, node.storage, NullValue))
            node
        }
    }

    override fun visit(node: AssignmentNode): AstNode {
        val rhs = visit(node.rhs)
        val symbol = scopeSymbols.resolveLocalSymbol(node.lhs)
        if (symbol != null) {
            if (symbol.storage == StorageType.VAL) throw CompileException("val cannot be reassigned: ${node.lhs}")
            if (rhs.isLiteral()) {
                scopeSymbols.addSymbol(ValuedSymbol(node.lhs, symbol.storage, rhs.getValue()))
            } else {
                symbol.value = NullValue
            }
        }
        return AssignmentNode(node.lhs, rhs)
    }

    override fun visit(node: AtomIdNode): AstNode {
        val symbol = scopeSymbols.resolveLocalSymbol(node.identifier) ?: unknownSymbol
        val value = symbol.value
        return when (value) {
            is IntValue -> AtomIntNode(value.value)
            is StrValue -> AtomStrNode(value.value)
            else -> node
        }
    }

    // TODO: expression re-writing: +(5, +(x, 3)) => +(x, 8)
    override fun visit(node: BinaryExpNode): AstNode {
        val left = visit(node.left)
        val right = visit(node.right)

        if (left.isLiteral() && right.isLiteral()) {
            return expEval.evalConstantBinaryExp(node.op, left, right)
        }

        if (left is AtomIntNode) {
            return strengthReduce(node.op, left.value, right)
        }

        if (right is AtomIntNode) {
            return strengthReduce(node.op, left, right.value)
        }

        return node
    }

    // TODO
    override fun visit(node: ForStatementNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: IfStatementNode): AstNode {
        val condition = visit(node.condition)
        return if (condition.isLiteral()) {
            val intVal = condition.getValue() as? IntValue ?: throw Exception("invalid condition type for $node")
            if (intVal.value == 0) {
                AstNode(children = node.falseStatements)
            } else {
                AstNode(children = node.trueStatements)
            }
        } else if (condition is NotExpNode) {
            IfStatementNode(condition.right as ExpNode, node.falseStatements, node.trueStatements)
        } else {
            node
        }
    }

    override fun visit(node: NotExpNode): AstNode {
        val exp = visit(node.right)
        return if (exp is AtomIntNode) {
            AtomIntNode(expEval.evalUnaryExpression(Op.NOT, exp.value))
        } else {
            node
        }
    }

    // TODO
    override fun visit(node: WhileStatementNode): AstNode {
        return super.visit(node)
    }

    override fun visitChildren(node: AstNode): AstNode {
        node.children = node.children.map { visit(it) }
                .filterNot { it == EmptyNode }
                .toCollection(mutableListOf())
        return node
    }

    override fun defaultValue(): AstNode {
        return EmptyNode
    }
}