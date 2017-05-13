package org.ygl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration

internal class TestLoops
{
    @Test
    fun testConstantWhile() {
        Assertions.assertTimeout(Duration.ofSeconds(5), {
            testConstantWhile(0, "i < 4", "i += 1", "0123")
            testConstantWhile(0, "i < 5", "i += 2", "024")
            testConstantWhile(3, "i > 0", "i -= 1", "321")
        })
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

    @Test
    fun testRuntimeWhile() {
        Assertions.assertTimeout(Duration.ofSeconds(5), {
            testRuntimeWhile(0, "")
            testRuntimeWhile(2, "1")
            testRuntimeWhile(5, "1234")
        })
    }

    fun testRuntimeWhile(input: Int, expected: String) {
        val program = """
            fn main() {
                readInt(x);
                i = 1;
                while (i < x) {
                    print(i);
                    i += 1;
                }
            }
        """
        val result = compileAndEval(program, userInput = input.toString())
        Assertions.assertEquals(expected, result.trim())
    }
}