package org.ygl

import org.antlr.v4.runtime.tree.xpath.XPath
import org.ygl.BrainSaverParser.*

fun resolveGlobals(parser: BrainSaverParser, tree: ProgramContext): Map<String, Symbol> {

    val globals = HashMap<String, Symbol>()
    val finalGlobals = HashMap<String, Symbol>()
    var address = 0

    val readSymbols = XPath.findAll(tree, "//atom", parser)
            .mapNotNull { it as? AtomIdContext }
            .map { it.text }
            .toCollection(HashSet())

    XPath.findAll(tree, "//globalVariable", parser)
            .mapNotNull { ctx -> ctx as? GlobalVariableContext }
            .forEach { ctx ->
                val name = ctx.Identifier().text
                if (globals.containsKey(name)) throw CompileException("global symbol $name redefined", ctx)
                when (ctx.rhs) {
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
            throw CompileException("unused global variable $name")
        }
        finalGlobals.put(name, symbol)
    }

    return finalGlobals
}