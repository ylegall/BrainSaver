package org.ygl

import org.antlr.v4.runtime.ParserRuleContext
import org.ygl.BrainSaverParser.*
import java.util.stream.Collectors

/**
 * information returned from walking tree nodes
 */
internal class SymbolResult {
    val readSymbols    = HashSet<String>()
    val writtenSymbols = HashSet<String>()
}

/**
 * Walks the tree to collect symbol usage info
 */
internal class AnalyzingVisitor : BrainSaverBaseVisitor<SymbolResult>()
{
    private var currentFunction = ""
    private val functionInfo = HashMap<String, ScopeInfo>()

    private class ScopeInfo(val function: Function)
    {
        val assignedSymbols = HashSet<String>()
        val loopSymbolsWritten = HashMap<ParserRuleContext, HashSet<String>>()
        val lastSymbolsUsedMap = HashMap<ParserRuleContext, HashSet<String>>()
        val symbolUseMap = HashMap<String, ParserRuleContext>()
    }

    /**
     *
     */
    fun getAnalysisInfo(globals: Map<String, Symbol>): Map<String, SymbolInfo> {

        fun buildAnalysisInfo(tempInfo: ScopeInfo): SymbolInfo {
            return SymbolInfo(
                    tempInfo.function,
                    tempInfo.assignedSymbols.subtract(tempInfo.symbolUseMap.keys).subtract(globals.keys),
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

    override fun visitFunction(ctx: FunctionContext?): SymbolResult {
        val name = ctx!!.name.text
        val isVoid = ctx.functionBody().ret == null
        val function = Function(name, ctx, isVoid)

        if (name in functionInfo) {
            throw CompilationException("duplicate function", ctx)
        }
        functionInfo[name] = ScopeInfo(function)

        currentFunction = name
        val result = visitChildren(ctx)
        currentFunction = ""

        return result
    }

    override fun visitWhileStatement(ctx: WhileStatementContext?): SymbolResult {
        val symbolInfo = visitChildren(ctx!!)
        val scopeInfo = functionInfo[currentFunction] ?: throw Exception("unregistered function: $currentFunction")
        scopeInfo.loopSymbolsWritten[ctx] = symbolInfo.writtenSymbols
        return symbolInfo
    }

    override fun visitForStatement(ctx: ForStatementContext?): SymbolResult {
        val symbolInfo = visitChildren(ctx!!)
        val scopeInfo = functionInfo[currentFunction] ?: throw Exception("unregistered function: $currentFunction")
        scopeInfo.loopSymbolsWritten[ctx] = symbolInfo.writtenSymbols
        return symbolInfo
    }

    override fun visitIfStatement(ctx: IfStatementContext?): SymbolResult {
        val symbolInfo = visitChildren(ctx!!)
        val scopeInfo = functionInfo[currentFunction] ?: throw Exception("unregistered function: $currentFunction")
        scopeInfo.loopSymbolsWritten[ctx] = symbolInfo.writtenSymbols
        return symbolInfo
    }

    override fun visitReturnStatement(ctx: ReturnStatementContext?): SymbolResult {
        ctx ?: throw Exception("null ReturnStatementContext")
        val symbolInfo = visit(ctx.exp())
        recordSymbolRead(symbolInfo, ctx)
        return symbolInfo
    }

    override fun visitStatement(ctx: StatementContext?): SymbolResult {
        ctx ?: throw Exception("null StatementContext")
        val symbolInfo = visitChildren(ctx)
        recordSymbolRead(symbolInfo, ctx)
        return symbolInfo
    }

    private fun recordSymbolRead(symbolInfo: SymbolResult, ctx: ParserRuleContext) {
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

    override fun visitAssignmentStatement(ctx: AssignmentStatementContext?): SymbolResult {
        ctx ?: throw Exception("null AssignmentStatementContext")
        return recordWriteSymbol(ctx.lhs.text, ctx.rhs)
    }

    override fun visitArrayConstructor(ctx: ArrayConstructorContext?): SymbolResult {
        ctx ?: throw Exception("null ArrayConstructorContext")
        return recordWriteSymbol(ctx.lhs.text)
    }

    override fun visitArrayLiteral(ctx: ArrayLiteralContext?): SymbolResult {
        ctx ?: throw Exception("null ArrayLiteralContext")
        return recordWriteSymbol(ctx.lhs.text)
    }

    override fun visitReadStatement(ctx: ReadStatementContext?): SymbolResult {
        ctx ?: throw Exception("null ReadStatementContext")
        return recordWriteSymbol(ctx.Identifier().text)
    }

    override fun visitArrayWriteStatement(ctx: ArrayWriteStatementContext?): SymbolResult {
        ctx ?: throw Exception("null ArrayWriteStatementContext")
        return recordWriteSymbol(ctx.Identifier().text, ctx)
    }

    private fun recordWriteSymbol(lhs: String, ctx: ParserRuleContext? = null): SymbolResult {
        val info = functionInfo[currentFunction] ?: throw Exception("unregistered function: $currentFunction")
        info.assignedSymbols.add(lhs)
        return if (ctx != null)
            visitChildren(ctx).apply{ writtenSymbols.add(lhs) }
        else
            SymbolResult()
    }

    override fun visitAtomId(ctx: AtomIdContext?): SymbolResult {
        ctx ?: throw Exception("null AtomIdContext")
        return SymbolResult().apply{ readSymbols.add(ctx.text) }
    }

    override fun aggregateResult(aggregate: SymbolResult?, nextResult: SymbolResult?): SymbolResult {
        return if (aggregate != null && nextResult != null) {
            aggregate.readSymbols.addAll(nextResult.readSymbols)
            aggregate.writtenSymbols.addAll(nextResult.writtenSymbols)
            aggregate
        } else if (aggregate != null) {
            aggregate
        } else if (nextResult != null) {
            nextResult
        } else {
            SymbolResult()
        }
    }
}