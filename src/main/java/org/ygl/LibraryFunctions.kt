package org.ygl


typealias SymbolList = List<Symbol?>
typealias Procedure =  (SymbolList) -> Unit

class LibraryFunctions(val codegen: CodeGen, val tree: TreeWalker)
{
    val procedures: HashMap<String, Procedure> = hashMapOf(
            Pair("println", this::println)
    )

    private fun println(args: SymbolList) {
        args.filterNotNull().forEach {
            codegen.io.print(it)
            codegen.io.print("\n")
        }
    }

    fun invoke(name: String, args: SymbolList) {
        procedures[name]?.invoke(args) ?: throw Exception("undefined function $name")
    }

}