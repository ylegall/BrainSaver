package org.ygl.runtime

/**
 *
 */
class Scope<out NodeType, SymbolType>(
        val node: NodeType
)
{
    val symbols = mutableMapOf<String, SymbolType>()
}
