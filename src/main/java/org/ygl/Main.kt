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
import java.io.InputStream

const val VERSION = "1.0"

private inline fun printUsageAndHalt(options: Options) {
    HelpFormatter().printHelp("brainsaver", options, true)
    System.exit(1)
}

fun compile(input: InputStream, options: CompilerOptions = DEFAULT_COMPILE_OPTIONS) {
    // parse and generate the AST:
    val lexer = BrainSaverLexer(CharStreams.fromStream(input))
    val tokens = CommonTokenStream(lexer)
    val parser = BrainSaverParser(tokens)
    parser.addErrorListener(CompileErrorListener.INSTANCE)
    val tree = parser.program()

    var output = options.output
    if (options.minify) {
        output = MinifyingOutputStream(
                output,
                options.margin
        )
    }

    CodeGen(output, options).use {
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
                verbose = !commandLine.hasOption("minify"),
                optimize = !commandLine.hasOption("no-cf"),
                minify = commandLine.hasOption("minify"),
                output = if (commandLine.hasOption("output")) {
                    FileOutputStream(File(commandLine.getOptionValue("output")))
                } else {
                    System.`out`
                },
                margin = commandLine.getOptionValue("margin")?.toInt() ?: 64
        )

        compile(FileInputStream(remainingArgs[0]), compilerOptions)

    } catch (e: ParseException) {
        printUsageAndHalt(options)
    } catch (e: ParseCancellationException) {
        println(e.message)
        System.exit(1)
    }

}