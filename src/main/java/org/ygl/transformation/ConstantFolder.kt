package org.ygl.transformation

import org.ygl.CompileException
import org.ygl.ast.*
import org.ygl.model.*
import org.ygl.runtime.*

/**
 */
class ConstantFolder(
        private val scopeInfo: Map<AstNode, SymbolInfo>
): AstWalker<AstNode>()
{
    private val scopeContext = ScopeContext()
    private val expEval = ExpressionEvaluator()

    private fun AstNode.getValue(): Value {
        return when (this) {
            is AtomIntNode -> IntValue(this.value)
            is AtomStrNode -> StrValue(this.value)
            else -> NullValue
        }
    }

    private fun AstNode.getIntValue(): Int {
        return when (this) {
            is AtomIntNode -> this.value
            else -> throw CompileException("$this is not an integer value")
        }
    }

    override fun visit(node: ProgramNode): AstNode {
        // add global symbols to scope
        scopeContext.enterScope(node)
        node.children.filterIsInstance<GlobalVariableNode>()
                .forEach {
                    val result = visit(it) as GlobalVariableNode
                    scopeContext.createSymbol(it.lhs, it.storage, result.rhs.getValue())
                }

        node.children.filterIsInstance<FunctionNode>().forEach { visit(it) }
        scopeContext.exitScope()
        return node
    }

    override fun visit(node: GlobalVariableNode): AstNode {
        return GlobalVariableNode(node.storage, node.lhs, visit(node.rhs))
    }

    override fun visit(node: FunctionNode): AstNode {
        scopeContext.enterScope(node)
        node.params.forEach { scopeContext.createSymbol(it) }
        val stmts = node.statements.map { visit(it) }
                .filter { it != EmptyNode }
                .toCollection(mutableListOf())
        scopeContext.exitScope()
        node.statements.clear()
        node.statements.addAll(stmts)
        return node
    }

    override fun visit(node: StatementNode): AstNode {
        visitChildren(node)
        return if (node.children.isEmpty()) {
            EmptyNode
        } else {
            node
        }
    }

    override fun visit(node: DeclarationNode): AstNode {
        // check for dead local store
        val info = scopeInfo[scopeContext.currentScope().node]!!
        if (info.isDeadStore(node)) {
            return EmptyNode
        }

        val rhs = visit(node.rhs)
        return if (rhs.isConstant()) {
            scopeContext.createSymbol(node.lhs, node.storage, rhs.getValue())
            DeclarationNode(node.storage, node.lhs, rhs)
        } else {
            scopeContext.createSymbol(node.lhs, node.storage)
            node
        }
    }

    override fun visit(node: AssignmentNode): AstNode {
        // check for dead local store:
        val info = scopeInfo[scopeContext.currentScope().node]!!
        if (info.isDeadStore(node)) {
            return EmptyNode
        }

        val rhs = visit(node.rhs)
        val symbol = scopeContext.resolveLocalSymbol(node.lhs)
        if (symbol != null) {
            if (symbol.storage == StorageType.VAL) {
                throw CompileException("val cannot be reassigned: ${node.lhs}")
            } else if (rhs.isConstant()) {
                //scopeContext.createSymbol(node.lhs, symbol.storage, rhs.getValue())
                symbol.value = rhs.getValue()
            } else {
                symbol.value = NullValue
            }
        }
        return AssignmentNode(node.lhs, rhs)
    }

    // TODO: check if value is written in scope context
    override fun visit(node: AtomIdNode): AstNode {
        // check if symbol is modified in current scope
        val info = scopeInfo[scopeContext.currentScope().node]!!
        if (node.identifier in info.modifiedSymbols) {
            return node
        }

        val symbol = scopeContext.resolveSymbol(node.identifier) ?: UnknownSymbol
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

        if (left.isConstant() && right.isConstant()) {
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

    override fun visit(node: ForStatementNode): AstNode {
        scopeContext.enterScope(node)
        val start = visit(node.start)
        val stop = visit(node.stop)
        val inc = visit(node.inc)
        val statements = visitList(node.statements)

        // unroll loop
        if (start.isConstant() && stop.isConstant() && inc.isConstant()) {
            val result = AstNode()
            var i = start.getIntValue()
            val j = stop.getIntValue()
            val k = inc.getIntValue()
            while (i < j) {
                result.children.addAll(node.statements)
                i += k
            }
            return result
        }
        scopeContext.exitScope()
        return ForStatementNode(node.counter, node.start, node.stop, node.inc, statements)
    }

    override fun visit(node: IfStatementNode): AstNode {
        scopeContext.enterScope(node)
        val condition = visit(node.condition)

        val result = if (condition.isConstant()) {
            val intVal = condition.getIntValue()
            if (intVal == 0) {
                AstNode(children = node.falseStatements)
            } else {
                AstNode(children = node.trueStatements)
            }
        } else {
            val trueStatements = visitList(node.trueStatements)
            val falseStatements = visitList(node.falseStatements)
            if (condition is NotExpNode) {
                IfStatementNode(condition.right as ExpNode, falseStatements, trueStatements)
            } else {
                IfStatementNode(node.condition, trueStatements, falseStatements)
            }
        }
        scopeContext.exitScope()
        return result
    }

    override fun visit(node: NotExpNode): AstNode {
        val exp = visit(node.right)
        return if (exp is AtomIntNode) {
            AtomIntNode(expEval.evalUnaryExpression(Op.NOT, exp.value))
        } else {
            node
        }
    }

    override fun visit(node: WhileStatementNode): AstNode {
        scopeContext.enterScope(node)
        val condition = visit(node.condition)
        val statements = visitList(node.statements)
        scopeContext.exitScope()

        if (condition.isConstant() && condition.getIntValue() == 0) {
            return EmptyNode
        }
        return WhileStatementNode(condition as ExpNode, statements)
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