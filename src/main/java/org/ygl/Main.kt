package org.ygl

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream

const val VERSION = "1.0"

private inline fun printUsageAndHalt(options: Options) {
    HelpFormatter().printHelp("brainsaver", options, true)
    System.exit(1)
}

fun compile(input: InputStream, outputStream: OutputStream? = null, options: CompilerOptions = DEFAULT_COMPILE_OPTIONS) {
    // parse and generate the AST:
    val lexer = BrainSaverLexer(CharStreams.fromStream(input))
    val tokens = CommonTokenStream(lexer)
    val parser = BrainSaverParser(tokens)
    parser.addErrorListener(CompileErrorListener.INSTANCE)
    val tree = parser.program()

    CodeGen(outputStream, options).use {
        val visitor = TreeWalker(it)
        visitor.visit(tree)
    }
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

        val compilerOptions = CompilerOptions(
                optimize = !commandLine.hasOption("no-cf"),
                minify = commandLine.hasOption("minify"),
                wrapping = commandLine.hasOption("wrapping"),
                output = commandLine.getOptionValue("output") ?: "",
                margin = commandLine.getOptionValue("margin")?.toInt() ?: 64
        )

        compile(FileInputStream(remainingArgs[0]), options = compilerOptions)

    } catch (e: ParseException) {
        printUsageAndHalt(options)
    } catch (e: ParseCancellationException) {
        println(e.message)
        System.exit(1)
    }

}