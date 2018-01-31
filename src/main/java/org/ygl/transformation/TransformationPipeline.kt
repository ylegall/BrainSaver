package org.ygl.transformation

import org.antlr.v4.runtime.ParserRuleContext
import org.ygl.CompileException
import org.ygl.DEBUG
import org.ygl.ast.AstBuilder
import org.ygl.ast.AstNode
import org.ygl.ast.AstPrinter
import org.ygl.ast.SemanticValidator
import org.ygl.runtime.SystemContext

/**
 *
 */
class TransformationPipeline(
        private val ctx: SystemContext
) {
    private val printer = AstPrinter()

    fun buildAst(parserRuleContext: ParserRuleContext): AstNode {
        return AstBuilder().visit(parserRuleContext)
    }

    fun semanticValidation(ast: AstNode) {
        val validator = SemanticValidator(ctx.stdlib)
        val errors = validator.validate(ast)
        if (errors.isNotEmpty()) {
            errors.forEach { println(it.message) }
            throw CompileException("compilation failed")
        }
    }

    fun resolveConstants(ast: AstNode): AstNode {
        val constants = ConstantResolver().resolveConstants(ast)
        if (ctx.options.verbose) {
            println("\nresolved constants:")
            println("-------------------")
            constants.forEach { (key, value) -> println("\t$key = $value") }
        }
        return ConstantEvaluator(constants).evaluateConstants(ast)
    }

    fun propagateConstants(ast: AstNode): AstNode {
        return ConstantPropagator().visit(ast)
    }

    fun strengthReduce(ast: AstNode): AstNode {
        return StrengthReducer().visit(ast)
    }

    fun removeDeadCode(ast: AstNode): AstNode {
        val deadStores = DeadStoreResolver().getDeadStores(ast)
        if (ctx.options.verbose) {
            println("\ndead stores:")
            println("-------------")
            deadStores.forEach { println("\t$it: ${it.children[0]}") }
        }
        return DeadStoreRemover(deadStores).visit(ast)
    }

    fun findLastUsages(ast: AstNode): AstNode {
        ctx.lastUseInfo = LastUseResolver().getSymbolLastUseInfo(ast)
        if (DEBUG) {
            println("last use nodes:")
            println("---------------")
            ctx.lastUseInfo.forEach { node, symbols ->
                println("  $node\t\t: $symbols")
            }
        }
        return ast
    }

    fun transform(parserRuleContext: ParserRuleContext): AstNode {
        var ast = buildAst(parserRuleContext)
                .also { semanticValidation(it) }
                .let { resolveConstants(it) }

        if (ctx.options.verbose) {
            println("\ninitial AST:")
            println("------------")
            printer.print(ast)
        }

        if (ctx.options.optimize) {
            ast = propagateConstants(ast)
                    .let { strengthReduce(it) }
                    .let { removeDeadCode(it) }
        }

        ast = findLastUsages(ast)

        if (ctx.options.verbose) {
            println("\nfinal AST:")
            println("----------")
            printer.print(ast)
        }

        return ast
    }
}