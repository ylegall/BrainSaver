package org.ygl.transformation

import org.ygl.CompileException
import org.ygl.ast.*
import org.ygl.runtime.ScopeContext
import org.ygl.runtime.Symbol
import org.ygl.runtime.UnknownSymbol

class CodeGen: AstWalker<Symbol>()
{
    private val scopeContext = ScopeContext()
    private val functions = mutableMapOf<String, AstNode>()

    override fun visit(node: ProgramNode): Symbol {
        registerFunctions(node)

        scopeContext.enterScope(node)
        node.children.filterIsInstance<GlobalVariableNode>()
                .forEach { global -> scopeContext.createSymbol(global) }

        return UnknownSymbol
    }

    private fun registerFunctions(node: ProgramNode) {
        node.children.filterIsInstance<FunctionNode>()
                .forEach {
                    if (it.name in functions) {
                        throw CompileException("function ${it.name} redefined")
                    }
                    functions.put(it.name, it)
                }
        if ("main" !in functions) throw CompileException("no main function found")
    }

    override fun defaultValue() = UnknownSymbol
}