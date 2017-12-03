package org.ygl

import org.ygl.BrainSaverParser.AtomIntContext
import org.ygl.BrainSaverParser.AtomStrContext

/**
 * Parses and validates global symbols
 */
class GlobalVisitor : BrainSaverBaseVisitor<Unit>() {

    private val globals = HashMap<String, Symbol>()
    private val readSymbols = HashSet<String>()

    private var address = 0

    fun getGlobals(tree: BrainSaverParser.ProgramContext): Set<Symbol> {
        val finalGlobals = HashSet<Symbol>()
        visit(tree)
        for ((name, symbol) in globals) {
            if (!readSymbols.contains(name)) {
                throw Exception("unused global variable $name")
            }
            finalGlobals.add(symbol)
        }
        return finalGlobals
    }

    /**
     *
     */
    override fun visitGlobalConstant(ctx: BrainSaverParser.GlobalConstantContext?) {
        ctx ?: throw Exception("null GlobalConstantContext")
        val name = ctx.Identifier().text
        if (globals.containsKey(name)) throw Exception("global symbol $name redefined")
        if (ctx.rhs.text != name) {
            when (ctx.atom()) {
                is AtomIntContext -> {
                    globals[name] = Symbol(name, 1, address, Type.INT, ctx.rhs.text.toInt())
                    address += 1
                }
                is AtomStrContext -> {
                    globals[name] = Symbol(name, 1, address, Type.STRING, ctx.rhs.text)
                    address += ctx.rhs.text.length
                }
            }
        }
    }

    override fun visitAtomId(ctx: BrainSaverParser.AtomIdContext?) {
        ctx ?: throw Exception("null AtomIdContext")
        readSymbols.add(ctx.text)
    }

}