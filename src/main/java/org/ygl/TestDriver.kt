package org.ygl

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


fun main(args: Array<String>) {

    val compilerOptions = CompilerOptions(
            minify = false,
            optimize = true,
            output = "output.txt"
    )

    val outputFile = File("output.txt")
    val inStream  = FileInputStream(File("input.txt"))
    val outStream = TeeOutputStream(FileOutputStream(outputFile))

    inStream.use { input ->
        outStream.use { output ->
            try {
                compile(input, output, compilerOptions)
            } catch (e: CompilationException) {
                System.err.println(e.message)
                return
            }
        }
    }

    //println(String(Files.readAllBytes(outputFile.toPath())))

    println("\n______________")

    val evalOptions = InterpreterOptions(
            debug = true,
            predefinedInput = "1"
    )

    eval(outputFile, options = evalOptions)
}