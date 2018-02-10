package org.ygl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TestIO
{
    @Test
    fun testPrintInt() {
        testPrint(0, "0")
        testPrint(3, "3")
        testPrint(10, "10")
        testPrint(102, "102")
        testPrint("\"ab\\ncd\"", "ab\ncd")
    }

    private fun testPrint(a: Any, expected: String) {
        val program = """
            fn main() {
                val x = $a;
                print(x);
            }
        """
        val code = compile(program)
        val result = eval(code)
        assertEquals(expected, result.trim())
    }

    @Test
    fun testRuntimePrintInt() {
        testRuntimePrint(0, "0")
        testRuntimePrint(3, "3")
    }

    private fun testRuntimePrint(a: Int, expected: String) {
        val program = """
            fn main() {
                val x = readInt();
                print(x);
            }
        """
        val code = compile(program)
        val result = eval(code, userInput = a.toString())
        assertEquals(expected, result.trim())
    }
}