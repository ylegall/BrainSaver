package org.ygl.runtime

import org.ygl.runtime.Symbol.NullSymbol

//private val NullFunction = { x -> UnknownSymbol }

private typealias Params = Iterable<Symbol>
private typealias Procedure = (p: Params) -> Symbol

/**
 *
 */
class StdLib(
        private val cg: CodeGen,
        private val runtime: Runtime
)
{

    val functions = mapOf(
            "readInt" to { _: Params -> readInt() },
            "print"   to { args: Params -> print(args) },
            "println" to { args: Params -> println(args) },
            "debug"   to { args: Params -> debug(args) }
    )

    fun invoke(name: String, args: Params): Symbol {
        val fn = functions[name]!!
        return fn.invoke(args)
    }

    private fun print(args: Params): Symbol {
        args.forEach { cg.io.print(it) }
        return NullSymbol
    }

    private fun println(args: Params): Symbol {
        args.forEach {
            cg.io.print(it)
            cg.io.printImmediate("\n")
        }
        return NullSymbol
    }

    private fun readInt(): Symbol {
        val ret = runtime.createTempSymbol()
        with (cg) {
            commentLine("read int $ret")
            moveTo(ret)
            emit(",")
            emit("-".repeat(48), "convert char to int")
        }
        return ret
    }

    private fun debug(args: Params): Symbol {
        args.forEach { cg.debug(it, "${it.name}=") }
        return NullSymbol
    }
}