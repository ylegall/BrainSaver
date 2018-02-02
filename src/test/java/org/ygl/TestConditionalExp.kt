package org.ygl

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class TestConditionalExp
{
    @Test
    fun testConditionalExp() {
        val program = """
            var g1 = true;
            var g2 = false;
            fn main() {
                val x = g1 ? 3 : 4;
                val y = g2 ? 3 : 4;
                print(x);
                print(y);
            }
        """
        val result = compileAndEval(program)
        assertEquals("34", result)
    }

}