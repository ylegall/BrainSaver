package org.ygl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 */
internal class TestRuntimeComparisons
{
    @Test
    fun testRuntimeLessThan() {
        test(2, 4, "<", 1)
        test(8, 9, "<", 1)
        test(0, 1, "<", 1)
        test(1, 1, "<", 0)
        test(0, 0, "<", 0)
    }

    @Test
    fun testRuntimeGreaterThan() {
        test(2, 4, ">", 0)
        test(3, 1, ">", 1)
        test(1, 1, ">", 0)
        test(0, 0, ">", 0)
    }

    @Test
    fun testRuntimeLessThanEqual() {
        test(10, 9, "<=", 0)
        test(2, 3, "<=", 1)
        test(0, 0, "<=", 1)
    }

    @Test
    fun testRuntimeGreaterThanEqual() {
        test(10, 9, ">=", 1)
        test(2, 3, ">=", 0)
        test(1, 1, ">=", 1)
        test(0, 0, ">=", 1)
    }

    @Test
    fun testRuntimeEqual() {
        test(2, 4, "==", 0)
        test(3, 2, "==", 0)
        test(1, 1, "==", 1)
        test(0, 0, "==", 1)
    }

    @Test
    fun testRuntimeNotEqual() {
        test(3, 5, "!=", 1)
        test(2, 1, "!=", 1)
        test(3, 3, "!=", 0)
        test(0, 0, "!=", 0)
    }

    fun test(a: Int, b: Int, op: String, expected: Int) {
        val program = """
            fn main() {
                readInt(y);
                x = $a $op y;
                print(x);
            }
        """
        val result = compileAndEval(program, userInput = b.toString())
        assertEquals(expected.toString(), result.trim())
    }
}