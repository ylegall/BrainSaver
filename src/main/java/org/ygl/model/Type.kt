package org.ygl.model

sealed class Type

object IntType: Type()

object StrType: Type()

object NullType: Type()

fun getType(value: Any): Type {
    return when (value) {
        is Int -> IntType
        is String -> StrType
        else -> NullType
    }
}