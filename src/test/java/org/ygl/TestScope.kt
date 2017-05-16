package org.ygl

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class TestScope {

    private val scope = Scope(startAddress = 3)
    private val initialSize = scope.scopeSize
    private val startAddress = scope.startAddress + initialSize

    @Test
    fun testSimpleAllocation() {
        val t1 = scope.getTempSymbol(size = 1)
        assertEquals(initialSize + 1, scope.scopeSize)
        val t2 = scope.getTempSymbol(size = 1)
        assertEquals(initialSize + 2, scope.scopeSize)
        scope.delete(t2)
        assertEquals(initialSize + 1, scope.scopeSize)
        scope.delete(t1)
        assertEquals(initialSize, scope.scopeSize)
    }

    @Test
    fun testMemoryReuse() {
        val t1 = scope.getTempSymbol(size = 1)
        val t2 = scope.getTempSymbol(size = 1)
        assertEquals(initialSize + 2, scope.scopeSize)
        scope.delete(t1)
        assertEquals(initialSize + 2, scope.scopeSize)
        val t3 = scope.getTempSymbol(size = 1)
        assertEquals(initialSize + 2, scope.scopeSize)
    }

    @Test
    fun testArrayMemoryReuse() {
        val t1 = scope.getTempSymbol(size = 3)
        val t2 = scope.getTempSymbol(size = 2)
        assertEquals(initialSize + 5, scope.scopeSize)
        scope.delete(t1)
        val t3 = scope.getTempSymbol(size = 2)
        val t4 = scope.getTempSymbol(size = 1)
        assertEquals(initialSize + 5, scope.scopeSize)
        assertEquals(startAddress, t3.address)
        assertEquals(startAddress + 2, t4.address)
    }

}