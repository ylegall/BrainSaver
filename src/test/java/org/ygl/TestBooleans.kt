package org.ygl

import org.junit.jupiter.api.Assertions.assertEquals
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
        val ctx = testContext(wrapping)
        with (ctx) {
            runtime.enterScope()
            val x = runtime.createSymbol("x")
            val y = runtime.createSymbol("y")
            cg.load(x, a)
            cg.load(y, b)

            val r = when(op) {
                "&&" -> cg.math.and(x, y)
                "||" -> cg.math.or(x, y)
                else -> throw Exception("unsupported op $op")
            }
            val interpreter = Interpreter(str = output.toString(), options = InterpreterOptions(wrap = wrapping))
            interpreter.eval()
            assertEquals(expected, interpreter.getCellValue(r.address))
        }
    }

    @Test
    fun testNot() {
        testNot(1, 0, false)
        testNot(0, 1, false)

        testNot(1, 0, true)
        testNot(0, 1, true)
    }

    private fun testNot(a: Int, expected: Int, wrapping: Boolean) {
        val ctx = testContext(wrapping)
        with (ctx) {
            runtime.enterScope()
            val x = runtime.createSymbol("x")
            cg.load(x, a)
            val r = cg.math.not(x)
            val interpreter = Interpreter(str = output.toString(), options = InterpreterOptions(wrap = wrapping))
            interpreter.eval()
            assertEquals(expected, interpreter.getCellValue(r.address))
        }
    }
}