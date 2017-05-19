package org.ygl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

internal class TestArrays
{
    @Test
    fun testArrayInitializer() {

        fun test(a: Int, expected: Int) {
            val program = """
            fn main() {
                x = [1,2,3,4];
                print(x[$a]);
            }
        """
            val result = compileAndEval(program)
            Assertions.assertEquals(expected.toString(), result.trim())
        }

        test(0, 1)
        test(3, 4)
    }

    @Test
    fun testConstantArrays() {

        fun test(a: Int) {
            val idx = Random().nextInt(5)
            val program = """
            fn main() {
                x = array(5);
                x[$idx] = $a;
                print(x[$idx]);
            }
            """
            val result = compileAndEval(program)
            Assertions.assertEquals(a.toString(), result.trim())
        }

        test(0)
        test(3)
    }

    @Test
    fun testRuntimeArrays() {

        fun test(a: Int) {
            val idx = Random().nextInt(5)
            val program = """
            fn main() {
                x = array(5);
                readInt(y);
                x[$idx] = y;
                print(x[$idx]);
            }
        """
            val result = compileAndEval(program, userInput = a.toString())
            Assertions.assertEquals(a.toString(), result.trim())
        }

        test(0)
        test(6)
    }

}