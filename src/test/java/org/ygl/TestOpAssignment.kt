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

    private fun test(a: Int, op:String, b:Int, expected: Int) {
        val program = """
            fn main() {
                var x = readInt();
                x $op $b;
                debug(x);
            }
        """
        val options = InterpreterOptions(predefinedInput = a.toString())
        val interpreter = getInterpreter(program, options)
        interpreter.eval()
        assertEquals(expected, interpreter.getCellValue(1))
    }
}
