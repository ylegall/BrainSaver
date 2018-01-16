package org.ygl.transformation

import org.ygl.ast.*
import java.util.*

/**
 *
 */
class ConstantEvaluator(
        private val constants: Map<String, AstNode> = mutableMapOf()
) : AstTransformer() {

    private val scopeSymbols = ArrayDeque<MutableSet<String>>()

    fun evaluateConstants(tree: AstNode): AstNode {
        return visit(tree)
    }

    override fun visit(node: ProgramNode): AstNode {
        scopeSymbols.push(mutableSetOf())

        val newGlobals = node.children
                .filterIsInstance<GlobalVariableNode>()
                .map { visit(it) }

        val functions = node.children
                .filterIsInstance<FunctionNode>()
                .map { visit(it) }

        node.children.clear()
        node.children.addAll(newGlobals)
        node.children.addAll(functions)

        scopeSymbols.pop()
        return node
    }

    override fun visit(node: FunctionNode): AstNode {
        scopeSymbols.push(mutableSetOf())
        node.params.forEach { scopeSymbols.peek().add(it) }
        val newStatements = MutableList(node.statements.size, { i -> visit(node.statements[i]) })
        scopeSymbols.pop()
        return FunctionNode(node.name, node.params, newStatements)
    }

    // shadowing not allowed
    override fun visit(node: AtomIdNode): AstNode {
        return constants[node.identifier] ?: node
    }

}