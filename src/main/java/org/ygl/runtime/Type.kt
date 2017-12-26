package org.ygl.runtime

import org.ygl.ast.AstNode
import org.ygl.ast.AtomIntNode
import org.ygl.ast.AtomStrNode

sealed class Type

open class Number: Type()
class IntType: Number()
open class ArrayType: Type()
class StrType: ArrayType()

object UnknownType: Type()

fun AstNode.getType(): Type {
    return when (this) {
        is AtomIntNode -> IntType()
        is AtomStrNode -> StrType()
        else -> UnknownType
    }
}