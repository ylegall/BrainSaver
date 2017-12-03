package org.ygl

import org.antlr.v4.runtime.tree.xpath.XPath
import org.ygl.BrainSaverParser.*

/**
 * Parses and validates global symbols
 */
class GlobalVisitor(
        private val parser: BrainSaverParser,
        private val tree: ProgramContext
) : BrainSaverBaseVisitor<Unit>()
{

    // TODO: factor out into common code:
    private fun getReadSymbols(): HashSet<String> {
        return XPath.findAll(tree, "//atom", parser)
                .mapNotNull { it as? AtomIdContext }
                .map { it.text }
                .toCollection(HashSet())
    }

    fun getGlobals(): Set<Symbol> {

        var address = 0
        val readSymbols = getReadSymbols()
        val globals = HashMap<String, Symbol>()
        val finalGlobals = HashSet<Symbol>()

        XPath.findAll(tree, "//globalVariable", parser)
                .mapNotNull { ctx -> ctx as? GlobalVariableContext }
                .forEach { ctx ->
                    val name = ctx.Identifier().text
                    if (globals.containsKey(name)) throw Exception("global symbol $name redefined")
                    when (ctx.atom()) {
                        is BrainSaverParser.AtomIntContext -> {
                            globals[name] = Symbol(name, 1, address, Type.INT, ctx.rhs.text.toInt())
                            address += 1
                        }
                        is BrainSaverParser.AtomStrContext -> {
                            globals[name] = Symbol(name, 1, address, Type.STRING, ctx.rhs.text)
                            address += ctx.rhs.text.length
                        }
                    }
                }

        for ((name, symbol) in globals) {
            if (!readSymbols.contains(name)) {
                throw Exception("unused global variable $name")
            }
            finalGlobals.add(symbol)
        }

        return finalGlobals
    }

}