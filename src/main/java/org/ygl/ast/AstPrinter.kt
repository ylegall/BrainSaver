package org.ygl.ast

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
        for (i in 0 until node.children.size) {
            print(node.children[i], newPrefix, i == node.children.size - 1)
        }
    }

    override fun defaultValue() {}
}