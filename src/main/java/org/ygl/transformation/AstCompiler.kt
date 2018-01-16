package org.ygl.transformation

import com.sun.org.apache.xalan.internal.xsltc.compiler.CompilerException
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.ygl.BrainSaverParser
import org.ygl.CompileException
import org.ygl.CompilerOptions
import org.ygl.ast.*
import org.ygl.model.*
import org.ygl.runtime.*
import java.io.OutputStream
import java.util.ArrayList

/**
 * Walks the Ast and generates code
 */
class AstCompiler(
        outputStream: OutputStream,
        private val options: CompilerOptions,
        private val lastUseInfo: Map<AstNode, Set<String>> = mapOf()
): AstWalker<Symbol>()
{
    private val runtime = Runtime()
    private val functions = mutableMapOf<String, FunctionNode>()
    private val cg = CodeGen(outputStream, options, runtime)
    private val stdlib = StdLib(cg, runtime)

    /**
     * visit the main function.
     * add globals to the global scope
     * register all functions
     */
    override fun visit(node: ProgramNode): Symbol {
        // register functions:
        node.children.filterIsInstance<FunctionNode>()
                .forEach {
                    if (it.name in functions) {
                        throw CompileException("function ${it.name} redefined")
                    }
                    functions.put(it.name, it)
                }

        // add global variables:
        runtime.enterScope(node)
        node.children.filterIsInstance<GlobalVariableNode>()
                .forEach { visit(it) }

        functions["main"]?.let { visit(it) } ?: throw CompileException("no main function found")

        return UnknownSymbol
    }

    override fun visit(node: GlobalVariableNode): Symbol {
        val rhs = visit(node.rhs)
        return when {
            rhs.isConstant() -> {
                val lhs = runtime.createSymbol(node.lhs, StorageType.VAR, rhs.value)
                assignConstant(lhs, rhs.value)
            }
            rhs.isTemp() -> runtime.rename(rhs, node.lhs)
            else -> {
                val lhs = runtime.createSymbol(node.lhs, StorageType.VAR, rhs.value)
                assignVariable(lhs, rhs)
            }
        }
    }

    private fun evaluateGlobal(node: GlobalVariableNode): Symbol {
        val rhs = visit(node.rhs)
        TODO("assign value")
        return rhs
    }



    /**
     * visit the statement.
     * can delete all temp symbols after each statement.
     * can also garbage collect any symbol that is not referenced after this statement
     */
    override fun visit(node: StatementNode): Symbol {
        val result = visit(node.children)
        // TODO(clean up temp variables)
        // TODO(check if this is the last time a symbol is used)
        return result
    }

    override fun visit(node: CallStatementNode): Symbol {
        val args = node.params.map { visit(it) }

        // check for pre-defined stdlib function
        if (node.name in stdlib.functions) {
            // TODO
        }

        // lookup matching function and its params
        val functionNode = functions[node.name] ?: throw CompilerException("unrecognized function ${node.name}")

        return functionCall(functionNode, args)
    }

    override fun visit(node: CallExpNode): Symbol {
        val args = node.params.map { visit(it) }

        // check for pre-defined stdlib function
        if (node.name in stdlib.functions) {
            // TODO
        }

        // lookup matching function and its params
        val functionNode = functions[node.name] ?: throw CompilerException("unrecognized function ${node.name}")

        return functionCall(functionNode, args)
    }

    private fun functionCall(fnNode: FunctionNode, args: List<Symbol>): Symbol {
        // collect arguments for function call
        if (args.isNotEmpty()) {
            val params = fnNode.params
            if (params.isEmpty() || args.size != params.size) {
                throw Exception("wrong number of arguments to ${fnNode.name}")
            }

            runtime.enterScope(fnNode)
            cg.commentLine("call ${fnNode.name}")

            // create new scope and copy expression args into function param variables
            for (i in 0 until params.size) {
                val param = runtime.createSymbol(params[i], StorageType.VAL, args[i].value)
                when {
                    args[i].isConstant() -> assignConstant(param, args[i].value)
                    else -> assignVariable(param, args[i])
                }
            }

        } else {
            runtime.enterScope(fnNode)
            cg.commentLine("call ${fnNode.name}")
        }

        // execute statements in function body:
        fnNode.statements.forEach { visit(it) }

        // TODO
        var result: Symbol = UnknownSymbol
        if (fnNode.ret != null) {
            val ret = visit(fnNode.ret)
            runtime.exitScope()
            val cpy = runtime.createTempSymbol(ret.value)
            result = cg.move(cpy, ret)
        } else {
            runtime.exitScope()
        }

        cg.commentLine("end call ${fnNode.name}")
        return result
    }

    override fun visit(node: DeclarationNode): Symbol {
        val rhs = visit(node.rhs)

        return when {
            rhs.isConstant() -> {
                val lhs = runtime.createSymbol(node.lhs, node.storage, rhs.value)
                assignConstant(lhs, rhs.value)
            }
            rhs.isTemp() -> {
                runtime.rename(rhs, node.lhs)
            }
            else -> {
                val lhs = runtime.createSymbol(node.lhs, node.storage, rhs.value)
                assignVariable(lhs, rhs)
            }
        }
    }

    override fun visit(node: AssignmentNode): Symbol {
        val rhs = visit(node.rhs)
        if (rhs.name == node.lhs) return rhs // no-op
        val lhs = runtime.resolveSymbol(node.lhs) ?: throw CompileException("unresolved symbol ${node.lhs}")

        return when {
            rhs.isConstant() -> assignConstant(lhs, rhs.value)
            rhs.isTemp() -> runtime.rename(rhs, lhs.name)
            else -> assignVariable(lhs, rhs)
        }
    }

    private fun assign(lhs: Symbol, rhs: Symbol): Symbol {
        return when {
            rhs.isConstant() -> assignConstant(lhs, rhs.value)
            else -> assignVariable(lhs, rhs)
        }
    }

    private fun assignVariable(lhs: Symbol, rhs: Symbol): Symbol {
        return when (rhs.value) {
            is Int -> cg.copyInt(lhs, rhs)
            else -> throw CompileException("invalid rhs type: $rhs")
        }
    }

    private fun assignConstant(lhs: Symbol, rhs: Any): Symbol {
        return when (rhs) {
            is Int -> cg.loadImmediate(lhs, rhs)
            is String -> cg.loadImmediate(lhs, rhs)
            else -> throw CompileException("invalid rhs value: ${rhs}")
        }
    }

    override fun visit(node: BinaryExpNode): Symbol {
        val left = visit(node.left)
        val right = visit(node.right)

        with (cg) {
            return when (node.op) {
                Op.ADD -> math.add(left, right)
                Op.SUB -> math.subtract(left, right)
                Op.MUL -> math.multiply(left, right)
                Op.DIV -> math.divide(left, right)
                Op.MOD -> math.mod(left, right)
                Op.AND -> math.and(left, right)
                Op.OR ->  math.or(left, right)
                Op.EQ ->  math.equal(left, right)
                Op.NEQ -> math.notEqual(left, right)
                Op.GEQ -> math.greaterThanEqual(left, right)
                Op.LEQ -> math.lessThanEqual(left, right)
                Op.LT ->  math.lessThan(left, right)
                Op.GT ->  math.greaterThan(left, right)
                else -> throw Exception("invalid op: ${node.op}")
            }
        }
    }

    override fun visit(node: NotExpNode): Symbol {
        return cg.math.not(visit(node.right))
    }

    override fun visit(node: AtomIdNode): Symbol {
        return runtime.resolveSymbol(node.identifier) ?: throw CompileException("unresolved symbol: ${node.identifier}")
    }

    override fun visit(node: AtomIntNode): Symbol {
        return ConstantSymbol(node.value)
    }

    override fun visit(node: AtomStrNode): Symbol {
        return ConstantSymbol(node.value)
    }

    override fun defaultValue(node: AstNode) = UnknownSymbol
}