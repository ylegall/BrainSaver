package org.ygl

import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.io.OutputStream


class CompilerOptions(
        val verbose: Boolean = false,
        val commentChar: String = "#",
        val optimize: Boolean = true,
        val minify: Boolean = false,
        val output: OutputStream = System.`out`,
        val margin: Int = 64
)

val DEFAULT_COMPILE_OPTIONS = CompilerOptions()

fun configureCommandLine(): Options {
    val options = Options()

    val verbose = Option.builder("m")
            .longOpt("minify")
            .desc("generate minimal compact output")
            .build()

    val margin = Option.builder()
            .longOpt("margin")
            .desc("minified margin size")
            .hasArg(true)
            .build()

    val noOptimization = Option.builder()
            .longOpt("no-cf")
            .desc("disables constant folding optimizations")
            .build()

    val output = Option.builder("o")
            .longOpt("output")
            .desc("specifies the output file")
            .hasArg(true)
            .build()

    val version = Option.builder()
            .longOpt("version")
            .desc("print the compiler version")
            .build()

    options.addOption(output)
    options.addOption(noOptimization)
    options.addOption(verbose)
    options.addOption(version)
    options.addOption(margin)
    options.addOption(Option("help", "print this message"))
    return options
}