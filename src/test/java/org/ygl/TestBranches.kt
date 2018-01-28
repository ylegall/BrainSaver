package org.ygl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTimeout
import org.junit.jupiter.api.Test
import java.time.Duration


internal class TestBranches
{
    @Test
    fun testConstantIfElse() {
        testConstant(5, 7, "<", 1)
        testConstant(5, 7, ">", 2)

        testConstant(6, 6, ">=", 1)
        testConstant(8, 6, ">=", 1)

        testConstant(4, 4, "==", 1)
        testConstant(0, 0, "==", 1)
        testConstant(3, 0, "==", 2)

        testConstant(0, 2, "!=", 1)
        testConstant(1, 2, "!=", 1)
    }

    fun testConstant(a: Int, b: Int, op: String, expected: Int) {
        val program = """
            fn main() {
                val x = $a;
                val y = $b;
                if (x $op y) {
                    print("1");
                } else {
                    print("2");
                }
            }
        """
        val result = compileAndEval(program)
        assertEquals(expected.toString(), result.trim())
    }

    @Test
    fun testRuntimeIfElse() {
        testRuntime(5, 7, "<", 1)
        testRuntime(5, 7, ">", 2)

        testRuntime(6, 6, ">=", 1)
        testRuntime(8, 6, ">=", 1)

        testRuntime(4, 4, "==", 1)
        testRuntime(0, 0, "==", 1)
        testRuntime(3, 0, "==", 2)

        testRuntime(0, 2, "!=", 1)
        testRuntime(1, 2, "!=", 1)
    }

    private fun testRuntime(a: Int, b: Int, op: String, expected: Int) {
        val program = """
            fn main() {
                val x = $a;
                val y = readInt();
                if (x $op y) {
                    print("1");
                } else {
                    print("2");
                }
            }
        """
        val result = compileAndEval(program, userInput = b.toString())
        assertEquals(expected.toString(), result.trim())
    }

    @Test
    fun testConstantIf() {
        //testConstantIf(4, 5, ">", "")
        testConstantIf(5, 4, ">", "1")
        //testConstantIf(2, 2, "==", "1")
        //testConstantIf(2, 1, "==", "")
    }

    private fun testConstantIf(a: Int, b: Int, op: String, expected: String) {
        val program = """
            fn main() {
                val x = $a;
                val y = $b;
                if (x $op y) {
                    print("1");
                }
            }
        """
        val result = compileAndEval(program, userInput = b.toString())
        assertEquals(expected, result.trim())
    }

    @Test
    fun testRuntimeIf() {
        testRuntimeIf(4, 5, ">", "")
        testRuntimeIf(5, 4, ">", "1")
        testRuntimeIf(2, 2, "==", "1")
        testRuntimeIf(2, 1, "==", "")
    }

    private fun testRuntimeIf(a: Int, b: Int, op: String, expected: String) {
        val program = """
            fn main() {
                val x = $a;
                val y = readInt();
                if (x $op y) {
                    print("1");
                }
            }
        """
        val result = compileAndEval(program, userInput = b.toString())
        assertEquals(expected, result.trim())
    }

    @Test
    fun testMultiStatements() {
        Assertions.assertTimeout(Duration.ofSeconds(5), {
            testMultiStatements(1, "22")
            testMultiStatements(0, "30")
        })
    }

    private fun testMultiStatements(a: Int, expected: String) {
        val program = """
            fn main() {
                var x = readInt();
                var y = 1;
                if (x) {
                    x += 1;
                    y *= 2;
                } else {
                    x += 3;
                    y -= 1;
                }
                print(x);
                print(y);
            }
        """
        val result = compileAndEval(program, userInput = a.toString())
        assertEquals(expected, result.trim())
    }
}