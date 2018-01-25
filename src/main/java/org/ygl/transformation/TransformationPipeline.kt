package org.ygl.transformation

import org.antlr.v4.runtime.ParserRuleContext
import org.ygl.CompileException
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
)
{
    //private var lastUseInfo: Map<AstNode, Set<String>> = mapOf()
    private val options = ctx.options

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
    private fun AstNode.semanticValidation(): AstNode {
        val validator = SemanticValidator(ctx.stdlib)
        val errors = validator.validate(this)
        if (errors.isNotEmpty()) {
            errors.forEach { println(it.message) }
            throw CompileException("compilation failed")
        }
        return this
    }

    /**
     *
     */
    private fun AstNode.resolveConstants(): AstNode {
        val resolver = ConstantResolver()
        val constants = resolver.resolveConstants(this)
        if (options.verbose) {
            println("\nresolved constants:")
            println("-------------------")
            constants.forEach { (key, value) -> println("\t$key = $value") }
        }
        return ConstantEvaluator(constants).evaluateConstants(this)
    }

    private fun AstNode.removeDeadCode(): AstNode {
        //symbolInfo = MutabilityResolver().getSymbolMutabilityInfo(this)
        val deadStores = DeadStoreResolver().getDeadStores(this)

        if (options.verbose) {
            println("\ndead stores:")
            println("-------------")
            deadStores.forEach { println("\t$it") }
        }

        return DeadStoreRemover(deadStores).visit(this)
    }

    private fun AstNode.findLastUsages(): AstNode {
        AstPrinter().print(this)

        ctx.lastUseInfo = LastUseResolver().getSymbolLastUseInfo(this)

        //if (options.verbose) {
            println("last use nodes:")
            println("---------------")
            ctx.lastUseInfo.forEach { node, symbols ->
                println("  $node\t\t: $symbols")
            }
        //}

        return this
    }

    private fun AstNode.propagateConstants(): AstNode {
        return ConstantPropagator().visit(this)
    }

    private fun AstNode.strengthReduce(): AstNode {
        return StrengthReducer().visit(this)
    }

    fun transform(ruleContext: ParserRuleContext) {
        val ast = ruleContext.buildAst()
                .semanticValidation()
                .resolveConstants()
                .propagateConstants()
                .strengthReduce()
                .removeDeadCode()
                .findLastUsages()

        println("\nfinal ast:\n")
        println("----------")
        AstPrinter().print(ast)
        println("\n______________\n")

        AstCompiler(ctx).visit(ast)

        // generate bf code:
        //AstCodeGenerator(outStream, options, lastUseInfo).visit(ast)
    }
}