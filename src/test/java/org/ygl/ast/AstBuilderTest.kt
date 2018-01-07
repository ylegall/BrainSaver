package org.ygl.ast

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.ygl.parse

internal class AstBuilderTest {

    @Test
    fun testGlobalVariables() {
        val program = """var x = 10;"""
        val ast = parse(program)
        assertTrue(ast.children[0] is GlobalVariableNode)
        val global = ast.children[0] as GlobalVariableNode
        assertEquals("x", global.lhs)
    }

    @Test
    fun testConstantVariables() {
        val program = """val x = 10;"""
        val ast = parse(program)
        assertTrue(ast.children[0] is ConstantNode)
        val constant = ast.children[0] as ConstantNode
        assertEquals("x", constant.lhs)
    }

    @Test
    fun testFunctions() {
        val program = """fn foo(x, y) { val z = 0; return z; }"""
        val ast = parse(program)
        assertTrue(ast.children[0] is FunctionNode)
        val function = ast.children[0] as FunctionNode
        assertEquals("foo", function.name)
        assertEquals(listOf("x", "y"), function.params)
        assertTrue(function.statements[0].children[0] is DeclarationNode)
        assertTrue(function.statements[1] is ReturnNode)
    }

}