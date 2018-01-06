package org.ygl.ast

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.ygl.CompileException
import org.ygl.parse

internal class SemanticValidatorTest {

    @Test
    fun testRedeclaration() {
        val program = """var x = 0; var x = 1;"""
        val errors = validate(program)
        assertTrue(errors.isNotEmpty())
        assertEquals("duplicate declaration: x", errors[0].message)
    }

    @Test
    fun testReassignVal() {
        val program = """fn main() {val x = 0; x = 1;}"""
        val errors = validate(program)
        assertTrue(errors.isNotEmpty())
        assertEquals("val x cannot be reassigned", errors[0].message)
    }

    @Test
    fun testUndefinedIdentifier() {
        val program = """ fn main() { x = 0;}"""
        val errors = validate(program)
        assertTrue(errors.isNotEmpty())
        assertEquals("undefined identifier x", errors[0].message)
    }

    private fun validate(program: String): List<CompileException> {
        val validator = SemanticValidator()
        val ast = parse(program)
        return validator.validate(ast)
    }
}