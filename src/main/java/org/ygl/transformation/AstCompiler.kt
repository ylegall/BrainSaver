package org.ygl.transformation

import org.ygl.CompileException
import org.ygl.ast.*
import org.ygl.model.IntType
import org.ygl.model.Op
import org.ygl.model.StorageType
import org.ygl.model.StrType
import org.ygl.runtime.Symbol
import org.ygl.runtime.Symbol.NullSymbol
import org.ygl.runtime.SystemContext

/**
 * Walks the Ast and generates code
 */
class AstCompiler(
    private val ctx: SystemContext
): AstWalker<Symbol>()
{
    private val runtime = ctx.runtime
    private val functions = mutableMapOf<String, FunctionNode>()
    private val cg = ctx.cg
    private val stdlib = ctx.stdlib

    /**
     * visit the main function.
     * add globals to the global scope
     * register all functions
     */
    override fun visit(node: ProgramNode): Symbol {
        // register functions:
        node.children.filterIsInstance<FunctionNode>()
                .also {  }
                .forEach {
                    if (it.name in functions) {
                        throw CompileException("function ${it.name} redefined")
                    }
                    functions[it.name] = it
                }

        // add global variables:
        runtime.enterScope(node)
        node.children.filterIsInstance<GlobalVariableNode>()
                .forEach { visit(it) }

        functions["main"]?.let { visit(it.statements) } ?: throw CompileException("no main function found")

        return Symbol.NullSymbol
    }

    override fun visit(node: GlobalVariableNode): Symbol {
        val rhs = visit(node.rhs)
        val lhs = runtime.createSymbol(node.lhs, StorageType.VAR, rhs.type, rhs.size)
        return assign(lhs, rhs)
    }

    /**
     * visit the statement.
     * can delete all temp symbols after each statement.
     * can also garbage collect any symbol that is not referenced after this statement
     */
    override fun visit(node: StatementNode): Symbol {
        val result = visit(node.children)

        // TODO(check if this is the last time a symbol is used)
        val lastUsedSymbols = ctx.lastUseInfo[node] ?: emptySet()
        lastUsedSymbols.mapNotNull { runtime.resolveSymbol(it) }
                .forEach { runtime.delete(it) }

        runtime.deleteTempSymbols()
        return result
    }

    override fun visit(node: CallStatementNode): Symbol {
        val args = node.params.map { visit(it) }

        // check for pre-defined stdlib function
        if (node.name in stdlib.functions) {
            return stdlib.invoke(node.name, args)
        }

        // lookup matching function and its params
        val functionNode = functions[node.name] ?: throw CompileException("unrecognized function ${node.name}")

        return functionCall(functionNode, args)
    }

    override fun visit(node: CallExpNode): Symbol {
        val args = node.params.map { visit(it) }

        // check for pre-defined stdlib function
        // todo check for void function in semantic analysis pass
        if (node.name in stdlib.functions) {
            return stdlib.functions[node.name]?.invoke(args) ?: throw Exception("function ${node.name} is void")
        }

        // lookup matching function and its params
        val functionNode = functions[node.name] ?: throw CompileException("unrecognized function ${node.name}")

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
                    args[i].isConstant -> assignConstant(param, args[i].value)
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
        var result: Symbol = NullSymbol
        if (fnNode.ret != null) {
            val ret = visit(fnNode.ret)
            runtime.exitScope()
            val cpy = runtime.createTempSymbol(ret.size, ret.type)
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
            rhs.isConstant -> {
                val lhs = runtime.createSymbol(node.lhs, node.storage, rhs.type, rhs.size)
                assignConstant(lhs, rhs.value)
            }
            rhs.isTemp -> {
                assert(rhs.hasAddress)
                runtime.rename(rhs, node.lhs)
            }
            else -> {
                val lhs = runtime.createSymbol(node.lhs, node.storage, rhs.type, rhs.size)
                assignVariable(lhs, rhs)
            }
        }
    }

    override fun visit(node: AssignmentNode): Symbol {
        val rhs = visit(node.rhs)
        val lhs = runtime.resolveSymbol(node.lhs) ?: runtime.createSymbol(node.lhs, StorageType.VAR, rhs.type, rhs.size)
        return assign(lhs, rhs)
    }

    private fun assign(lhs: Symbol, rhs: Symbol): Symbol {
        return when {
            rhs.isConstant -> assignConstant(lhs, rhs.value)
            else -> assignVariable(lhs, rhs)
        }
    }

    private fun assignVariable(lhs: Symbol, rhs: Symbol): Symbol {
        return when (rhs.type) {
            IntType -> cg.copyInt(lhs, rhs)
            StrType -> cg.copyStr(lhs, rhs)
            else -> throw CompileException("invalid rhs value: $rhs")
        }
    }

    private fun assignConstant(lhs: Symbol, rhs: Any): Symbol {
        return when (rhs) {
            is Int -> cg.loadImmediate(lhs, rhs)
            is String -> cg.loadImmediate(lhs, rhs)
            else -> throw CompileException("invalid rhs value: $rhs")
        }
    }

    override fun visit(node: IfStatementNode): Symbol {
        val condition = visit(node.condition)
        if (node.falseStatements.isEmpty()) {
            doIf(condition, node)
        } else {
            doIfElse(condition, node)
        }
        return NullSymbol
    }

    private fun doIf(condition: Symbol, node: IfStatementNode) {
        with (cg) {
            val cpy = runtime.createSymbol("&${condition.name}")
            assign(cpy, condition)

            cf.startIf(cpy)
            visit(node.trueStatements)
            cf.endIf(cpy)

            runtime.delete(cpy)
        }
    }

    private fun doIfElse(condition: Symbol, node: IfStatementNode) {
        with (cg) {
            val cpy = runtime.createSymbol("&${condition.name}")
            assign(cpy, condition)

            val elseFlag = runtime.createSymbol("${cpy.name}_else")
            loadImmediate(elseFlag, 1)
            cf.onlyIf(cpy, {
                setZero(elseFlag)
            })
            assign(cpy, condition)

            cf.startIf(cpy)
            visit(node.trueStatements)
            cf.endIf(cpy)

            cf.startElse(elseFlag)
            visit(node.falseStatements)
            cf.endElse(elseFlag)

            runtime.delete(cpy)
            runtime.delete(elseFlag)
        }
    }

    override fun visit(node: ConditionExpNode): Symbol {
        val condition = visit(node.condition)
        val trueExp = visit(node.trueExp)
        val falseExp = visit(node.trueExp)

        val cpy = runtime.createTempSymbol()
        val elseFlag = runtime.createTempSymbol()
        val ret = runtime.createTempSymbol()

        with (cg) {
            assign(cpy, condition)
            loadImmediate(elseFlag, 1)
            cf.onlyIf(cpy, {
                setZero(elseFlag)
                assign(ret, trueExp)
            })

            cf.onlyIf(elseFlag, {
                assign(ret, falseExp)
            })
        }

        runtime.delete(cpy)
        runtime.delete(elseFlag)
        return ret
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
        return Symbol.constant(node.value)
    }

    override fun visit(node: AtomStrNode): Symbol {
        return Symbol.constant(node.value)
    }

    override fun defaultValue(node: AstNode) = NullSymbol
}