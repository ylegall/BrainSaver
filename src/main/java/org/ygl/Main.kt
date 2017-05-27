package org.ygl

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.apache.commons.cli.*
import java.io.File
import java.io.FileOutputStream

const val VERSION = "1.0"

private inline fun printUsageAndHalt(options: Options) {
    HelpFormatter().printHelp("brainsaver", options, true)
    System.exit(1)
}

fun main(args: Array<String>) {

    val options = configureCommandLine()

    try {
        val commandLine = DefaultParser().parse(options, args)
        if (commandLine.hasOption("version")) {
            println("brainsaver version \"$VERSION\"")
            return
        }
        val remainingArgs = commandLine.argList
        if (remainingArgs.isEmpty()) {
            printUsageAndHalt(options)
        }

        // parse and generate the AST:
        val lexer = BrainSaverLexer(CharStreams.fromFileName(remainingArgs[0]))
        val tokens = CommonTokenStream(lexer)
        val parser = BrainSaverParser(tokens)
        parser.addErrorListener(CompileErrorListener.INSTANCE)
        val tree = parser.program()

        val compilerOptions = CompilerOptions(
                verbose = !commandLine.hasOption("minify"),
                optimize = !commandLine.hasOption("no-cf")
        )

        var outputStream = if (commandLine.hasOption("output")) {
            FileOutputStream(File(commandLine.getOptionValue("output")))
        } else {
            System.`out`
        }

        if (commandLine.hasOption("minify")) {
            val margin = if (commandLine.hasOption("margin"))
                commandLine.getOptionValue("margin").toInt()
            else
                0
            outputStream = MinifyingOutputStream(outputStream, margin)
        }

        CodeGen(outputStream, compilerOptions).use {
            val visitor = TreeWalker(it)
            visitor.visit(tree)
        }

    } catch (e: ParseException) {
        printUsageAndHalt(options)
    } catch (e: ParseCancellationException) {
        println(e.message)
        System.exit(1)
    }

}