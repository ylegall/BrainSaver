package org.ygl

import org.antlr.v4.runtime.ANTLRFileStream
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
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
    val tree = parser.program()

    CodeGen(File("output.txt")).use {
        val visitor = BrainLoveVisitorImpl(it)
        visitor.visit(tree)
    }

    println("\n\n=========")

    val interpreter = Interpreter()
    val code = String(Files.readAllBytes(Paths.get("output.txt")))
    println(interpreter.eval(code))
}