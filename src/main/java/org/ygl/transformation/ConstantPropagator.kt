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
        if (rhs.isConstant()) {
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
        val start = eval(node.start) as AtomIntNode
        val stop = eval(node.stop) as AtomIntNode
        val inc = eval(node.inc) as AtomIntNode
        val statements = visitList(node.statements)

        // unroll loop
        if (start.isConstant() && stop.isConstant() && inc.isConstant()) {
            val result = StatementNode()
            var i = start.value
            val j = stop.value
            val k = inc.value
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
        val result = if (condition.isConstant()) {

            val cond = condition as AtomIntNode
            val children = if (cond.value == 0) {
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
            if (condition.nodeType == NodeType.UNARY_EXP) {
                val cond = condition as NotExpNode
                IfStatementNode(cond.right, falseStatements, trueStatements)
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
            else -> {
                if (condition.isConstant() && (condition as AtomIntNode).value == 0) {
                    EmptyNode
                } else {
                    WhileStatementNode(condition, statements)
                }
            }
        }
    }

    override fun visitChildren(node: AstNode): AstNode {
        return if (isExp(node)) {
            eval(node)
        } else {
            super.visitChildren(node)
        }
    }

    private fun eval(node: AstNode): AstNode {
        val result = evaluator.evaluate(node, constantSymbols)
        return if (result == EmptyNode) node else result
    }

    private fun isExp(node: AstNode): Boolean {
        return when (node.nodeType) {
            NodeType.ARRAY_READ_EXP -> true
            NodeType.ATOM_ID -> true
            NodeType.ATOM_INT -> true
            NodeType.ATOM_STR -> true
            NodeType.BINARY_EXP -> true
            NodeType.CALL_EXP -> true
            NodeType.IF_EXP -> true
            NodeType.UNARY_EXP -> true
            else -> false
        }
    }
}