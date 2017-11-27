package org.ygl

import org.antlr.v4.runtime.ParserRuleContext
import org.ygl.BrainSaverParser.*
import java.util.stream.Collectors

/**
 * information returned from walking tree nodes
 */
class SymbolInfo {
    val readSymbols    = HashSet<String>()
    val writtenSymbols = HashSet<String>()
}

/**
 * Finalized info passed to the code generation phase
 */
class AnalysisInfo (
    val function: Function,
    val unusedSymbols: Set<String>,
    val lastSymbolsUsedMap: Map<ParserRuleContext, Set<String>>,
    val loopSymbolsWritten: Map<ParserRuleContext, Set<String>>
)

typealias UsageInfoMap = Map<String, AnalysisInfo>

/**
 *
 */
private class TempAnalysisInfo(val function: Function)
{
    val assignedSymbols = HashSet<String>()
    val loopSymbolsWritten = HashMap<ParserRuleContext, HashSet<String>>()
    val lastSymbolsUsedMap = HashMap<ParserRuleContext, HashSet<String>>()
    val symbolUseMap = HashMap<String, ParserRuleContext>()
}

/**
 *
 */
fun analysisPass(tree: ProgramContext, options: CompilerOptions): UsageInfoMap {
    val visitor = AnalyzingVisitor()
    visitor.visit(tree)
    val analysisInfoMap = visitor.getAnalysisInfo()

    if ("main" !in analysisInfoMap) throw Exception("no main function found")

    if (options.verbose) {
        for ((fn, info) in analysisInfoMap) {
            println("\n$fn")
            println("-".repeat(fn.length))
            println("\tunused symbols: ${info.unusedSymbols}")
        }
    }
    return analysisInfoMap
}

/**
 * Walks the tree to collect symbol usage info
 */
class AnalyzingVisitor : BrainSaverBaseVisitor<SymbolInfo>()
{
    private var currentFunction = ""
    private val functionInfo = HashMap<String, TempAnalysisInfo>()

    /**
     *
     */
    fun getAnalysisInfo(): Map<String, AnalysisInfo> {

        fun buildAnalysisInfo(tempInfo: TempAnalysisInfo): AnalysisInfo {
            return AnalysisInfo(
                    tempInfo.function,
                    tempInfo.assignedSymbols.subtract(tempInfo.symbolUseMap.keys),
                    tempInfo.lastSymbolsUsedMap,
                    tempInfo.loopSymbolsWritten
            )
        }

        return functionInfo.entries.stream()
                .collect(Collectors.toMap(
                        { entry -> entry.key },
                        { entry -> buildAnalysisInfo(entry.value) }
                    )
                )
    }

    override fun visitFunction(ctx: FunctionContext?): SymbolInfo {
        ctx ?: throw Exception("null FunctionContext")
        val name = ctx.name.text
        val isVoid = ctx.functionBody().ret == null
        val function = Function(name, ctx, isVoid)

        if (name in functionInfo) {
            throw Exception("duplicate function: $name")
        }
        functionInfo[name] = TempAnalysisInfo(function)

        currentFunction = name
        val result = visitChildren(ctx)
        currentFunction = ""

        return result
    }

    override fun visitWhileStatement(ctx: WhileStatementContext?): SymbolInfo {
        ctx ?: throw Exception("null WhileStatementContext")
        val symbolInfo = visitChildren(ctx)
        val scopeInfo = functionInfo[currentFunction] ?: throw Exception("unregistered function: $currentFunction")
        scopeInfo.loopSymbolsWritten[ctx] = symbolInfo.writtenSymbols
        return symbolInfo
    }

    override fun visitForStatement(ctx: ForStatementContext?): SymbolInfo {
        ctx ?: throw Exception("null ForStatementContext")
        val symbolInfo = visitChildren(ctx)
        val scopeInfo = functionInfo[currentFunction] ?: throw Exception("unregistered function: $currentFunction")
        scopeInfo.loopSymbolsWritten[ctx] = symbolInfo.writtenSymbols
        return symbolInfo
    }

    override fun visitIfStatement(ctx: IfStatementContext?): SymbolInfo {
        ctx ?: throw Exception("null IfStatementContext")
        val symbolInfo = visitChildren(ctx)
        val scopeInfo = functionInfo[currentFunction] ?: throw Exception("unregistered function: $currentFunction")
        scopeInfo.loopSymbolsWritten[ctx] = symbolInfo.writtenSymbols
        return symbolInfo
    }

    override fun visitReturnStatement(ctx: ReturnStatementContext?): SymbolInfo {
        ctx ?: throw Exception("null ReturnStatementContext")
        val symbolInfo = visit(ctx.exp())
        recordSymbolRead(symbolInfo, ctx)
        return symbolInfo
    }

    override fun visitStatement(ctx: StatementContext?): SymbolInfo {
        ctx ?: throw Exception("null StatementContext")
        val symbolInfo = visitChildren(ctx)
        recordSymbolRead(symbolInfo, ctx)
        return symbolInfo
    }

    private fun recordSymbolRead(symbolInfo: SymbolInfo, ctx: ParserRuleContext) {
        val scopeInfo = functionInfo[currentFunction] ?: throw Exception("unregistered function: $currentFunction")
        symbolInfo.readSymbols.forEach { symbol ->
            val oldStatement = scopeInfo.symbolUseMap[symbol]
            oldStatement?.let {
                scopeInfo.lastSymbolsUsedMap[it]?.remove(symbol)
                if (scopeInfo.lastSymbolsUsedMap[it]?.isEmpty() == true) {
                    scopeInfo.lastSymbolsUsedMap.remove(it)
                }
            }
            val symbolSet = scopeInfo.lastSymbolsUsedMap.getOrDefault(ctx, hashSetOf())
            symbolSet.add(symbol)
            scopeInfo.lastSymbolsUsedMap[ctx] = symbolSet
            scopeInfo.symbolUseMap[symbol] = ctx
        }
    }

    override fun visitAssignmentStatement(ctx: AssignmentStatementContext?): SymbolInfo {
        ctx ?: throw Exception("null AssignmentStatementContext")
        return recordWriteSymbol(ctx.lhs.text, ctx.rhs)
    }

    override fun visitArrayConstructor(ctx: ArrayConstructorContext?): SymbolInfo {
        ctx ?: throw Exception("null ArrayConstructorContext")
        return recordWriteSymbol(ctx.lhs.text)
    }

    override fun visitArrayLiteral(ctx: ArrayLiteralContext?): SymbolInfo {
        ctx ?: throw Exception("null ArrayLiteralContext")
        return recordWriteSymbol(ctx.lhs.text)
    }

    override fun visitReadStatement(ctx: ReadStatementContext?): SymbolInfo {
        ctx ?: throw Exception("null ReadStatementContext")
        return recordWriteSymbol(ctx.Identifier().text)
    }

    override fun visitArrayWriteStatement(ctx: ArrayWriteStatementContext?): SymbolInfo {
        ctx ?: throw Exception("null ArrayWriteStatementContext")
        return recordWriteSymbol(ctx.Identifier().text, ctx)
    }

    private fun recordWriteSymbol(lhs: String, ctx: ParserRuleContext? = null): SymbolInfo {
        val info = functionInfo[currentFunction] ?: throw Exception("unregistered function: $currentFunction")
        info.assignedSymbols.add(lhs)
        return if (ctx != null)
            visitChildren(ctx).apply{ writtenSymbols.add(lhs) }
        else
            SymbolInfo()
    }

    override fun visitAtomId(ctx: AtomIdContext?): SymbolInfo {
        ctx ?: throw Exception("null AtomIdContext")
        return SymbolInfo().apply{ readSymbols.add(ctx.text) }
    }

    override fun aggregateResult(aggregate: SymbolInfo?, nextResult: SymbolInfo?): SymbolInfo {
        return if (aggregate != null && nextResult != null) {
            aggregate.readSymbols.addAll(nextResult.readSymbols)
            aggregate.writtenSymbols.addAll(nextResult.writtenSymbols)
            aggregate
        } else if (aggregate != null) {
            aggregate
        } else if (nextResult != null) {
            nextResult
        } else {
            SymbolInfo()
        }
    }
}