package org.ygl

import org.apache.commons.cli.Option
import org.apache.commons.cli.Options

class InterpreterOptions
(
        val wrap: Boolean = false,
        val optimize: Boolean = true,
        val debug: Boolean = false,
        val predefinedInput: String = "",
        val memorySize: Int = 30000
)

val DEFAULT_INTERPRETER_OPTIONS = InterpreterOptions()

fun configureInterpreterOptions(): Options {
    val options = Options()

    val input = Option.builder("i")
            .longOpt("input")
            .desc("pre-specify program input")
            .hasArg()
            .build()

    val noOptimization = Option.builder()
            .longOpt("no-opt")
            .desc("disable optimizations")
            .build()

    val wrapping = Option.builder("w")
            .longOpt("wrap")
            .desc("generate code for wrapping interpreters")
            .build()

    val debug = Option.builder("d")
            .longOpt("debug")
            .desc("enable debug mode interpreter output")
            .build()

    val size = Option.builder("m")
            .longOpt("memory")
            .desc("specify memory size")
            .hasArg(true)
            .build()

    with (options) {
        addOption(noOptimization)
        addOption(wrapping)
        addOption(debug)
        addOption(input)
        addOption(size)
        addOption(Option("help", "print this message"))
    }
    return options
}