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

    fun constantFold(): TransformationPipeline {
        ast = ConstantFolder().visit(ast)
        return this
    }

    fun getAst() = ast
}