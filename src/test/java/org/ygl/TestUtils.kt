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
    val options = CompilerOptions(output = outputStream)
    compile(inputStream, options)
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
