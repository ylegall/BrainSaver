package org.ygl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TestConversionMath
{

    @Test
    fun testPrintLargeInt() {
        val program = """
            fn main() {
                x = 102;
                print(x);
            }
        """
        val code = compile(program)
        val result = eval(code)
        assertEquals("102", result.trim())
    }
}