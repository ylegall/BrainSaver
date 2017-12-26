package org.ygl.model

import org.ygl.runtime.IntType
import org.ygl.runtime.StrType
import org.ygl.runtime.Type
import org.ygl.runtime.UnknownType

/**
 *
 */
sealed class Value {
    abstract val value: Any
    open fun getType(): Type = UnknownType
    open fun getSize(): Int = 0
}

data class IntValue(
        override val value: Int
) : Value() {
    override fun getType() = IntType()
    override fun getSize() = 1
}

data class StrValue(
        override val value: String
) : Value() {
    override fun getType() = StrType()
    override fun getSize() = value.length
}

object NullValue : Value() {
    override val value: Any
        get() = throw Exception("null value")

    override fun getType() = UnknownType
}

