package org.ygl

import org.antlr.v4.runtime.misc.ParseCancellationException


typealias SymbolList = List<Symbol?>
typealias Procedure =  (SymbolList) -> Symbol?

class LibraryFunctions(val cg: CodeGen, val tree: TreeWalker)
{
    val procedures: HashMap<String, Procedure> = hashMapOf(
            "println" to this::println,
            "readStr" to this::readStr
    )

    private fun println(args: SymbolList): Symbol? {
        if (args.isEmpty()) {
            cg.io.print("\n")
        } else {
            args.filterNotNull().forEach {
                if (tree.isConstant(it)) {
                    cg.io.print(it.value)
                } else {
                    cg.io.print(it)
                }
                cg.io.print("\n")
            }
        }
        return null
    }

    private fun readStr(args: SymbolList): Symbol? {
        if (args.size != 1 || !tree.isConstant(args[0]!!)) {
            throw ParseCancellationException("function readStr accepts 1 constant integer argument")
        }
        val arg = args[0]!!
        val len = arg.value as Int
        val str = cg.currentScope().getTempSymbol(type = Type.STRING, size = len)
        for (i in 0 until len) {
            cg.io.readChar(str.offset(i))
        }
        return str
    }

    fun invoke(name: String, args: SymbolList): Symbol? {
        val fn = procedures[name] ?: throw Exception("undefined function $name")
        return fn.invoke(args)
    }

}