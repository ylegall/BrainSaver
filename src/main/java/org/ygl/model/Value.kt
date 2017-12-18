package org.ygl.model

sealed class Value {
    abstract val value: Any
}

data class IntValue(
        override val value: Int
) : Value()

data class StrValue(
        override val value: String
) : Value()
