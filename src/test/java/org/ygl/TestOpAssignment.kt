package org.ygl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


internal class TestOpAssignment
{
    // TODO
    @Test
    fun testRuntimeOp() {
        test(0, "+=", 1, 1)
        test(1, "+=", 2, 3)
        test(1, "-=", 1, 0)
        test(4, "-=", 2, 2)
    }

    private fun test(a: Int, op:String, b:Int, expected: Int) {
        val program = """
            fn main() {
                readInt(x);
                x $op $b;
                print(x);
            }
        """
        val code = compile(program)
        val result = eval(code, userInput = a.toString())
        Assertions.assertEquals(expected.toString(), result.trim())
    }
}
