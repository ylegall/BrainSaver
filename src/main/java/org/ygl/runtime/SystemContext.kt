package org.ygl.runtime

import org.ygl.CompilerOptions
import org.ygl.DEFAULT_COMPILE_OPTIONS
import org.ygl.ast.AstNode
import org.ygl.util.MinifyingOutputStream
import java.io.OutputStream

/**
 *
 */
class SystemContext(
        outputStream: OutputStream,
        val options: CompilerOptions = DEFAULT_COMPILE_OPTIONS
): AutoCloseable {
    val output = if (options.minify) {
        MinifyingOutputStream(outputStream, options.margin)
    } else {
        outputStream
    }

    val runtime = Runtime()
    val cg = CodeGen(output, options, runtime)
    val stdlib = StdLib(cg, runtime)

    var lastUseInfo: Map<AstNode, Set<String>> = mapOf()

    override fun close() {
        output.close()
    }
}