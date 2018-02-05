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
                .forEach {
                    if (it.name in functions) {
                        throw CompileException("function ${it.name} redefined")
                    }
                    functions[it.name] = it
                }

        // add global variables:
        runtime.enterScope()
        node.children.filterIsInstance<GlobalVariableNode>()
                .forEach { visit(it) }

        functions["main"]
                ?.let { fn -> fn.statements.forEach { visit(it) } }
                ?: throw CompileException("no main function found")

        return NullSymbol
    }

    override fun visit(node: GlobalVariableNode): Symbol {
        val rhs = visit(node.rhs)
        return assignNew(node.lhs, rhs)
    }

    /**
     * visit the statement.
     * can delete all temp symbols after each statement.
     * can also garbage collect any symbol that is not referenced after this statement
     */
    override fun visit(node: StatementNode): Symbol {
        node.children.forEach { visit(it) }

        val lastUsedSymbols = ctx.lastUseInfo[node] ?: emptySet()
        lastUsedSymbols.mapNotNull { runtime.resolveSymbol(it) }
                .forEach { runtime.delete(it) }

        runtime.deleteTempSymbols()
        return NullSymbol
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

            runtime.enterScope()
            cg.commentLine("call ${fnNode.name}")

            // create new scope and copy expression args into function param variables
            for (i in 0 until params.size) {
                val param = runtime.createSymbol(params[i], StorageType.VAL, args[i].type)
                when {
                    args[i].isConstant -> assignConstant(param, args[i].value)
                    else -> assignVariable(param, args[i])
                }
            }

        } else {
            runtime.enterScope()
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
        if (rhs == NullSymbol) {
            // TODO: will have to update the size/type upon re-assignment
            runtime.createSymbol(node.lhs, node.storage)
            return NullSymbol
        }
        return assignNew(node.lhs, rhs)
    }

    override fun visit(node: AssignmentNode): Symbol {
        val rhs = visit(node.rhs)
        val lhs = runtime.resolveSymbol(node.lhs) ?: throw Exception("undefined identifier: ${node.lhs}")
        return assign(lhs, rhs)
    }

    private fun assign(lhs: Symbol, rhs: Symbol): Symbol {
        return when {
            rhs.isConstant -> assignConstant(lhs, rhs.value)
            rhs.isTemp -> cg.move(lhs, rhs)
            else -> assignVariable(lhs, rhs)
        }
    }

    private fun assignNew(name: String, rhs: Symbol): Symbol {
        return when {
            rhs.isConstant -> {
                val lhs = runtime.createSymbol(name, StorageType.VAR, rhs.type, rhs.size)
                assignConstant(lhs, rhs.value)
            }
            rhs.isTemp -> {
                assert(rhs.hasAddress)
                runtime.rename(rhs, name)
            }
            else -> {
                val lhs = runtime.createSymbol(name, StorageType.VAR, rhs.type, rhs.size)
                assignVariable(lhs, rhs)
            }
        }
    }

    private fun assignSafe(lhs: Symbol, rhs: Symbol): Symbol {
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
            is Int -> cg.load(lhs, rhs)
            is String -> cg.load(lhs, rhs)
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
            assignSafe(cpy, condition)

            cf.startIf(cpy)
            node.trueStatements.forEach { visit(it) }
            cf.endIf(cpy)

            runtime.delete(cpy)
        }
    }

    private fun doIfElse(condition: Symbol, node: IfStatementNode) {
        with (cg) {
            val cpy = runtime.createSymbol("&${condition.name}")
            assignSafe(cpy, condition)

            val elseFlag = runtime.createSymbol("&${cpy.name}_else")
            load(elseFlag, 1)
            cf.onlyIf(cpy, {
                zero(elseFlag)
            })
            assign(cpy, condition)

            cf.startIf(cpy)
            node.trueStatements.forEach { visit(it) }
            cf.endIf(cpy)

            cf.startElse(elseFlag)
            node.falseStatements.forEach { visit(it) }
            cf.endElse(elseFlag)

            runtime.delete(cpy)
            runtime.delete(elseFlag)
        }
    }

    override fun visit(node: WhileStatementNode): Symbol {
        var condition = visit(node.condition)
        assert(!condition.isConstant, { "constant loop condition should have been removed" })
        val cpy = assignNew("&${condition.name}", condition)

        cg.cf.loop(cpy, {
            node.statements.forEach { visit(it) }
            condition = visit(node.condition)
            assign(cpy, condition)
        })

        runtime.delete(cpy)
        return NullSymbol
    }

    // TODO: test
    override fun visit(node: ForStatementNode): Symbol {
        val start = visit(node.start)
        val stop = visit(node.stop)
        val step = visit(node.inc)
        val counter = assignNew(node.counter, start)

        var condition = assignNew("&${counter.name}", cg.math.lessThanEqual(counter, stop))
        cg.cf.loop(condition, {
            node.statements.forEach { visit(it) }
            cg.math.addTo(counter, step)
            condition = assign(condition, cg.math.lessThanEqual(counter, stop))
        })

        runtime.delete(counter)
        runtime.delete(condition)
        return NullSymbol
    }

    override fun visit(node: ConditionExpNode): Symbol {
        val condition = visit(node.condition)
        assert(!condition.isConstant)

        val elseFlag = runtime.createTempSymbol()
        val ret = runtime.createTempSymbol()

        with (cg) {
            load(elseFlag, 1)

            cf.onlyIf(condition, {
                zero(elseFlag)
                val trueExp = visit(node.trueExp)
                assign(ret, trueExp)
            })

            cf.onlyIf(elseFlag, {
                val falseExp = visit(node.falseExp)
                assign(ret, falseExp)
            })
        }

        runtime.delete(elseFlag)
        return ret
    }

    override fun visit(node: BinaryExpNode): Symbol {
        var left = visit(node.left)
        var right = visit(node.right)

        // TODO: stregnth reduce to inc()/dec() for add/sub with 1 constant operand

        if (!left.hasAddress) {
            val temp = runtime.createTempSymbol(left.size, left.type)
            left = assign(temp, left)
        }

        if (!right.hasAddress) {
            val temp = runtime.createTempSymbol(right.size, right.type)
            right = assign(temp, right)
        }

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