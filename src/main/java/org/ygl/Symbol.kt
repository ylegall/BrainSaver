package org.ygl

enum class Type {
    STRING,
    INT
}

data class Symbol(
    var name: String,
    val size: Int,
    val address: Int,
    val type: Type = Type.INT,
    var value: Any?
) : Comparable<Symbol>
{
    override fun toString(): String {
        val addr = if (size > 1) "$address~${address + size - 1}" else "$address"
        val contents = if (value != null && value !is String) "=$value" else ""
        return "$name{$addr}$contents"
    }

    override fun compareTo(other: Symbol) = address - other.address
}

fun Symbol.offset(offset: Int, name: String = this.name): Symbol {
    val address = if (this.type == Type.INT && this.isArray()) {
        this.address + 4 + offset
    } else {
        this.address + offset
    }
    return Symbol("$name($offset)", 1, address, this.type, this.value)
}

fun Symbol.isArray(): Boolean = this.size > 1

fun Symbol.isTemp(): Boolean = this.name.startsWith("$")