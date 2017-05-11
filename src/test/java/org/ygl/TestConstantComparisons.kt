package org.ygl

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class TestConstantComparisons
{
    @Test
    fun testConstantLessThan() {
        test(2, 4, "<", 1)
        test(3, 1, "<", 0)
        test(1, 1, "<", 0)
        test(0, 0, "<", 0)
    }

    @Test
    fun testConstantGreaterThan() {
        test(2, 4, ">", 0)
        test(3, 1, ">", 1)
        test(1, 1, ">", 0)
        test(0, 0, ">", 0)
    }

    @Test
    fun testConstantLessThanEqual() {
        test(9, 10, "<=", 1)
        test(3, 1, "<=", 0)
        test(1, 1, "<=", 1)
        test(0, 0, "<=", 1)
    }

    @Test
    fun testConstantGreaterThanEqual() {
        test(9, 10, ">=", 0)
        test(3, 2, ">=", 1)
        test(1, 1, ">=", 1)
        test(0, 0, ">=", 1)
    }

    @Test
    fun testConstantEqual() {
        test(2, 4, "==", 0)
        test(3, 2, "==", 0)
        test(1, 1, "==", 1)
        test(0, 0, "==", 1)
    }

    @Test
    fun testConstantNotEqual() {
        test(3, 5, "!=", 1)
        test(2, 1, "!=", 1)
        test(3, 3, "!=", 0)
        test(0, 0, "!=", 0)
    }

    fun test(a: Int, b: Int, op: String, expected: Int) {
        val program = """
            fn main() {
                x = $a $op $b;
                print(x);
            }
        """
        val code = compile(program)
        val result = eval(code)
        assertEquals(expected.toString(), result.trim())
    }
}
