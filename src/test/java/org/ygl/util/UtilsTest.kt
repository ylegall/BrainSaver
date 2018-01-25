package org.ygl.util

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class UtilsTest {

    @Test
    fun testOrElse() {
        val str1: String? = null
        val str2: String? = "hello"
        assertEquals("hello", str1.orElse { str2 })
        assertEquals("hello", str2.orElse { str1 })
        assertEquals(3, null.orElse { 1 + 2 })
        assertEquals(null, null.orElse { null })
    }
}