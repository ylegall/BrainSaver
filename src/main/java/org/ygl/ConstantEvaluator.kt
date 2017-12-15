package org.ygl

import org.ygl.ast.*

class ExpResult(val value: Int)

class ConstantEvaluator(val programInfo: ProgramInfo) : AstWalker<ExpResult>()
{
    private val symbols = mutableMapOf<String, Int>()
    private val emptyResult = ExpResult(-1)

    override fun visit(node: AssignmentNode): ExpResult {
        val rhs = visit(node.rhs)
        if (rhs != emptyResult) {
            node.rhs = AtomIntNode(rhs.value)
            symbols[node.lhs] = rhs.value
        }
        return emptyResult
    }

    override fun visit(node: BinaryExpNode): ExpResult {
        val op = node.op
        val left = visit(node.left)
        val right = visit(node.right)

        if (left == emptyResult || right == emptyResult) return emptyResult

        return when (node.op) {
            "+" ->  ExpResult(left.value + right.value)
            "-" ->  ExpResult(left.value - right.value)
            "*" ->  ExpResult(left.value * right.value)
            "/" ->  ExpResult(left.value / right.value)
            "%" ->  ExpResult(left.value % right.value)
            "<" ->  ExpResult(if (left.value < right.value)  1 else 0)
            ">" ->  ExpResult(if (left.value > right.value)  1 else 0)
            "==" -> ExpResult(if (left.value == right.value) 1 else 0)
            "!=" -> ExpResult(if (left.value != right.value) 1 else 0)
            "<=" -> ExpResult(if (left.value <= right.value) 1 else 0)
            ">=" -> ExpResult(if (left.value >= right.value) 1 else 0)
            "&&" -> ExpResult(left.value and right.value)
            "||" -> ExpResult(left.value or right.value)
            else -> throw Exception("invalid op $op")
        }
    }

    override fun visit(node: NotExpNode): ExpResult {
        val childExp = visit(node.right)
        return when (childExp) {
            emptyResult -> emptyResult
            else -> ExpResult(if (childExp.value == 0) 1 else 0)
        }
    }

    override fun visit(node: AtomIdNode): ExpResult {
        // TODO check this symbol is written in a conditional context
        return if (node.identifier in symbols) {
            ExpResult(symbols[node.identifier]!!)
        } else {
            emptyResult
        }
    }

    override fun visit(node: AtomIntNode): ExpResult {
        return ExpResult(node.value)
    }

    override fun aggregateResult(agg: ExpResult, next: ExpResult): ExpResult {
        return emptyResult
    }

    override fun defaultValue(): ExpResult {
        return emptyResult
    }

}