package org.ygl

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.ygl.runtime.Scope

internal class TestScope {

    private val scope = Scope(0)

    @Test
    fun testSimpleAllocation() {
        val t1 = scope.createTempSymbol(size = 1)
        assertEquals(1, scope.headPointer)
        val t2 = scope.createTempSymbol(size = 1)
        assertEquals(2, scope.headPointer)
        scope.delete(t2)
        assertEquals(1, scope.headPointer)
        scope.delete(t1)
        assertEquals(0, scope.headPointer)
    }

    @Test
    fun testMemoryReuse() {
        val t1 = scope.createTempSymbol(size = 1)
        scope.createTempSymbol(size = 1)
        assertEquals(2, scope.headPointer)
        scope.delete(t1)
        assertEquals(2, scope.headPointer)
        scope.createTempSymbol(size = 1)
        assertEquals(2, scope.headPointer)
    }

    @Test
    fun testArrayMemoryReuse() {
        val t1 = scope.createTempSymbol(size = 3)
        scope.createTempSymbol(size = 2)
        assertEquals(5, scope.headPointer)
        scope.delete(t1)
        val t3 = scope.createTempSymbol(size = 2)
        val t4 = scope.createTempSymbol(size = 1)
        assertEquals(5, scope.headPointer)
        assertEquals(0, t3.address)
        assertEquals(2, t4.address)
    }

    @Test
    fun testArrayMemoryReuseAndIncrease() {
        scope.createTempSymbol(size = 3)
        val t2 = scope.createTempSymbol(size = 2)
        val t3 = scope.createTempSymbol(size = 2)
        assertEquals(7, scope.headPointer)
        scope.delete(t2)
        assertEquals(7, scope.headPointer)
        scope.delete(t3)
        assertEquals(5, scope.headPointer)
        val t4 = scope.createTempSymbol(size = 3)
        assertEquals(5, t4.address)
        assertEquals(8, scope.headPointer)
    }

}