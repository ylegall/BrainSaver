package org.ygl.transformation

import org.antlr.v4.runtime.ParserRuleContext
import org.ygl.Function
import org.ygl.SymbolInfo
import org.ygl.ast.GlobalVariableNode

class ProgramInfo(
        val globals: Map<String, GlobalVariableNode> = mutableMapOf(),
        val functionInfo: Map<String, SymbolInfo> = mutableMapOf()
)

class FunctionInfo(
        val function: Function,
        val unusedSymbols: Set<String>,
        val lastSymbolsUsedMap: Map<ParserRuleContext, Set<String>>,
        val loopSymbolsWritten: Map<ParserRuleContext, Set<String>>
)