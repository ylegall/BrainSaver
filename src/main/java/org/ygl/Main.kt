package org.ygl

import org.antlr.v4.runtime.*
import java.io.File
import java.io.FileOutputStream


fun main(args: Array<String>) {

    if (args.size < 1) {
        println("missing input file")
        return
    }

    val lexer = BrainSaverLexer(CharStreams.fromFileName(args[0]))
    val tokens = CommonTokenStream(lexer)
    val parser = BrainSaverParser(tokens)
    parser.addErrorListener(CompileErrorListener.INSTANCE)

    val options = CompileOptions(verbose = true)

    try {
        val tree = parser.program()
        CodeGen(FileOutputStream(File("output.txt")), options).use {
            val visitor = TreeWalker(it)
            visitor.visit(tree)
        }
    } catch (e: Exception) {
        print(e)
        return
    }

    println("\n\n___________________")
    //val interpreter = Interpreter()
    //val code = String(Files.readAllBytes(Paths.get("output.txt")))
    //println(interpreter.eval(code))
    val interpreter = bfInterpreter(File("output.txt"), options= InterpreterOptions(isVerbose = true))
    interpreter.eval()
}