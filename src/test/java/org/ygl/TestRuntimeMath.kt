package org.ygl

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class TestRuntimeMath
{
    @Test
    fun testRuntimeAdd() {
        test(3, 1, "+", 4)
        test(0, 2, "+", 2)
        test(7, 3, "+", 10)
    }

    @Test
    fun testRuntimeSub() {
        test(3, 1, "-", 2)
        test(3, 0, "-", 3)
        test(0, 2, "-", 0)
        test(9, 3, "-", 6)
    }

    @Test
    fun testRuntimeMult() {
        test(3, 1, "*", 3)
        test(3, 0, "*", 0)
        test(5, 2, "*", 10)
        test(3, 40, "*", 120)
    }

    @Test
    fun testRuntimeDiv() {
        test(3, 1, "/", 3)
        test(0, 3, "/", 0)
        test(5, 2, "/", 2)
        test(8, 2, "/", 4)
    }

    private fun test(a: Int, b: Int, op: String, expected: Int) {
        val program = """
            fn main() {
                readInt(y);
                x = y $op $b;
                print(x);
            }
        """
        val code = compile(program)
        val result = eval(code, userInput = a.toString())
        assertEquals(expected.toString(), result.trim())
    }
}