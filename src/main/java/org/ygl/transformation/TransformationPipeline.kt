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
    private var symbolInfo: Map<AstNode, SymbolInfo> = mapOf()
    private var lastUseInfo: Map<AstNode, Set<String>> = mapOf()
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

//    private fun AstNode.constantFold(): AstNode {
//        val ast = ConstantFolder(symbolInfo).visit(this)
//
//        if (options.verbose) {
//            println("constant folding:")
//            println("-----------------")
//            AstPrinter().print(ast)
//        }
//
//        return ast
//    }

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

    private fun AstNode.propagateConstants(): AstNode {
//        val env = AssignmentResolver().resolveAssignments(this)
//
//        println("\nresolved env:")
//        println("-------------")
//        for ((key, value) in env) {
//            println("\t$key:\t\t$value")
//        }

        return ConstantPropagator().visit(this)
    }

    fun transform(ctx: ParserRuleContext) {
        val ast = ctx.buildAst()
                .semanticValidation()
                .resolveConstants()
                .propagateConstants()
//                .findUnusedSymbols()
//                .constantFold()
//                .findUnusedSymbols()
//                .constantFold()
//                .findUnusedSymbols()
//                .constantFold()
//                .findLastUsages()

        println("\nfinal ast:\n")
        AstPrinter().print(ast)

        // generate bf code:
        //AstCodeGenerator(outStream, options, lastUseInfo).visit(ast)
    }
}