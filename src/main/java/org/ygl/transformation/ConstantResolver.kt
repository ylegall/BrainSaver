package org.ygl.transformation

import org.ygl.CompileException
import org.ygl.CompilerOptions
import org.ygl.ast.*
import java.util.*

/**
 *
 */
class ConstantResolver: AstWalker<Unit>() {

    private val expEvaluator = ExpressionEvaluator()
    private val constants = mutableMapOf<String, AstNode>()

    init {
        constants["true"] = AtomIntNode(1)
        constants["false"] = AtomIntNode(0)
    }

    fun resolveConstants(root: AstNode): Map<String, AstNode> {
        assert(root is ProgramNode)
        val initialConstants = constants.size
        val constantNodes = root.children.filterIsInstance<ConstantNode>()

        if (constantNodes.isNotEmpty()) {
            do {
                var madeProgress = false
                for (node in constantNodes) {
                    val result = expEvaluator.evaluate(node.rhs, constants)
                    if (result.isConstant) {
                        madeProgress = true
                        constants[node.lhs] = result
                    }
                }
                val remaining = constantNodes.size - (constants.size - initialConstants)
                if (!madeProgress && (remaining > 0)) {
                    throw CompileException("cannot evaluate ${constantNodes[0].lhs} at compile time")
                }
            } while (remaining > 0)
        }
        return constants
    }

    override fun defaultValue(node: AstNode) = Unit
}

