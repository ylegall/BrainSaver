package org.ygl.transformation

import org.antlr.v4.runtime.ParserRuleContext
import org.ygl.CompilerOptions
import org.ygl.ast.AstBuilder
import org.ygl.ast.AstPrinter
import org.ygl.ast.AstNode


/**
 *
 */
class TransformationPipeline(
        private val options: CompilerOptions
)
{
    private var symbolInfo: Map<AstNode, SymbolInfo> = mapOf()
    private var lastUseInfo: Map<AstNode, Set<String>> = mapOf()

    /**
     *
     */
    private fun ParserRuleContext.buildAst(): AstNode {
        val ast = AstBuilder().visit(this)
        println("initial ast:")
        println("------------")
        AstPrinter().print(ast)
        return ast
    }

    /**
     *
     */
    private fun AstNode.resolveConstants(): AstNode {
        return ConstantResolver(options).resolveConstants(this)
    }

    private fun AstNode.findUnusedSymbols(): AstNode {
        symbolInfo = MutabilityResolver().getSymbolMutabilityInfo(this)

        if (options.verbose) {
            println("\nsymbol info:")
            println("-------------")
            symbolInfo.forEach { scope, symbols ->
                if (!symbols.deadStores.isEmpty() || !symbols.modifiedSymbols.isEmpty()) {
                    println("  in scope '$scope':")
                    println("    modified: ${symbols.modifiedSymbols}")
                    println("    unused:   ${symbols.deadStores}")
                    println()
                }
            }
            println()
        }

        return this
    }

    private fun AstNode.constantFold(): AstNode {
        val ast = ConstantFolder(symbolInfo).visit(this)

        if (options.verbose) {
            println("constant folding:")
            println("-----------------")
            AstPrinter().print(ast)
        }

        return ast
    }

    private fun AstNode.findLastUsages(): AstNode {
        lastUseInfo = LastUseResolver().getSymbolLastUseInfo(this)

        if (options.verbose) {
            println("last use nodes:")
            println("---------------")
            lastUseInfo.forEach { node, symbols ->
                println("  $node\t\t: $symbols")
            }
        }

        return this
    }

    fun transform(ctx: ParserRuleContext) {
        val ast = ctx.buildAst()
                .resolveConstants()
                .findUnusedSymbols()
                .constantFold()
                .findUnusedSymbols()
                .constantFold()
                .findLastUsages()
        println("\nfinal ast:\n")
        AstPrinter().print(ast)

    }
}