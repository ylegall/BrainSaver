package org.ygl.ast

/**
 * utility for printing an ast
 */
class AstPrinter : AstWalker<Unit>()
{
    fun print(tree: AstNode) {
        print(tree, "", true)
    }

    private fun print(node: AstNode, prefix: String, last: Boolean = false) {
        print(prefix)
        val newPrefix = prefix + if (last) {
            print('└')
            "  "
        } else {
            print('├')
            "| "
        }
        println(node)

        val children = when (node) {
            is ForStatementNode -> node.statements
            else -> node.children
        }

        for (i in 0 until children.size) {
            print(children[i], newPrefix, i == children.size - 1)
        }
    }

    override fun defaultValue(node: AstNode) {}
}