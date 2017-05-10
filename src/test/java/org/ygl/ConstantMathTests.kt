package org.ygl

import org.junit.jupiter.api.*
import kotlin.test.assertEquals

/**
 */
internal class ConstantMathTests {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
        }
    }

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun testConstantAddition() {
        test(2, 3, "+", 5)
        test(0, 0, "+", 0)
    }

    @Test
    fun testConstantSubtraction() {
        test(5, 5, "-", 0)
        test(8, 5, "-", 3)
        test(5, 8, "-", 0)
    }

    @Test
    fun testConstantMultiplication() {
        test(5, 5, "*", 25)
        test(0, 4, "*", 0)
        test(1, 4, "*", 4)
        test(4, 1, "*", 4)
    }

    @Test
    fun testConstantDivision() {
        test(5, 5, "/", 1)
        test(5, 2, "/", 2)
        test(1, 2, "/", 0)
        test(4, 1, "/", 4)
    }

    @Test
    fun testConstantMod() {
        test(5, 5, "%", 0)
        test(5, 2, "%", 1)
        test(11, 7, "%", 4)
        test(4, 1, "%", 0)
    }

    fun test(a: Int, b: Int, op: String, expected: Int) {
        val program = """
            fn main() {
                x = $a $op $b;
                print(x);
            }
        """
        val code = compile(program)
        val result = eval(code)
        assertEquals(expected.toString(), result.trim())
    }

}