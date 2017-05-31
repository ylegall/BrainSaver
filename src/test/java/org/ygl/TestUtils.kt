package org.ygl

import java.io.ByteArrayOutputStream

fun compileAndEval(input: String, userInput: String = "", wrapping: Boolean = true): String {
    return eval(compile(input), userInput)
}

fun compile(input: String): String {
    val outputStream = ByteArrayOutputStream()
    val options = CompilerOptions()
    outputStream.use { output ->
        compile(input, output, options = options)
    }
    val result = outputStream.toString()
    return result
}

fun eval(code: String, userInput: String = "", wrapping: Boolean = false): String {
    val options = InterpreterOptions(
            predefinedInput = userInput,
            wrap = wrapping
    )

    val outputStream = ByteArrayOutputStream()
    outputStream.use { output ->
        val interpreter = interpreter(code, output, options)
        interpreter.eval()
    }
    return outputStream.toString()
}

class TestContext(wrapping: Boolean = false)
{
    val output: ByteArrayOutputStream = ByteArrayOutputStream()
    val cg: CodeGen = buildCodegen(wrapping)

    private fun buildCodegen(wrapping: Boolean): CodeGen {
        val options = CompilerOptions(minify = true, wrapping = wrapping)
        val cg = CodeGen(output, options)
        cg.enterScope()
        return cg
    }

    fun eval(options: InterpreterOptions = DEFAULT_INTERPRETER_OPTIONS): Interpreter {
        cg.flush()
        output.flush()
        output.close()
        val str = output.toString()
        val interpreter = Interpreter(str, output, options)
        interpreter.eval()
        return interpreter
    }

}