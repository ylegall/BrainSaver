package org.ygl

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.InputStream
import java.io.OutputStream



fun compile(
        inputStream: InputStream,
        outputStream: OutputStream = System.out,
        compileOptions: CompilerOptions = DEFAULT_COMPILE_OPTIONS
) {
    val lexer = BrainSaverLexer(CharStreams.fromStream(inputStream))
    val tokens = CommonTokenStream(lexer)
    val parser = BrainSaverParser(tokens)
    parser.addErrorListener(CompileErrorListener.INSTANCE)

    val tree = parser.program()
    CodeGen(outputStream, compileOptions).use {
        val visitor = TreeWalker(it)
        visitor.visit(tree)
    }
}