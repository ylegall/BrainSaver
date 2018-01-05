package org.ygl.ast

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.ygl.parse

internal class SemanticValidatorTest {

    val validator = SemanticValidator()

    @Test
    fun testRedeclaration() {
        val program = """var x = 0; var x = 1;"""

        val ast = parse(program)
        val errors = validator.validate(ast)
        assertTrue(errors.isNotEmpty())
        assertTrue(errors[0].message == "duplicate declaration: x")
    }

}