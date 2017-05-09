package org.ygl

import org.antlr.v4.runtime.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths


fun main(args: Array<String>) {

    if (args.size < 1) {
        println("missing input file")
        return
    }

    val lexer = BrainLoveLexer(ANTLRFileStream(args[0]) as CharStream)
    val tokens = CommonTokenStream(lexer)
    val parser = BrainLoveParser(tokens)
    //parser.errorHandler = BailErrorStrategy()
    parser.addErrorListener(CompileErrorListener.INSTANCE)

    try {
        val tree = parser.program()
        CodeGen(File("output.txt")).use {
            val visitor = BrainLoveVisitorImpl(it)
            visitor.visit(tree)
        }
    } catch (e: Exception) {
        print(e)
        return
    }

    /*
    println("\n\n=========")
    val interpreter = Interpreter()
    val code = String(Files.readAllBytes(Paths.get("output.txt")))
    println(interpreter.eval(code))
    */
}