package org.ygl.transformation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.ygl.ast.*
import org.ygl.parse

internal class DeadStoreRemoverTest
{
    private val resolver = DeadStoreResolver()

    @Test
    fun testBasicBlock() {
        val program = """
            fn main() {
                val x = 1;
                val y = x;
                val z = y;
                println(z);
            }
        """

        val ast = test(program)
        assertEquals(1, ast.children[0].children.size)
        assertTrue(ast.children[0].children[0].children[0] is CallStatementNode)
    }

    @Test
    fun testDoubleWrite() {
        val program = """
            fn main() {
                val x = readInt();
                x = readInt();
                val z = x;
                println(z);
            }
        """

        val ast = test(program)
        val main = ast.children[0]
        assertEquals(3, main.children.size)
        assertTrue(main.children[0].children[0] is AssignmentNode)
    }

    @Test
    fun testDanglingWrite() {
        val program = """
            fn main() {
                val x = readInt();
                println(x);
                x = readInt();
            }
        """

        val ast = test(program)
        val main = ast.children[0]
        assertEquals(2, main.children.size)
    }

    private fun test(program: String): AstNode {
        val ast = parse(program)
                .also { ConstantPropagator().visit(it) }
        val deadStores = resolver.getDeadStores(ast)
        return DeadStoreRemover(deadStores).visit(ast)
    }
}