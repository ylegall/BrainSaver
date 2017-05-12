package org.ygl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


internal class TestBranches
{
    @Test
    fun testIfElseBranch() {
        testIfElse(0, 0, "+", 2)
        testIfElse(0, 1, "+", 1)

        testIfElse(1, 1, "-", 2)
        testIfElse(2, 1, "-", 1)
        testIfElse(1, 2, "-", 2)

        testIfElse(5, 7, "<", 1)
        testIfElse(5, 7, ">", 2)

        testIfElse(6, 6, ">=", 1)
        testIfElse(8, 6, ">=", 1)

        testIfElse(4, 4, "==", 1)
        testIfElse(0, 0, "==", 1)
        testIfElse(3, 0, "==", 2)

        testIfElse(0, 2, "!=", 1)
        testIfElse(1, 2, "!=", 1)
    }

    fun testIfElse(a: Int, b: Int, op: String, expected: Int) {
        val program = """
            fn main() {
                x = $a;
                y = $b;
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
    fun testIfBranch() {
        testIf(4, 5, ">", "")
        testIf(5, 4, ">", "1")
        testIf(2, 2, "==", "1")
        testIf(2, 1, "==", "")
    }

    fun testIf(a: Int, b: Int, op: String, expected: String) {
        val program = """
            fn main() {
                x = $a;
                y = $b;
                if (x $op y) {
                    print("1");
                }
            }
        """
        val result = compileAndEval(program, userInput = b.toString())
        assertEquals(expected, result.trim())
    }
}