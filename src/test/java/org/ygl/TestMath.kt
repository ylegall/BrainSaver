package org.ygl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 */
internal class TestMath {

    @Test
    fun testAdd() {
        test(3, "+", 5, 8, false)
        test(0, "+", 0, 0, false)
        test(0, "+", 1, 1, false)
        test(3, "+", 5, 8, true)
        test(0, "+", 0, 0, true)
        test(0, "+", 1, 1, true)
    }
    
    @Test
    fun testSub() {
        test(5, "-", 3, 2, false)
        test(3, "-", 5, 0, false)
        test(0, "-", 0, 0, false)
        test(1, "-", 1, 0, false)
        test(5, "-", 3, 2, true)
        test(3, "-", 5, 0, true)
        test(0, "-", 0, 0, true)
    }

    @Test
    fun testMult() {
        test(3, "*", 4, 12, false)
        test(1, "*", 5, 5, false)
        test(0, "*", 3, 0, false)

        test(3, "*", 4, 12, true)
        test(1, "*", 5, 5, true)
        test(0, "*", 3, 0, true)
    }

    @Test
    fun testDiv() {
        test(12, "/", 4, 3, false)
        test(3, "/", 5, 0, false)
        test(3, "/", 3, 1, false)
        test(5, "/", 3, 1, false)

        test(12, "/", 4, 3, true)
        test(3, "/", 5, 0, true)
        test(3, "/", 3, 1, true)
        test(5, "/", 3, 1, true)
    }

    @Test
    fun testMod() {
        test(12, "%", 4, 0, false)
        test(5, "%", 3, 2, false)
        test(3, "%", 3, 0, false)

        test(12, "%", 4, 0, true)
        test(5, "%", 3, 2, true)
        test(3, "%", 3, 0, true)
    }

    fun test(a: Int, op: String, b: Int, expected: Int, wrapping: Boolean) {
        val ctx = TestContext(wrapping)
        val cg = ctx.cg
        val x = cg.currentScope().createSymbol("x")
        val y = cg.currentScope().createSymbol("y")
        cg.loadInt(x, a)
        cg.loadInt(y, b)
        val r = when(op) {
            "+" -> cg.math.add(x, y)
            "-" -> cg.math.subtract(x, y)
            "*" -> cg.math.multiply(x, y)
            "/" -> cg.math.divide(x, y)
            "%" -> cg.math.mod(x, y)
            else -> throw Exception("unsupported op $op")
        }

        val interpreter = ctx.eval(InterpreterOptions(isWrapping = wrapping))
        assertEquals(expected, interpreter.getCellValue(r.address))
    }

}