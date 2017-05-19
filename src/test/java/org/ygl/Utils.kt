package org.ygl

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

fun compileAndEval(input: String, userInput: String = ""): String {
    return eval(compile(input), userInput)
}

fun compile(input: String): String {
    val inputStream = ByteArrayInputStream(input.toByteArray(StandardCharsets.UTF_8))
    val outputStream = ByteArrayOutputStream()
    org.ygl.compile(inputStream, outputStream)
    val result = outputStream.toString()
    return result
}

fun eval(code: String, userInput: String = ""): String {
    val options = if (!userInput.isEmpty()) {
        InterpreterOptions(predefinedInput = userInput)
    } else {
        DEFAULT_OPTIONS
    }

    val outputStream = ByteArrayOutputStream()
    val interpreter = bfInterpreter(code, outputStream, options)
    interpreter.use { it.eval() }
    return outputStream.toString()
}
