package org.ygl

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.InputStream
import java.io.OutputStream

class CompileOptions(
    val verbose: Boolean = false,
    val commentChar: String = ";;"
)

val DEFAULT_COMPILE_OPTIONS = CompileOptions()

fun compile(
        inputStream: InputStream,
        outputStream: OutputStream = System.out,
        compileOptions: CompileOptions = DEFAULT_COMPILE_OPTIONS
) {
    val lexer = BrainLoveLexer(CharStreams.fromStream(inputStream))
    val tokens = CommonTokenStream(lexer)
    val parser = BrainLoveParser(tokens)
    parser.addErrorListener(CompileErrorListener.INSTANCE)

    val tree = parser.program()
    CodeGen(outputStream, compileOptions).use {
        val visitor = BrainLoveVisitorImpl(it)
        visitor.visit(tree)
    }
}