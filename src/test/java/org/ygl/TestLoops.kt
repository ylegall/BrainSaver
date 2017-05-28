package org.ygl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration

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
        Assertions.assertTimeout(Duration.ofSeconds(5), {
            val result = compileAndEval(program)
            Assertions.assertEquals(expected, result.trim())
        })
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

    @Test
    fun testConstantFor() {
        testConstantFor("1", "4", expected="10")
        testConstantFor("0", "4", "by 2", expected="6")
        testConstantFor("3", "2", expected="0")
    }

    fun testConstantFor(start: String, stop: String, step: String = "", expected: String) {
        val program = """
            fn main() {
                x = 0;
                for (i in $start .. $stop $step) {
                    x += i;
                }
                print(x);
            }
        """
        val result = compileAndEval(program)
        Assertions.assertEquals(expected, result.trim())
    }

    @Test
    fun testRuntimeFor() {
        testRuntimeFor("1", "4", expected="1234")
        testRuntimeFor("0", "4", "by 2", expected="024")
        testRuntimeFor("3", "2", expected="")
    }

    fun testRuntimeFor(start: String, stop: String, step: String = "", expected: String) {
        val program = """
            fn main() {
                for (i in $start .. $stop $step) {
                    print(i);
                }
            }
        """
        val result = compileAndEval(program)
        Assertions.assertEquals(expected, result.trim())
    }

    @Test
    fun testMultiWhileStatements() {
        val program = """
            fn main() {
                x = 0;
                y = 0;
                l = 4;
                while (x < l) {
                    x += 1;
                    y += 1;
                }
                println(x, y);
            }
        """
        val result = compileAndEval(program)
        Assertions.assertEquals("4\n4", result.trim())
    }
}
