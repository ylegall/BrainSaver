package org.ygl

import org.ygl.ast.AstBuilder
import org.ygl.ast.AstNode
import org.ygl.runtime.SystemContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

fun compileAndEval(input: String, userInput: String = "", wrapping: Boolean = true): String {
    return eval(compile(input), userInput)
}

fun compile(input: String): String {
    val outputStream = ByteArrayOutputStream()
    val options = CompilerOptions(minify = true)
    outputStream.use { output ->
        compile(input, output, options)
    }
    return outputStream.toString()
}

fun eval(code: String, userInput: String = "", wrapping: Boolean = false): String {
    if (code.isBlank()) return ""

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

fun parse(code: String): AstNode {
    val inputStream = ByteArrayInputStream(code.toByteArray(Charsets.UTF_8))
    return AstBuilder().visit(parse(inputStream))
}

fun getInterpreter(
        code: String,
        options: InterpreterOptions = DEFAULT_INTERPRETER_OPTIONS
): Interpreter {
    val compiled = compile(code)
    return Interpreter(str = compiled, options = options)
}

fun testContext(wrapping: Boolean = false): SystemContext {
    return SystemContext(ByteArrayOutputStream(), CompilerOptions(wrapping = wrapping, minify = true))
}
