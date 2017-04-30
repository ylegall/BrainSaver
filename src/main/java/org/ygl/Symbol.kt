package org.ygl

enum class Type {
    STRING,
    INT
}

interface Addressable {
    fun address(): Int
    fun size(): Int
    var value: Any?
}

data class Symbol(
    val name: String,
    val size: Int,
    val address: Int,
    val type: Type = Type.INT,
    override var value: Any?
) : Addressable
{
    override fun address(): Int {
        return address
    }

    override fun size(): Int {
        return size
    }

    override fun toString(): String {
        return "$name{$address}"
    }

    fun isConstant(): Boolean = value != null
}

class Offset(val symbol: Symbol, val offset: Int = 0) : Addressable
{
    override var value: Any? = symbol.value

    override fun address(): Int {
        return symbol.address + offset
    }

    override fun size(): Int {
        return symbol.size
    }

    override fun toString(): String {
        return "offset(${symbol.name}{${symbol.address + offset}})"
    }
}