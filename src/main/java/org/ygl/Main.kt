package org.ygl

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import java.io.*

const val VERSION = "1.0"

private fun printUsageAndHalt(options: Options) {
    HelpFormatter().printHelp("brainsaver", options, true)
    System.exit(1)
}

fun compile(str: String, outStream: OutputStream, options: CompilerOptions = DEFAULT_COMPILE_OPTIONS) {
    val inputStream = ByteArrayInputStream(str.toByteArray(Charsets.UTF_8))
    inputStream.use { input ->
        compile(input, outStream, options)
    }
}

fun compile(infile: File, options: CompilerOptions = DEFAULT_COMPILE_OPTIONS) {
    val inputStream = FileInputStream(infile)
    inputStream.use { input ->
        if (options.output.isNotEmpty()) {
            FileOutputStream(options.output).use { output ->
                compile(input, output, options)
            }
        } else {
            compile(input, System.`out`, options)
        }
    }
}

fun compile(input: InputStream, outStream: OutputStream, options: CompilerOptions = DEFAULT_COMPILE_OPTIONS) {
    // parse and generate the AST:
    val lexer = BrainSaverLexer(CharStreams.fromStream(input))
    val tokens = CommonTokenStream(lexer)
    val parser = BrainSaverParser(tokens)
    parser.addErrorListener(CompileErrorListener.INSTANCE)
    val tree = parser.program()

    val globals = resolveGlobals(parser, tree)
    val analysisInfoMap = analysisPass(tree, options, globals)

    val cg = CodeGen(outStream, options, globals)
    cg.use {
        val visitor = TreeWalker(it, analysisInfoMap)
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
        if (commandLine.hasOption("help") || remainingArgs.isEmpty()) {
            printUsageAndHalt(options)
        }

        val compilerOptions = CompilerOptions(
                optimize = !commandLine.hasOption("no-cf"),
                minify = commandLine.hasOption("minify"),
                wrapping = commandLine.hasOption("wrapping"),
                output = commandLine.getOptionValue("output") ?: "",
                margin = commandLine.getOptionValue("margin")?.toInt() ?: 64,
                verbose = commandLine.hasOption("verbose")
        )

        val elapsed = time({
            compile(File(remainingArgs[0]), options = compilerOptions)
        })
        println("compiled in $elapsed ms")

    } catch (e: ParseException) {
        System.err.println(e.message)
        printUsageAndHalt(options)
    } catch (e: CompilationException ) {
        System.err.println(e.message)
    } catch (e: ParseCancellationException) {
        System.err.println(e.message)
    }
}