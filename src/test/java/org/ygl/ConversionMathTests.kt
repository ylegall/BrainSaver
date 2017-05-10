package org.ygl

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class ConversionMathTests
{
    @Test
    fun testPrintLargeInt() {
        test(102, 0, "+", 103)
    }

    private fun test(a: Int, b: Int, op: String, expected: Int) {
        val program = """
            fn main() {
                x = $a;
                print(x);
            }
        """
        val code = compile(program)
        val result = eval(code, userInput = a.toString())
        assertEquals(expected.toString(), result.trim())
    }
}