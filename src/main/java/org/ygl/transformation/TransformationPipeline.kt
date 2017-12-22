package org.ygl.transformation

import org.antlr.v4.runtime.ParserRuleContext
import org.ygl.CompilerOptions
import org.ygl.ast.AstBuilder
import org.ygl.ast.AstDebugger
import org.ygl.ast.AstNode

fun buildAst(ctx: ParserRuleContext, options: CompilerOptions): TransformationPipeline
{
    val ast = AstBuilder().visit(ctx)

    println("\nbefore\n")
    AstDebugger().print(ast)

    return TransformationPipeline(ast, options)
}

class TransformationPipeline(
        private var ast: AstNode,
        private val options: CompilerOptions
)
{
    fun resolveConstants(): TransformationPipeline {
        ast = ConstantResolver(options).resolveConstants(ast)
        return this
    }

    fun findUnusedSymbols(): TransformationPipeline {
        val symbolInfo = MutabilityResolver().getSymbolMutabilityInfo(ast)

        println("\nsymbol info:")
        println("-------------")
        symbolInfo.forEach { scope, symbols ->
            if (!symbols.symbolsRead.isEmpty() || !symbols.symbolsWritten.isEmpty()) {
                println("  in scope '$scope':")
                println("    read:     ${symbols.symbolsRead}")
                println("    written:  ${symbols.symbolsWritten}")
                println("    declared: ${symbols.symbolsDeclared}")
                println("    unused :  ${symbols.unusedSymbols}")
                println()
            }
        }
        println()

        return this
    }

    fun constantFold(): TransformationPipeline {
        ast = ConstantFolder().visit(ast)
        return this
    }

    fun getAst() = ast
}