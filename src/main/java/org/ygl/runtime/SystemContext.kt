package org.ygl.runtime

import org.ygl.CompilerOptions
import org.ygl.DEFAULT_COMPILE_OPTIONS
import java.io.OutputStream

/**
 *
 */
class SystemContext(
        val outputStream: OutputStream,
        val options: CompilerOptions = DEFAULT_COMPILE_OPTIONS
) {
    val runtime = Runtime()
    val cg = CodeGen(outputStream, options, runtime)
    val stdlib = StdLib(cg, runtime)
}