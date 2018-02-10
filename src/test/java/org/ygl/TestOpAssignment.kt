package org.ygl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


internal class TestOpAssignment
{
    @Test
    fun testRuntimeOp() {
        test(0, "+=", 1, 1)
        test(1, "+=", 2, 3)
        test(1, "-=", 1, 0)
        test(4, "-=", 2, 2)
    }

    private fun test(userInput: Int, op:String, b:Int, expected: Int) {
        val program = """
            fn main() {
                var x = readInt();
                x $op $b;
                print(x);
            }
        """
        val result = compileAndEval(program, userInput.toString())
        assertEquals(expected.toString(), result)
    }
}
