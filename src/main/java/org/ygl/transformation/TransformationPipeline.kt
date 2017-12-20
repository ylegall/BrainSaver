package org.ygl.transformation

import org.antlr.v4.runtime.ParserRuleContext
import org.ygl.CompilerOptions
import org.ygl.ast.AstBuilder
import org.ygl.ast.AstNode

fun buildAst(ctx: ParserRuleContext, options: CompilerOptions): TransformationPipeline
{
    val ast = AstBuilder().visit(ctx)
    return TransformationPipeline(ast, options)
}

class TransformationPipeline(
        private var ast: AstNode,
        private val options: CompilerOptions
)
{
    fun resolveConstants(): TransformationPipeline {
        val constants = ConstantExtractor().extractConstants(ast)
        if (options.verbose) {
            println("\nresolved constants:")
            println("-------------------")
            constants.forEach { (key, value) -> println("\t$key = $value") }
        }
        ast = ConstantResolver(constants).resolveConstants(ast)
        return this
    }

    fun getAst() = ast
}