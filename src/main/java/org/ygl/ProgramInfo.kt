package org.ygl

import org.antlr.v4.runtime.ParserRuleContext
import org.ygl.BrainSaverParser.ProgramContext

class ProgramInfo(
    val globals: Map<String, Symbol> = mutableMapOf(),
    val constants: Map<String, Symbol> = mutableMapOf(),
    val functionInfo: Map<String, SymbolInfo> = mutableMapOf()
)

class SymbolInfo(
    val function: Function,
    val unusedSymbols: Set<String>,
    val lastSymbolsUsedMap: Map<ParserRuleContext, Set<String>>,
    val loopSymbolsWritten: Map<ParserRuleContext, Set<String>>
)

fun getProgramInfo(parser: BrainSaverParser,  options: CompilerOptions, tree: ProgramContext): ProgramInfo {
    val globals = resolveGlobals(parser, tree)
    val functionInfo = AnalyzingVisitor().apply {
        visit(tree)
    }.getAnalysisInfo(globals)

    // TODO: get constants
    val constants = mutableMapOf<String, Symbol>()
    val programInfo = ProgramInfo(globals, constants, functionInfo)

    // TODO: move out
    if (options.verbose) {
        for ((fn, info) in functionInfo) {
            println("\n$fn")
            println("-".repeat(fn.length))
            println("\tunused symbols: ${info.unusedSymbols}")
        }
    }

    return programInfo
}