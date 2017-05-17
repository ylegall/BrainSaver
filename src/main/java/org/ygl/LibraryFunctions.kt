package org.ygl


typealias ExpList = List<BrainStoolParser.ExpContext>
typealias Procedure =  (ExpList) -> Unit

class LibraryFunctions(val codegen: CodeGen)
{
    val procedures: HashMap<String, Procedure> = hashMapOf(
            Pair("readInt", this::readInt),
            Pair("read", this::read)
    )

    private fun readInt(args: ExpList) {
        args.forEach {
            if (it is BrainStoolParser.AtomIdContext) {
                val id = it as BrainStoolParser.AtomIdContext
                val sym = codegen.currentScope().getOrCreateSymbol(id.text, type = Type.INT)
                codegen.io.readInt(sym)
            } else {
                throw Exception("invalid argument to readInt(): ${it.text}")
            }
        }
    }

    private fun read(args: ExpList) {
        args.forEach {
            val sym = codegen.currentScope().getOrCreateSymbol(it.text, type = Type.INT)
            codegen.io.readChar(sym)
        }
    }

    fun invokeProcedure(name: String, args: ExpList) {
        val function = procedures[name]!!
        function.invoke(args)
    }

}