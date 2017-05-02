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
)
{
    override fun toString(): String {
        return "$name{$address} |${value?:""}|"
    }

    fun isConstant(): Boolean = value != null
}
