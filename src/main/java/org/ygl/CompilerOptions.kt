package org.ygl

import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.io.OutputStream


class CompilerOptions(
        val commentChar: String = "#",
        val optimize: Boolean = true,
        val minify: Boolean = false,
        val output: String = "",
        val margin: Int = 48,
        val verbose: Boolean = false,
        val wrapping: Boolean = false
)

val DEFAULT_COMPILE_OPTIONS = CompilerOptions()

fun configureCommandLine(): Options {
    val options = Options()

    val minify = Option.builder("m")
            .longOpt("minify")
            .desc("generate minimal compact output")
            .build()

    val margin = Option.builder()
            .longOpt("margin")
            .desc("generated code margin width")
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

    val verbose = Option.builder("v")
            .longOpt("verbose")
            .desc("generate verbose output during compilation")
            .build()

    val wrapping = Option.builder("w")
            .longOpt("wrapping")
            .desc("generate code for wrapping interpreters")
            .build()

    val version = Option.builder()
            .longOpt("version")
            .desc("print the compiler version")
            .build()

    val help = Option.builder("h")
            .longOpt("help")
            .desc("print this message")
            .build()

    with (options) {
        addOption(output)
        addOption(noOptimization)
        addOption(wrapping)
        addOption(minify)
        addOption(version)
        addOption(verbose)
        addOption(margin)
        addOption(help)
    }
    return options
}