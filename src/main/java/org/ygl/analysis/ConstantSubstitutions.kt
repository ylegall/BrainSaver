package org.ygl.analysis

import org.ygl.ast.*
import java.util.*

class ConstantSubstitutions(
        val constants: Map<String, AstNode>
) : AstWalker<Unit>() {

    val scopeSymbols = ArrayDeque<MutableSet<String>>()

    override fun visit(node: FunctionNode) {
        scopeSymbols.push(mutableSetOf())
        visit(node.statements)
        scopeSymbols.pop()
    }

    override fun visit(node: DeclarationNode) {
        scopeSymbols.peek().add(node.lhs)
        visit(node.rhs)
    }

    override fun visit(node: AtomIdNode) {
        // TODO: this child with atomIntNode
        scopeSymbols.reversed().forEach { scope ->
            if (node.identifier in scope) return
        }
    }

    override fun aggregateResult(agg: Unit, next: Unit) {}

    override fun defaultValue() {}
}