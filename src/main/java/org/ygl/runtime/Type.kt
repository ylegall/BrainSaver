package org.ygl.runtime

sealed class Type

open class Number: Type()
class IntType: Number()
open class ArrayType: Type()
class StrType: ArrayType()

object UnknownType: Type()
