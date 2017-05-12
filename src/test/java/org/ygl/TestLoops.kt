package org.ygl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class TestLoops
{
    @Test
    fun testConstantWhile() {
        testConstantWhile(0, "i < 4", "i += 1", "0123")
        testConstantWhile(0, "i < 5", "i += 2", "024")
        testConstantWhile(3, "i > 0", "i -= 1", "321")
    }

    fun testConstantWhile(start: Int, condition: String, inc: String, expected: String) {
        val program = """
            fn main() {
                i = $start;
                while ($condition) {
                    print(i);
                    $inc;
                }
            }
        """
        val result = compileAndEval(program)
        Assertions.assertEquals(expected, result.trim())
    }
}