package org.ygl.runtime

typealias Proceedure = (Iterable<Symbol>) -> Symbol

class StdLib(
        private val cg: CodeGen,
        private val runtime: Runtime
)
{
    val functions = mapOf<String, Proceedure>(

    )
}