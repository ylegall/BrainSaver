package org.ygl

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


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

        val outputStream = if (commandLine.hasOption("output")) {
            FileOutputStream(File(commandLine.getOptionValue("output")))
        } else {
            System.`out`
        }

        CodeGen(outputStream, compilerOptions).use {
            val visitor = TreeWalker(it)
            visitor.visit(tree)
        }

    } catch (e: ParseException) {
        printUsageAndHalt(options)
    } catch (e: ParseCancellationException) {
        println(e)
        System.exit(1)
    }

    val interpreter = BFInterpreter(inputStream = FileInputStream(File("output.txt")))
    interpreter.use { it.eval() }

}