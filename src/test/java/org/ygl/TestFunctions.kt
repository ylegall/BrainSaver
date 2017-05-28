package org.ygl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TestFunctions
{

    @Test
    fun testComplexCall() {
        val program = """
            fn bar(z) {
                readInt(x);
                return x + z;
            }

            fn foo() {
                x = 3 + bar(2);
                return x;
            }

            fn main() {
                x = foo() + bar(5);
                print(x);
            }
        """
        val result = compileAndEval(program, userInput = "44")
        assertEquals("18", result.trim())
    }

    @Test
    fun testNestedCall() {
        val program = """
            fn bar() {
                return 3;
            }

            fn foo() {
                x = bar();
                return x;
            }

            fn main() {
                x = foo();
                print(x);
            }
        """
        val result = compileAndEval(program)
        assertEquals("3", result.trim())
    }

    @Test
    fun testSimpleParams() {
        val program = """
            fn foo(x, y) {
                return x * y;
            }

            fn main() {
                x = foo(3, 4);
                print(x);
            }
        """
        val result = compileAndEval(program)
        assertEquals("12", result.trim())
    }

    @Test
    fun testSimpleReturn() {
        val program = """
            fn foo() {
                return 5;
            }

            fn main() {
                x = 3 + foo();
                print(x);
            }
        """
        val result = compileAndEval(program)
        assertEquals("8", result.trim())
    }

    @Test
    fun testLoopParams() {
        val program = """
            fn isEven(x) {
                ret = 0;
                if ((x % 2) == 0) {
                    ret = 1;
                } else {
                    ret = 0;
                }
                return ret;
            }

            fn main() {
                i = 1;
                while (i <= 6) {
                    if (isEven(i)) {
                        print(i);
                    }
                    i += 1;
                }
            }
        """
        val result = compileAndEval(program)
        assertEquals("246", result.trim())
    }
}