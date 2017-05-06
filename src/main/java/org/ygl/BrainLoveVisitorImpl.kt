package org.ygl

import org.antlr.v4.runtime.tree.ParseTree
import org.ygl.BrainLoveParser.*


/**
 * http://stackoverflow.com/questions/23092081/antlr4-visitor-pattern-on-simple-arithmetic-example
 */
class BrainLoveVisitorImpl(val codegen: CodeGen) : BrainLoveBaseVisitor<Symbol?>()
{
    /**
     * scan and register functions. look for the org.ygl.main function
     */
    override fun visitProgram(tree: ProgramContext?): Symbol? {
        val functionList = tree!!.getChild(0) as FunctionListContext
        var mainFunction: ParseTree? = null
        for (child in functionList.children) {
            var function = (child as FunctionContext)
            codegen.registerFunction(function)
            if (function.name.text == "main") {
                mainFunction = function
            }
        }
        if (mainFunction == null) throw Exception("no main function found")
        codegen.enterScope()
        return visit(mainFunction)
    }

    override fun visitFunction(ctx: FunctionContext?): Symbol? {
        val result = visitChildren(ctx)
        codegen.closeFunction(ctx?.name?.text ?: "")
        return result
    }

    override fun visitStatement(ctx: StatementContext?): Symbol? {
        val result = super.visitStatement(ctx)
        codegen.currentScope().deleteTemps()
        return result
    }

    override fun visitAssignmentStatement(ctx: AssignmentStatementContext?): Symbol? {
        if (ctx == null) throw Exception("null AssignmentStatementContext")
        val lhs = ctx.lhs.text
        val op = ctx.op.text
        val rhs = ctx.exp()

        val expResult = this.visit(rhs) ?: throw Exception("null rhs expression result")

        // TODO: implement operators
        val result = when (op) {
            "="  -> codegen.assign(lhs, expResult)
            "+=" -> codegen.opAssign(lhs, expResult, codegen::addTo)
            "-=" -> codegen.opAssign(lhs, expResult, codegen::subtractFrom)
            "*=" -> codegen.opAssign(lhs, expResult, codegen::multiplyBy)
            "/=" -> codegen.opAssign(lhs, expResult, codegen::divideBy)
            "%=" -> codegen.opAssign(lhs, expResult, codegen::modBy)
            else -> {
                throw Exception("invalid assignment operator: " + op)
            }
        }
        return result
    }

    override fun visitPrintStatement(ctx: PrintStatementContext?): Symbol? {
        if (ctx == null) throw Exception("null PrintStatementContext")
        val scope = codegen.currentScope()

        val exp = ctx.exp()
        when (exp) {
            is AtomIdContext -> {
                val symbol = scope.getSymbol(ctx.exp().text) ?:
                        throw Exception("undefined identifier: ${ctx.exp().text}")
                return codegen.print(symbol)
            }
            is AtomStrContext -> {
                val str = (exp as AtomStrContext).text
                val chars = removeQuotes(str)
                codegen.printImmediate(chars)
                return null
            }
            else -> {
                val symbol = visit(exp) ?: throw Exception("null argument to print()")
                return codegen.print(symbol)
            }
        }
    }

    private fun removeQuotes(str: String) = str.trim().substring(1 .. str.length-2)

    override fun visitReadStatement(ctx: ReadStatementContext?): Symbol? {
        if (ctx == null) throw Exception("null ReadStatementContext")
        val id = ctx.Identifier().text
        var sym = codegen.currentScope().getOrCreateSymbol(id)
        if (ctx.rd != null) {
            codegen.readChar(sym)
        } else if (ctx.rdint != null) {
            codegen.readInt(sym)
        }
        return sym
    }

    override fun visitAtomId(ctx: AtomIdContext?): Symbol? {
        val scope = codegen.currentScope()
        val symbolName = ctx?.Identifier()?.text ?: throw Exception("null atom identifier")
        // check for undefined identifier
        val symbol = scope.getSymbol(symbolName) ?: throw Exception("undefined identifier: $symbolName")
        return symbol
    }

    override fun visitAtomStr(ctx: AtomStrContext?): Symbol? {
        val scope = codegen.currentScope()
        val str = ctx?.StringLiteral()?.text ?: throw Exception("null string literal")
        val chars = removeQuotes(str)
        val tempSymbol = scope.getTempSymbol(Type.STRING, chars.length)
        codegen.set(tempSymbol, chars)
        tempSymbol.value = chars
        return tempSymbol
    }

    override fun visitAtomInt(ctx: AtomIntContext?): Symbol? {
        val scope = codegen.currentScope()
        val valueStr = ctx?.IntegerLiteral()?.text ?: throw Exception("null integer literal")
        val value = Integer.parseInt(valueStr)
        if (value >= 256) throw Exception("integer overflow: $value")
        val tempSymbol = scope.getTempSymbol(Type.INT)
        tempSymbol.value = value
        return tempSymbol
    }

    override fun visitParenExp(ctx: ParenExpContext?): Symbol? {
        return visit(ctx?.exp())
    }

    override fun visitCallStatement(ctx: CallStatementContext?): Symbol? {
        val name = ctx?.funcName?.text ?: throw Exception("null CallStatementContext")
        val args = ctx.expList().exp()
        functionCall(name, args)
        return null
    }

    override fun visitCallExp(ctx: CallExpContext?): Symbol? {
        val name = ctx?.funcName?.text ?: throw Exception("null CallExpContext")
        val args = ctx.expList()?.exp()
        return functionCall(name, args)
    }

    private fun functionCall(funcName: String, args: List<BrainLoveParser.ExpContext>?): Symbol? {
        // lookup matching function and its params
        val function = codegen.functions[funcName] ?: throw Exception("unrecognized function: $funcName")

        // collect arguments for function call
        if (args != null) {
            val params = function.ctx.params?.identifierList()?.Identifier()
            if (params == null || args.size != params.size) {
                throw Exception("wrong number of arguments to $funcName")
            }

            val arguments = ArrayList<Symbol>(params.size)
            for (exp in args) {
                val expResult = visit(exp) ?: throw Exception("null call argument: ${exp.text}")
                arguments.add(expResult)
            }

            codegen.enterScope()
            codegen.commentLine("call $funcName")
            // create new scope and copy expression args into function param variables
            for (i in 0 until params.size) {
                val param = params[i]
                codegen.currentScope().createSymbol(param.text, arguments[i])
            }
        } else {
            codegen.enterScope()
            codegen.commentLine("call $funcName")
        }

        // execute statements in function body:
        visit(function.ctx)

        val result = codegen.currentScope().getSymbol(org.ygl.returnSymbolName)
        codegen.exitScope()
        return if (result != null) {
            codegen.assign(codegen.currentScope().getTempSymbol(result.type, result.size), result)
        } else {
            return null
        }
    }

    override fun visitReturnStatement(ctx: ReturnStatementContext?): Symbol? {
        val ret = visit(ctx?.exp())
        codegen.emitReturn(ret)
        return ret
    }

    override fun visitOpExp(ctx: OpExpContext?): Symbol? {
        if (ctx == null) throw Exception("null op ctx")
        val op = ctx.op.text
        val left  = visit(ctx.left)  ?: throw Exception("null exp result")
        val right = visit(ctx.right) ?: throw Exception("null exp result")

        // TODO handle overflow
        if (left.isConstant() && right.isConstant()) {
            val symbol = codegen.currentScope().getTempSymbol()
            symbol.value = when (op) {
                "+" -> left.value as Int + right.value as Int
                "-" -> left.value as Int - right.value as Int
                "*" -> left.value as Int * right.value as Int
                "/" -> left.value as Int / right.value as Int
                "%" -> left.value as Int % right.value as Int
                else -> throw Exception("invalid op $op")
            }
            return symbol
        } else {
            return when (op) {
                "+" -> codegen.add(left, right)
                "-" -> codegen.subtract(left, right)
                "*" -> codegen.multiply(left, right)
                "/" -> codegen.divide(left, right)
                "%" -> codegen.mod(left, right)
                else -> throw Exception("invalid op $op")
            }
        }
    }

    override fun visitIfStatement(ctx: IfStatementContext?): Symbol? {
        if (ctx == null) throw Exception("null IfStatementContext")
        val condition = visit(ctx.condition()) ?: throw Exception("null condition result")

        if (condition.isConstant()) {
            if (condition.value == 0) {
                return visit(ctx.falseStatements)
            } else {
                return visit(ctx.trueStatements)
            }
        } else {
            // TODO
            if (ctx.falseStatements != null && !ctx.falseStatements.isEmpty) {
                codegen.startIf(condition)
                visit(ctx.trueStatements)
                codegen.endIf(condition)
                codegen.startElse(condition)
                visit(ctx.falseStatements)
                codegen.endElse(condition)
            } else {
                codegen.startIf(condition)
                visit(ctx.trueStatements)
                codegen.endIf(condition)
            }
            codegen.currentScope().popConditionFlag()
            return null
        }
    }

    override fun visitCondition(ctx: ConditionContext?): Symbol? {
        if (ctx == null) throw Exception("null ConditionContext")

        val left  = visit(ctx.left)  ?: throw Exception("null left exp")
        val right = visit(ctx.right) ?: throw Exception("null exp result")
        val op = ctx.op.text

        if (left.isConstant() && right.isConstant()) {
            val symbol = codegen.currentScope().getTempSymbol()
            symbol.value = when (op) {
                "<" -> if ((left.value as Int) < (right.value as Int)) 1 else 0
                ">" -> if ((left.value as Int) > (right.value as Int)) 1 else 0
                "==" -> if ((left.value as Int) == (right.value as Int)) 1 else 0
                "!=" -> if ((left.value as Int) != (right.value as Int)) 1 else 0
                "<=" -> if ((left.value as Int) <= (right.value as Int)) 1 else 0
                ">=" -> if ((left.value as Int) >= (right.value as Int)) 1 else 0
                else -> throw Exception("invalid op $op")
            }
            return symbol
        } else {
            return when (op) {
                // TODO
                "<" ->  codegen.lessThan(left, right)
                ">" ->  codegen.greaterThan(left, right)
                "==" -> codegen.equal(left, right)
//                "!=" -> codegen.notEqual(left, right)
                "<=" -> codegen.lessThanEqual(left, right)
                ">=" -> codegen.greaterThanEqual(left, right)
                else -> throw Exception("invalid op $op")
            }
        }
    }
}