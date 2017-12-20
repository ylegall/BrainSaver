package org.ygl.transformation

import org.ygl.CompileException
import org.ygl.ast.*
import org.ygl.model.StorageType

/**
 * TODO: function evaluation
 * TODO: local constants
 * TODO: transitive closure
 */
class ConstantExtractor: AstWalker<AstNode>() {

    private val emptyValue = AstNode()
    private val constants = mutableMapOf<String, AstNode>()
    private val globals = mutableSetOf<String>()
    private val expEvaluator = ExpressionEvaluator()

    fun extractConstants(tree: AstNode): Map<String, AstNode> {
        visit(tree)
        return constants
    }

    override fun visit(node: ConstantNode): AstNode {
        val rhs = expEvaluator.evaluate(node.rhs, constants)

        if (node.lhs in constants || node.lhs in globals) {
            throw CompileException("${node.lhs} redefined")
        }
        if (rhs !is AtomIntNode && rhs !is AtomStrNode) {
            throw CompileException("${node.lhs} cannot be evaluated at compile time")
        }
        constants.put(node.lhs, rhs)
        return emptyValue
    }

    override fun visit(node: GlobalVariableNode): AstNode {
        if (node.lhs in constants) {
            throw CompileException("${node.lhs} redefined")
        }
        if (node.storage == StorageType.VAL) {
            val rhs = visit(node.rhs)
            if (rhs !is AtomIntNode && rhs !is AtomStrNode) {
                globals.add(node.lhs)
            } else {
                constants.put(node.lhs, rhs)
            }
        } else {
            globals.add(node.lhs)
        }
        return emptyValue
    }

    override fun defaultValue(): AstNode {
        return emptyValue
    }
}