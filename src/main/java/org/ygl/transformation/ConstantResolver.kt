package org.ygl.transformation

import org.ygl.CompileException
import org.ygl.CompilerOptions
import org.ygl.ast.*
import org.ygl.model.StorageType
import org.ygl.runtime.NamedSymbol
import org.ygl.runtime.ScopeContext

/**
 *
 */
class ConstantResolver(
        private val options: CompilerOptions
) : AstWalker<AstNode>() {

    private val expEvaluator = ExpressionEvaluator()
    private val constants = mutableMapOf<String, AstNode>()
    private val scopeSymbols = ScopeContext<NamedSymbol>()

    fun resolveConstants(tree: AstNode): AstNode {
        val newTree = visit(tree)

        if (options.verbose) {
            println("\nresolved constants:")
            println("-------------------")
            constants.forEach { (key, value) -> println("\t$key = $value") }
        }

        return newTree
    }

    override fun visit(node: ProgramNode): AstNode {
        scopeSymbols.enterScope(node)

        node.children.filterIsInstance<ConstantNode>().forEach { visit(it) }

        val newGlobals = node.children
                .filterIsInstance<GlobalVariableNode>()
                .map { visit(it) }
                .filterNot { it == EmptyNode }

        val functions = node.children
                .filterIsInstance<FunctionNode>()
                .map { visit(it) }

        node.children.clear()
        node.children.addAll(newGlobals)
        node.children.addAll(functions)

        scopeSymbols.exitScope()
        return node
    }

    override fun visit(node: ConstantNode): AstNode {
        val rhs = expEvaluator.evaluate(node.rhs, constants)
        if (node.lhs in constants) {
            throw CompileException("${node.lhs} redefined")
        }
        if (rhs !is AtomIntNode && rhs !is AtomStrNode) {
            throw CompileException("${node.lhs} cannot be evaluated at compile time")
        }
        constants.put(node.lhs, rhs)
        return EmptyNode
    }

    override fun visit(node: GlobalVariableNode): AstNode {
        if (node.lhs in constants) {
            throw CompileException("${node.lhs} redefined")
        }
        val rhs = visit(node.rhs)
        if (node.storage == StorageType.VAL) {
            if (rhs.isConstant()) {
                constants.put(node.lhs, rhs)
                return EmptyNode
            }
        }
        return GlobalVariableNode(node.storage, node.lhs, visit(node.rhs))
    }

    override fun visit(node: DeclarationNode): AstNode {
        val rhs = visit(node.rhs)
        scopeSymbols.addSymbol(NamedSymbol(node.lhs))
        return DeclarationNode(node.storage, node.lhs, rhs)
    }

    override fun visit(node: ForStatementNode): AstNode {
        val start = visit(node.start) as AtomNode
        val stop = visit(node.stop) as AtomNode
        val inc = node.inc?.let { visit(it) as AtomNode }
        node.statements.forEach { visit(it) }
        return ForStatementNode(node.counter, start, stop, inc, node.statements)
    }

    override fun visit(node: WhileStatementNode): AstNode {
        val condition = visit(node.condition) as ExpNode
        node.statements.forEach { visit(it) }
        return WhileStatementNode(condition, node.statements)
    }

    override fun visit(node: IfStatementNode): AstNode {
        val condition = visit(node.condition) as ExpNode
        node.trueStatements.forEach { visit(it) }
        node.falseStatements.forEach { visit(it) }
        return IfStatementNode(condition, node.trueStatements, node.falseStatements)
    }

    override fun visit(node: FunctionNode): AstNode {
        scopeSymbols.enterScope(node)
        val newStatements = MutableList(node.statements.size, { i -> visit(node.statements[i]) })
        scopeSymbols.exitScope()
        return FunctionNode(node.name, node.params, newStatements)
    }

    override fun visit(node: AssignmentNode): AstNode {
        return AssignmentNode(node.lhs, visit(node.rhs))
    }

    override fun visit(node: ArrayReadExpNode): AstNode {
        return ArrayReadExpNode(node.array, visit(node.idx))
    }

    override fun visit(node: ArrayWriteNode): AstNode {
        return ArrayWriteNode(node.array, visit(node.idx), visit(node.rhs))
    }

    override fun visit(node: AtomIdNode): AstNode {
        return when (node.identifier) {
            in scopeSymbols -> node
            in constants -> constants[node.identifier]!!
            else -> node
        }
    }

    override fun visit(node: CallStatementNode): AstNode {
        val params = MutableList(node.params.size, { idx -> visit(node.params[idx]) })
        return CallStatementNode(node.name, params)
    }

    override fun visit(node: CallExpNode): AstNode {
        val params = MutableList(node.params.size, { idx -> visit(node.params[idx]) })
        return CallExpNode(node.name, params)
    }

    override fun visit(node: BinaryExpNode): AstNode {
        val left = visit(node.left)
        val right = visit(node.right)
        return BinaryExpNode(node.op, left, right)
    }

    override fun visit(node: NotExpNode): AstNode {
        return NotExpNode(visit(node.right))
    }

    override fun visitChildren(node: AstNode): AstNode {
        val newChildren = MutableList(node.children.size, { idx -> visit(node.children[idx]) })
        node.children.clear()
        newChildren.filterNot { it == EmptyNode }.forEach { node.children.add(it) }
        return node
    }

    override fun defaultValue(): AstNode {
        return EmptyNode
    }
}
