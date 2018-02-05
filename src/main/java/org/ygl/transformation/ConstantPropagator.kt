package org.ygl.transformation

import org.ygl.ast.*
import org.ygl.model.StorageType
import org.ygl.runtime.Runtime


/**
 * TODO: remove empty loops
 */
class ConstantPropagator: AstTransformer()
{
    private val runtime = Runtime()
    private val assignmentResolver = AssignmentResolver()
    private val evaluator = ExpressionEvaluator()
    private val globals = mutableMapOf<String, AstNode>()

    private var env = emptyMap<AstNode, Map<String, AstNode>>()
    private var constantSymbols = emptyMap<String, AstNode>()

    override fun visit(node: ProgramNode): AstNode {
        val newGlobals = node.children.filterIsInstance<GlobalVariableNode>().map { visit(it) }
        runtime.enterScope()
        val newFunctions = node.children.filterIsInstance<FunctionNode>().map { visit(it) }
        runtime.exitScope()

        return ProgramNode(mutableListOf<AstNode>().apply {
            addAll(newGlobals)
            addAll(newFunctions)
        })
    }

    override fun visit(node: GlobalVariableNode): AstNode {
        val rhs = evaluator.evaluate(node.rhs, globals)
        if (rhs.isConstant) {
            globals[node.lhs] = rhs
        }
        return GlobalVariableNode(node.lhs, rhs, node.sourceInfo)
    }

    override fun visit(node: FunctionNode): AstNode {
        env = assignmentResolver.resolveAssignments(node)
        //for ((key, value) in env) {
        //    println("${key.children}:\t\t$value")
        //}
        val newStatements = visitList(node.statements)
        val newRet = node.ret?.let{ ReturnNode(visit(it)) }
        return FunctionNode(node.name, node.params, newStatements, newRet)
    }

    override fun visit(node: ReturnNode): AstNode {
        constantSymbols = env[node] ?: emptyMap()
        val result = visit(node.children[0])
        return when (result) {
            EmptyNode -> EmptyNode
            else -> ReturnNode(result)
        }
    }

    override fun visit(node: StatementNode): AstNode {
        constantSymbols = env[node] ?: emptyMap()
        val result = visit(node.children[0])
        return when (result) {
            EmptyNode -> EmptyNode
            else -> StatementNode(mutableListOf(result), node.sourceInfo)
        }
    }

    override fun visit(node: ForStatementNode): AstNode {
        val start = eval(node.start)
        val stop = eval(node.stop)
        val inc = eval(node.inc)
        val statements = visitList(node.statements)

        // unroll loop
        if (start.isConstant && stop.isConstant && inc.isConstant) {
            val result = StatementNode()
            var i = start.intValue
            val j = stop.intValue
            val k = inc.intValue
            if (i <= j) result.children.add(DeclarationNode(StorageType.VAR, node.counter, AtomIntNode(0)))
            while (i <= j) {
                result.children.add(AssignmentNode(node.counter, AtomIntNode(i)))
                result.children.addAll(node.statements)
                i += k
            }
            return result
        }
        return ForStatementNode(node.counter, node.start, node.stop, node.inc, statements)
    }

    override fun visit(node: IfStatementNode): AstNode {
        val condition = eval(node.condition)
        val result = if (condition.isConstant) {

            val children = if (condition.intValue == 0) {
                visitList(node.falseStatements)
            } else {
                visitList(node.trueStatements)
            }

            if (children.isEmpty()) {
                EmptyNode
            } else {
                StatementNode(children)
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
        return result
    }

    override fun visit(node: WhileStatementNode): AstNode {
        val condition = eval(node.condition)
        val statements = visitList(node.statements)

        return when {
            statements.isEmpty() -> EmptyNode
            condition.isConstant && condition.intValue == 0 -> EmptyNode
            else -> WhileStatementNode(condition as ExpNode, statements)
        }
    }

    override fun visitChildren(node: AstNode): AstNode {
        return if (node is ExpNode) {
            eval(node)
        } else {
            super.visitChildren(node)
        }
    }

    private fun eval(node: AstNode): AstNode {
        val result = evaluator.evaluate(node, constantSymbols)
        return if (result == EmptyNode) node else result
    }
}