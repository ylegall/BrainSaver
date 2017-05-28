package org.ygl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class TestAssignment
{
    // TODO
    @Test
    fun testConstantAssignment() {
        test(0, "00")
        test(1, "11")
    }

    private fun test(a: Int, expected: String) {
        val program = """
            fn main() {
                x = $a;
                y = x;
                print(x);
                print(y);
            }
        """
        val result = compileAndEval(program)
        Assertions.assertEquals(expected, result.trim())
    }
}
