package org.ygl.transformation

import org.ygl.ast.*
import org.ygl.runtime.NamedSymbol
import org.ygl.runtime.ScopeContext

class ConstantResolver(
        private val constants: Map<String, AstNode>
) : AstWalker<AstNode>() {

    private val scopeSymbols = ScopeContext<NamedSymbol>()

    fun resolveConstants(tree: AstNode): AstNode {
        return visit(tree)
    }

    override fun visit(node: ConstantNode) = emptyNode

    override fun visit(node: GlobalVariableNode): AstNode {
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
        val newStatements = MutableList(node.statements.size, { i -> visit(node.statements[i]) as StatementNode })
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
        node.children = newChildren.filter { it != emptyNode }.toCollection(mutableListOf())
        return node
    }

    override fun defaultValue(): AstNode {
        return emptyNode
    }
}
