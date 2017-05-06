package org.ygl

enum class Type {
    STRING,
    INT
}

data class Symbol(
    val name: String,
    val size: Int,
    val address: Int,
    val type: Type = Type.INT,
    var value: Any?
) : Comparable<Symbol>
{
    override fun toString(): String {
        return "$name{$address} |${value?:""}|"
    }

    override fun compareTo(other: Symbol) = address - other.address

    fun isConstant(): Boolean = value != null
}
