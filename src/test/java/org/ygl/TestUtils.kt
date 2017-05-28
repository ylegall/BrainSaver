package org.ygl

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

fun compileAndEval(input: String, userInput: String = "", wrapping: Boolean = true): String {
    return eval(compile(input), userInput)
}

fun compile(input: String): String {
    val inputStream = ByteArrayInputStream(input.toByteArray(StandardCharsets.UTF_8))
    val outputStream = ByteArrayOutputStream()
    val options = CompilerOptions()
    compile(inputStream, outputStream, options = options)
    val result = outputStream.toString()
    return result
}

fun eval(code: String, userInput: String = "", wrapping: Boolean = false): String {
    val options = InterpreterOptions(
            predefinedInput = userInput,
            isWrapping = wrapping
    )

    val outputStream = ByteArrayOutputStream()
    val interpreter = bfInterpreter(code, outputStream, options)
    interpreter.use { it.eval() }
    return outputStream.toString()
}


class TestContext(wrapping: Boolean = false)
{
    val output: ByteArrayOutputStream = ByteArrayOutputStream()
    val cg: CodeGen = buildCodegen(output, wrapping)

    private fun buildCodegen(outputStream: ByteArrayOutputStream, wrapping: Boolean): CodeGen {
        val options = CompilerOptions(minify = true, wrapping = wrapping)
        val cg = CodeGen(outputStream, options)
        cg.enterScope()
        return cg
    }

    fun eval(options: InterpreterOptions = DEFAULT_OPTIONS): BFInterpreter {
        cg.close()
        val str = output.toString()
        val interpreter = bfInterpreter(str, output, options)
        interpreter.eval()
        return interpreter
    }

}