package org.ygl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TestAssignment
{

    @Test
    fun testConstantAssignment() {
        val program = """
            var g1 = 7;
            fn main() {
                val x = g1;
                val y = x;
                debug(y);
            }
        """
        val interpreter = getInterpreter(program)
        interpreter.eval()
        assertEquals(7, interpreter.getCellValue(0))
        assertEquals(7, interpreter.getCellValue(1))
        assertEquals(7, interpreter.getCellValue(2))
    }

    @Test
    fun testRuntimeAssignment() {
        val program = """
            fn main() {
                var y = 4;
                print(y);
                val x = readInt();
                y = x;
                print(y);
            }
        """
        val result = compileAndEval(program, "2")
        assertEquals("42", result)
    }
}
