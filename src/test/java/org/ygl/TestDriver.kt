package org.ygl

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

fun compile(input: String): String {
    val inputStream = ByteArrayInputStream(input.toByteArray(StandardCharsets.UTF_8))
    val outputStream = ByteArrayOutputStream()
    org.ygl.compile(inputStream, outputStream)
    val result = outputStream.toString()
    return result
}

fun eval(code: String, userInput: String = ""): String {
    val options = if (!userInput.isEmpty()) {
        BFOptions(predefinedInput = userInput)
    } else {
        DEFAULT_OPTIONS
    }

    val outputStream = ByteArrayOutputStream()
    val interpreter = bfInterpreter(code, outputStream, options)
    interpreter.eval()
    return outputStream.toString()
}
