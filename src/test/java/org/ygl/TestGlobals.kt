package org.ygl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.Exception

internal class TestGlobals {

    @Test
    fun testReadGlobals() {

        fun test(a: Int, expected: Int) {
            val program = """
            var g = 3;
            fn main() {
                val x = g + $a;
                print(x);
            }"""
            val result = compileAndEval(program)
            Assertions.assertEquals(expected.toString(), result.trim())
        }

        test(0, 3)
        test(3, 6)
    }

    @Test
    fun testWriteGlobals() {

        fun test(a: Int, op: String, expected: Int) {
            val program = """
            var g = 3;
            fn main() {
                g $op $a;
                print(g);
            }"""
            val result = compileAndEval(program)
            Assertions.assertEquals(expected.toString(), result.trim())
        }

        test(1, "+=", 4)
        test(2, "-=", 1)
    }


    @Test
    fun testConstantFold() {

        fun test(expected: Int) {
            val program = """
            var g = 3;
            fn foo() {g += 1;}
            fn main() {
                foo();
                val x = g + 1;
                print(x);
            }"""
            val result = compileAndEval(program)
            Assertions.assertEquals(expected.toString(), result.trim())
        }
        test(5)
    }
}