package org.ygl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class TestBooleans
{

    @Test
    fun testAnd() {
        test(1, "&&", 1, 1, false)
        test(0, "&&", 1, 0, false)
        test(1, "&&", 0, 0, false)
        test(0, "&&", 0, 0, false)

        test(1, "&&", 1, 1, true)
        test(0, "&&", 1, 0, true)
        test(1, "&&", 0, 0, true)
        test(0, "&&", 0, 0, true)
    }

    @Test
    fun testOr() {
        test(1, "||", 1, 1, false)
        test(0, "||", 1, 1, false)
        test(1, "||", 0, 1, false)
        test(0, "||", 0, 0, false)

        test(1, "||", 1, 1, true)
        test(0, "||", 1, 1, true)
        test(1, "||", 0, 1, true)
        test(0, "||", 0, 0, true)
    }

    fun test(a: Int, op: String, b: Int, expected: Int, wrapping: Boolean) {
        val ctx = TestContext(wrapping)
        val cg = ctx.cg
        val x = cg.currentScope().createSymbol("x")
        val y = cg.currentScope().createSymbol("y")
        cg.loadInt(x, a)
        cg.loadInt(y, b)
        val r = when(op) {
            "&&" -> cg.math.and(x, y)
            "||" -> cg.math.or(x, y)
            "!" -> cg.math.not(x)
            else -> throw Exception("unsupported op $op")
        }

        val interpreter = ctx.eval(InterpreterOptions(isWrapping = wrapping))
        Assertions.assertEquals(expected, interpreter.getCellValue(r.address))
    }

    @Test
    fun testNot() {
        testNot(1, 0, false)
        testNot(0, 1, false)

        testNot(1, 0, true)
        testNot(0, 1, true)
    }

    fun testNot(a: Int, expected: Int, wrapping: Boolean) {
        val ctx = TestContext(wrapping)
        val cg = ctx.cg
        val x = cg.currentScope().createSymbol("x")
        cg.loadInt(x, a)
        val r = cg.math.not(x)

        val interpreter = ctx.eval(InterpreterOptions(isWrapping = wrapping))
        Assertions.assertEquals(expected, interpreter.getCellValue(r.address))
    }
}