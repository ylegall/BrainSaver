package org.ygl

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class TestArrays
{
    @Test
    fun testArrayInitializer() {
        fun test(a: Int, expected: Int) {
            val program = """
            fn main() {
                val x = [1,2,3,4];
                debug(x[$a]);
            }"""
            val interpreter = getInterpreter(program)
            interpreter.eval()
            assertEquals(expected, interpreter.getCellValue(a + 4))
        }

        test(0, 1)
        test(3, 4)
    }

    // TODO: dead stores is removing 1st line
    @Test
    fun testConstantArrays() {
        fun test(a: Int, idx: Int) {
            val program = """
            fn main() {
                val x = array(5);
                x[$idx] = $a;
                debug(x[$idx]);
            }"""
            val interpreter = getInterpreter(program)
            interpreter.eval()
            assertEquals(a, interpreter.getCellValue(idx + 4))
        }

        test(3, 0)
        test(3, 3)
    }

    @Test
    fun testRuntimeArrays() {

        fun test(a: Int, idx: Int) {
            val program = """
            fn main() {
                val x = array(5);
                val y = readInt();
                x[$idx] = y;
                debug(x[$idx]);
            }"""
            val interpreter = getInterpreter(program, InterpreterOptions(predefinedInput = a.toString()))
            interpreter.eval()
            assertEquals(a, interpreter.getCellValue(idx + 4))
        }

        test(1, 0)
        test(2, 2)
        test(1, 4)
    }

}