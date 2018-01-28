package org.ygl.transformation

import org.ygl.ast.*
import java.util.*

/**
 * http://www.cs.tau.ac.il/~msagiv/courses/pa07/lecture2-notes-update.pdf
 */
class AssignmentResolver(
        private val globals: Map<String, AstNode> = mapOf()
): AstWalker<Unit>() {

    private val eval = ExpressionEvaluator()
    private val envMap = LinkedHashMap<AstNode, MutableMap<String, AstNode>>()
    private val currentEnv = mutableMapOf<String, AstNode>()
    private val scopeChanged = ArrayDeque<Boolean>()
    private val declarations = ArrayDeque<MutableSet<String>>()

    private fun reset() {
        envMap.clear()
        currentEnv.clear()
        scopeChanged.clear()
        declarations.clear()
        globals.forEach { currentEnv[it.key] = it.value }
    }

    private fun recordChange() {
        if (!scopeChanged.peek()) {
            scopeChanged.pop()
            scopeChanged.push(true)
        }
    }

    fun resolveAssignments(node: FunctionNode): Map<AstNode, Map<String, AstNode>> {
        reset()
        scopeChanged.push(false)
        declarations.push(mutableSetOf())

        visit(node.statements)

        scopeChanged.pop()
        declarations.pop()

        println("env map for fn ${node.name}:")
        envMap.forEach { println("\t${it.key}: \t${it.value}") }

        return envMap
    }

    override fun visit(node: ArrayConstructorNode) {
        currentEnv[node.array] = EmptyNode
    }

    override fun visit(node: ArrayLiteralNode) {
        for (i in 0 until node.items.size) {
            val key = "${node.array}[$i]"
            val value = evaluate(node.items[i])
            currentEnv[key] = value
        }
    }

    override fun visit(node: ArrayWriteNode) {
        val idx = evaluate(node.idx)
        if (idx == EmptyNode) {
            currentEnv.filterKeys { it.startsWith("${node.array}[") }
                    .forEach {
                        currentEnv[it.key] = EmptyNode
                    }
        } else {
            val rhs = evaluate(node.rhs)
            currentEnv["${node.array}[$idx]"] = rhs
        }
    }

    override fun visit(node: AssignmentNode) {
        val rhs = evaluate(node.rhs)
        currentEnv[node.lhs] = rhs
    }

    override fun visit(node: DeclarationNode) {
        val rhs = evaluate(node.rhs)
        declarations.peek().add(node.lhs)
        currentEnv[node.lhs] = rhs
    }

    override fun visit(node: ForStatementNode) {
        visitLoop(node, node.statements)
    }

    override fun visit(node: IfStatementNode) {
        val condition = evaluate(node.condition)
        if (condition.isConstant) {
            if (condition is AtomIntNode && condition.value != 0) {
                doInScope({ visit(node.trueStatements) })
            } else {
                doInScope({ visit(node.falseStatements) })
            }
        } else {
            doInScope({ visit(node.trueStatements) })
            doInScope({ visitList(node.falseStatements) })
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
        //visit(node.children)
        //updateNodeEnv(node)
    }

    private fun visitLoop(node: AstNode, nodes: Iterable<AstNode>) {
        scopeChanged.push(false)
        declarations.push(mutableSetOf())

        do {
            updateNodeEnv(node)
            scopeChanged.apply { pop(); push(false) }
            visit(nodes)
        } while (scopeChanged.peek())

        scopeChanged.pop()
        clearDeclarations()
    }

    private inline fun doInScope(body: () -> Unit) {
        declarations.push(mutableSetOf())
        body()
        for (symbol in declarations.pop()) {
            currentEnv.remove(symbol)
        }
    }

    private fun clearDeclarations() {
        for (symbol in declarations.pop()) {
            currentEnv.remove(symbol)
        }
    }

    private fun updateNodeEnv(node: AstNode) {
        if (node in envMap) {
            val oldEnv = envMap[node]!!
            for ((symbol, value) in currentEnv) {
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
            envMap[node] = HashMap(currentEnv)
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
        return eval.evaluate(node, currentEnv)
    }

    override fun defaultValue(node: AstNode) = Unit
}

//class AssignmentResolver(
//        private val globals: Map<String, AstNode> = mapOf()
//): AstWalker<Unit>() {
//
//    private val eval = ExpressionEvaluator()
//    private val envMap = LinkedHashMap<AstNode, MutableMap<String, AstNode>>()
//    private val envStack = ArrayDeque<MutableMap<String, AstNode>>()
//    private val scopeChanged = ArrayDeque<Boolean>()
//
//    private fun reset() {
//        envMap.clear()
//        scopeChanged.clear()
//        envStack.clear()
//        envStack.push(mutableMapOf())
//        globals.forEach { currentEnv()[it.key] = it.value }
//    }
//
//    private fun currentEnv() = envStack.peek()
//
//    private fun recordChange() {
//        if (!scopeChanged.peek()) {
//            scopeChanged.pop()
//            scopeChanged.push(true)
//        }
//    }
//
//    fun resolveAssignments(node: FunctionNode): Map<AstNode, Map<String, AstNode>> {
//        reset()
//
//        scopeChanged.push(false)
//        envStack.push(mutableMapOf())
//
//        node.statements.forEach { visit(it) }
//
//        scopeChanged.pop()
//        envStack.pop()
//
//        println("env map for fn ${node.name}:")
//        envMap.forEach { println("\t${it.key}: \t${it.value}") }
//
//        return envMap
//    }
//
//    override fun visit(node: ArrayConstructorNode) {
//        currentEnv()[node.array] = EmptyNode
//    }
//
//    override fun visit(node: ArrayLiteralNode) {
//        for (i in 0 until node.items.size) {
//            val key = "${node.array}[$i]"
//            val value = evaluate(node.items[i])
//            currentEnv()[key] = value
//        }
//    }
//
//    override fun visit(node: ArrayWriteNode) {
//        val idx = evaluate(node.idx)
//        if (idx == EmptyNode) {
//            currentEnv().filterKeys { it.startsWith("${node.array}[") }
//                    .forEach {
//                        currentEnv()[it.key] = EmptyNode
//                    }
//        } else {
//            val rhs = evaluate(node.rhs)
//            currentEnv()["${node.array}[$idx]"] = rhs
//        }
//    }
//
//    override fun visit(node: AssignmentNode) {
//        val rhs = evaluate(node.rhs)
//        currentEnv()[node.lhs] = rhs
//    }
//
//    override fun visit(node: DeclarationNode) {
//        val rhs = evaluate(node.rhs)
//        currentEnv()[node.lhs] = rhs
//    }
//
//    override fun visit(node: ForStatementNode) {
//        visitLoop(node, node.statements)
//    }
//
//    override fun visit(node: IfStatementNode) {
//        val condition = evaluate(node.condition)
//        if (condition.isConstant) {
//            if (condition is AtomIntNode && condition.value != 0) {
//                doInScope({ node.trueStatements.forEach { visit(it) } })
//            } else {
//                doInScope({ node.falseStatements.forEach { visit(it) } })
//            }
//        } else {
//            val env1 = doInScope({ node.trueStatements.forEach { visit(it) } })
//            val env2 = doInScope({ node.falseStatements.forEach { visit(it) } })
//
//            val combined = env1
//                    .filter { it.key in env2 }
//                    .mapValues { combine(it.value, env2.getOrDefault(it.key, EmptyNode)) }
//
//            combined.filter { it.key in currentEnv() }
//                    .mapValues { combine(it.value, currentEnv().getOrDefault(it.key, EmptyNode)) }
//                    .forEach { currentEnv()[it.key] = it.value }
//        }
//    }
//
//    override fun visit(node: WhileStatementNode) {
//        val condition = evaluate(node.condition)
//        if (!condition.isConstant || (condition is AtomIntNode && condition.value != 0)) {
//            visitLoop(node, node.statements)
//        }
//    }
//
//    override fun visit(node: StatementNode) {
//        if (node.children.size == 1) {
//            if (node.children[0] is WhileStatementNode || node.children[0] is ForStatementNode) {
//                updateNodeEnv(node)
//            }
//        }
//        updateNodeEnv(node)
//        node.children.forEach { visit(it) }
//    }
//
//    private fun visitLoop(node: AstNode, nodes: Iterable<AstNode>) {
//        scopeChanged.push(false)
//
//        do {
//            updateNodeEnv(node)
//            scopeChanged.apply { pop(); push(false) }
//            nodes.forEach { visit(it) }
//        } while (scopeChanged.peek())
//
//        scopeChanged.pop()
//        //clearDeclarations()
//    }
//
//    private inline fun doInScope(body: () -> Unit): MutableMap<String, AstNode> {
//        val newEnv = mutableMapOf<String, AstNode>()
//        currentEnv().forEach { key, value -> newEnv[key] = value }
//        envStack.push(newEnv)
//        body()
//        return envStack.pop()
//    }
//
//    private fun updateNodeEnv(node: AstNode) {
//        if (node in envMap) {
//            val oldEnv = envMap[node]!!
//            for ((symbol, value) in currentEnv()) {
//                if (symbol in oldEnv) {
//                    val oldValue = oldEnv[symbol]!!
//                    if (oldValue != value) {
//                        oldEnv[symbol] = combine(value, oldValue)
//                    }
//                } else {
//                    recordChange()
//                    oldEnv[symbol] = value
//                }
//            }
//        } else {
//            recordChange()
//            envMap[node] = HashMap(currentEnv())
//        }
//    }
//
//    private fun combine(a: AstNode, b: AstNode): AstNode {
//        if (a == EmptyNode || b == EmptyNode) return EmptyNode
//        if (a != b) {
//            recordChange()
//            return EmptyNode
//        }
//        return a
//    }
//
//    private fun evaluate(node: AstNode): AstNode {
//        return eval.evaluate(node, currentEnv())
//    }
//
//    override fun defaultValue(node: AstNode) = Unit
//}