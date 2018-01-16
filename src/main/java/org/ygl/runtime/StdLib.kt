package org.ygl.runtime

import org.ygl.ast.EmptyNode

typealias Proceedure = (Iterable<Symbol>) -> ConstantSymbol

private val NullFunction = Function("", EmptyNode, emptyList())

/**
 *
 */
class StdLib(
        private val cg: CodeGen,
        private val runtime: Runtime
)
{
    val functions = mapOf(
            "println" to NullFunction,
            "readInt" to NullFunction
    )
}