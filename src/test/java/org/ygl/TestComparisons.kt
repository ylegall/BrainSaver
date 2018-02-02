package org.ygl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 */
internal class TestComparisons {

    @Test
    fun testLessThan() {
        test(3, "<", 5, 1, false)
        test(5, "<", 3, 0, false)
        test(1, "<", 1, 0, false)
        test(0, "<", 0, 0, false)

        test(3, "<", 5, 1, true)
        test(5, "<", 3, 0, true)
        test(1, "<", 1, 0, true)
        test(0, "<", 0, 0, true)
    }

    @Test
    fun testLessThanEqual() {
        test(3, "<=", 5, 1, false)
        test(5, "<=", 3, 0, false)
        test(1, "<=", 1, 1, false)
        test(0, "<=", 0, 1, false)

        test(3, "<=", 5, 1, true)
        test(5, "<=", 3, 0, true)
        test(1, "<=", 1, 1, true)
        test(0, "<=", 0, 1, true)
    }

    @Test
    fun testGreaterThan() {
        test(3, ">", 5, 0, false)
        test(5, ">", 3, 1, false)
        test(1, ">", 1, 0, false)
        test(0, ">", 0, 0, false)

        test(3, ">", 5, 0, true)
        test(5, ">", 3, 1, true)
        test(1, ">", 1, 0, true)
        test(0, ">", 0, 0, true)
    }

    @Test
    fun testGreaterThanEqual() {
        test(3, ">=", 5, 0, false)
        test(5, ">=", 3, 1, false)
        test(1, ">=", 1, 1, false)
        test(0, ">=", 0, 1, false)

        test(3, ">=", 5, 0, true)
        test(5, ">=", 3, 1, true)
        test(1, ">=", 1, 1, true)
        test(0, ">=", 0, 1, true)
    }

    @Test
    fun testEqual() {
        test(3, "==", 5, 0, false)
        test(5, "==", 3, 0, false)
        test(1, "==", 1, 1, false)
        test(0, "==", 0, 1, false)

        test(3, "==", 5, 0, true)
        test(5, "==", 3, 0, true)
        test(1, "==", 1, 1, true)
        test(0, "==", 0, 1, true)
    }

    @Test
    fun testNotEqual() {
        test(3, "!=", 5, 1, false)
        test(5, "!=", 3, 1, false)
        test(1, "!=", 1, 0, false)
        test(0, "!=", 0, 0, false)

        test(3, "!=", 5, 1, true)
        test(5, "!=", 3, 1, true)
        test(1, "!=", 1, 0, true)
        test(0, "!=", 0, 0, true)
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
                "<" -> cg.math.lessThan(x, y)
                ">" -> cg.math.greaterThan(x, y)
                "<=" -> cg.math.lessThanEqual(x, y)
                ">=" -> cg.math.greaterThanEqual(x, y)
                "==" -> cg.math.equal(x, y)
                "!=" -> cg.math.notEqual(x, y)
                else -> throw Exception("unsupported op $op")
            }

            val interpreter = Interpreter(str = output.toString(), options = InterpreterOptions(wrap = wrapping))
            interpreter.eval()
            assertEquals(expected, interpreter.getCellValue(r.address))
        }
    }

}