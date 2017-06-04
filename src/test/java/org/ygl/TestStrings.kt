package org.ygl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


internal class TestStrings
{
    @Test
    fun testConstant() {
        val program = """
            fn main() {
                x = "hello";
                print(x);
            }
        """
        val code = compile(program)
        val result = eval(code)
        Assertions.assertEquals("hello", result.trim())
    }

    @Test
    fun testIndex() {
        val program = """
            fn main() {
                x = "save";
                x[2] = "n";
                print(x);
            }
        """
        val code = compile(program)
        val result = eval(code)
        Assertions.assertEquals("sane", result.trim())
    }

    @Test
    fun testAssign() {
        val program = """
            fn main() {
                x = "yes";
                y = x;
                x = 4;
                println(y);
                print(x);
            }
        """
        val code = compile(program)
        val result = eval(code)
        Assertions.assertEquals("yes\n4", result.trim())
    }

    @Test
    fun testRuntimeRead() {
        val program = """
            fn main() {
                x = readStr(4);
                x[2] = "n";
                print(x);
            }
        """
        val code = compile(program)
        val result = eval(code, userInput = "five")
        Assertions.assertEquals("fine", result.trim())
    }

//    @Test()
//    fun testRuntimeConditionalRead() {
//        val program = """
//            fn main() {
//                readInt(c);
//                x = readStr(4);
//                if (c) {
//                    x[2] = "n";  // fine
//                    debug(x);
//                } else {
//                    x[2] = "l";  // file
//                    debug(x);
//                }
//                print(x);
//            }
//        """
//        val code = compile(program)
//        val result = eval(code, userInput = "1five")
//        Assertions.assertEquals("fine", result.trim())
//    }
}