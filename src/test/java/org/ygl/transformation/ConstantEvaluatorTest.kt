package org.ygl.transformation

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.ygl.CompileException
import org.ygl.ast.AtomIntNode
import org.ygl.parse

internal class ConstantEvaluatorTest {

    private val resolver = ConstantResolver()

    @Test
    fun testNoConstants() {
        val program = """ fn main() { print(""); }"""
        val ast = parse(program)
        val constants = resolver.resolveConstants(ast)
        assertTrue(constants.isEmpty())
    }

    @Test
    fun testSingleConstant() {
        val program = """ val x = 5; var y = 3; fn main() { }"""
        val ast = parse(program)
        val constants = resolver.resolveConstants(ast)
        assertEquals(1, constants.size)
        val node = constants["x"] as AtomIntNode
        assertEquals(5, node.value)
    }

    @Test
    fun testSimpleConstants() {
        val program = """ val x = 5; val y = 4; fn main() { }"""
        val ast = parse(program)
        val constants = resolver.resolveConstants(ast)
        assertEquals(2, constants.size)
        val x = constants["x"] as AtomIntNode
        assertEquals(5, x.value)
        val y = constants["y"] as AtomIntNode
        assertEquals(4, y.value)
    }

    @Test
    fun testComplexConstants() {
        val program = """
            val x = y / (z - 1) ;
            val y = 2 * z - 2;
            val z = 7;
            fn main() { }
        """

        val ast = parse(program)
        val constants = resolver.resolveConstants(ast)
        assertEquals(3, constants.size)
        val x = constants["x"] as AtomIntNode
        assertEquals(2, x.value)
        val y = constants["y"] as AtomIntNode
        assertEquals(12, y.value)
        val z = constants["z"] as AtomIntNode
        assertEquals(7, z.value)
    }

    @Test
    fun testCircularDependency() {
        assertThrows(CompileException::class.java, {
            val program = """
                val x = y / (z - 1) ;
                val y = 2 * z - 2;
                val z = x;
                fn main() { }
            """
            val ast = parse(program)
            resolver.resolveConstants(ast)
        })
    }

    @Test
    fun testRuntimeDependency() {
        assertThrows(CompileException::class.java, {
            val program = """
                val x = y / (z - 1) ;
                val y = 2 * z - 2;
                var z = 7;
                fn main() { }
            """
            val ast = parse(program)
            resolver.resolveConstants(ast)
        })
    }
}