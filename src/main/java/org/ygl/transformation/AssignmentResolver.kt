package org.ygl.transformation

import org.ygl.ast.*
import java.util.*

private typealias Symbols = MutableMap<String, AstNode>

/**
 * http://www.cs.tau.ac.il/~msagiv/courses/pa07/lecture2-notes-update.pdf
 */
class AssignmentResolver(
        private val globals: Map<String, AstNode> = mapOf()
): AstWalker<Unit>() {

    private class Scope(
            val env: Symbols = mutableMapOf()
    ) {
        var changed = false
    }

    private val scopes = ArrayDeque<Scope>()
    private val eval = ExpressionEvaluator()
    private val envMap = LinkedHashMap<AstNode, MutableMap<String, AstNode>>()

    private fun currentEnv() = scopes.peek().env

    private fun recordChange() {
        scopes.peek().changed = true
    }

    private fun reset() {
        envMap.clear()
        scopes.clear()
        scopes.push(Scope())
        globals.forEach { currentEnv()[it.key] = it.value }
    }

    fun resolveAssignments(node: FunctionNode): Map<AstNode, Map<String, AstNode>> {
        reset()
        enterScope()

        visit(node.statements)

        scopes.pop()

        println("env map for fn ${node.name}:")
        envMap.forEach { println("\t${it.key}: \t${it.value}") }

        return envMap
    }

    override fun visit(node: ArrayConstructorNode) {
        currentEnv()[node.array] = EmptyNode
    }

    override fun visit(node: ArrayLiteralNode) {
        for (i in 0 until node.items.size) {
            val key = "${node.array}[$i]"
            val value = evaluate(node.items[i])
            currentEnv()[key] = value
        }
    }

    override fun visit(node: ArrayWriteNode) {
        val idx = evaluate(node.idx)
        if (idx == EmptyNode) {
            currentEnv().filterKeys { it.startsWith("${node.array}[") }
                    .forEach {
                        currentEnv()[it.key] = EmptyNode
                    }
        } else {
            val rhs = evaluate(node.rhs)
            currentEnv()["${node.array}[$idx]"] = rhs
        }
    }

    override fun visit(node: AssignmentNode) {
        val rhs = evaluate(node.rhs)
        currentEnv()[node.lhs] = rhs
    }

    override fun visit(node: DeclarationNode) {
        val rhs = evaluate(node.rhs)
        currentEnv()[node.lhs] = rhs
    }

    override fun visit(node: ForStatementNode) {
        visitLoop(node, node.statements)
    }

    override fun visit(node: IfStatementNode) {
        val condition = evaluate(node.condition)
        if (condition.isConstant) {
            if (condition is AtomIntNode && condition.value != 0) {
                visitList(node.trueStatements)
            } else {
                visitList(node.falseStatements)
            }
        } else {
            // combine the environments of both branches:
            enterScope()
            visitList(node.trueStatements)
            val scope1 = scopes.pop()

            enterScope()
            visitList(node.falseStatements)
            val scope2 = scopes.pop()

            val combined = scope1.env.filter { it.key in scope2.env }
                    .mapValues { combine(it.value, scope2.env.getOrDefault(it.key, EmptyNode)) }

            combined.filter { it.key in currentEnv() }
                    .forEach{ currentEnv()[it.key] = it.value }
        }
    }

    override fun visit(node: WhileStatementNode) {
        val condition = evaluate(node.condition)
        if (!condition.isConstant || (condition is AtomIntNode && condition.value != 0)) {
            visitLoop(node, node.statements)
        }
    }

    override fun visit(node: StatementNode) {
        // for loops, update env before and after
        updateNodeEnv(node)
        visit(node.children)
        if (node.children.size == 1) {
            if (node.children[0] is WhileStatementNode || node.children[0] is ForStatementNode) {
                updateNodeEnv(node)
            }
        }
    }

    private fun visitLoop(node: AstNode, nodes: Iterable<AstNode>) {
        enterScope()
        do {
            updateNodeEnv(node)
            scopes.peek().changed = false
            visit(nodes)
        } while (scopes.peek().changed)
        exitScope()
    }

    private fun enterScope() = scopes.push(Scope(HashMap(currentEnv())))

    private fun exitScope() {
        val scope = scopes.pop()
        scope.env.filter { it.key in currentEnv() }
                .mapValues { combine(it.value, currentEnv().getOrDefault(it.key, EmptyNode)) }
                .forEach { currentEnv()[it.key] = it.value }
    }

    private fun updateNodeEnv(node: AstNode) {
        if (node in envMap) {
            val oldEnv = envMap[node]!!
            for ((symbol, value) in currentEnv()) {
                if (symbol in oldEnv) {
                    val oldValue = oldEnv[symbol]!!
                    if (oldValue != value) {
                        oldEnv[symbol] = combine(value, oldValue)
                    }
                } else {
                    recordChange()
                    oldEnv[symbol] = value
                }
            }
        } else {
            recordChange()
            envMap[node] = HashMap(currentEnv())
        }
    }

    private fun combine(a: AstNode, b: AstNode): AstNode {
        if (a == EmptyNode || b == EmptyNode) return EmptyNode
        if (a != b) {
            recordChange()
            return EmptyNode
        }
        return a
    }

    private fun evaluate(node: AstNode): AstNode {
        return eval.evaluate(node, currentEnv())
    }

    override fun defaultValue(node: AstNode) = Unit
}
