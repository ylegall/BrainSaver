package org.ygl

import org.antlr.v4.runtime.tree.ParseTree
import org.ygl.BrainLoveParser.*


/**
 * http://stackoverflow.com/questions/23092081/antlr4-visitor-pattern-on-simple-arithmetic-example
 */
class BrainLoveVisitorImpl(val codegen: CodeGen) : BrainLoveBaseVisitor<Symbol?>()
{
    private val libraryFunctions = LibraryFunctions(codegen)

    /**
     * scan and register functions. look for the org.ygl.main function
     */
    override fun visitProgram(tree: ProgramContext?): Symbol? {
        val functionList = tree!!.getChild(0) as FunctionListContext
        var mainFunction: ParseTree? = null
        for (child in functionList.children) {
            val function = (child as FunctionContext)
            codegen.registerFunction(function)
            if (function.name.text == "main") {
                mainFunction = function
            }
        }
        mainFunction ?: throw Exception("no main function found")
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

    /**
     * 
     */
    override fun visitAssignmentStatement(ctx: AssignmentStatementContext?): Symbol? {
        ctx ?: throw Exception("null AssignmentStatementContext")
        val lhs = ctx.lhs.text
        val op = ctx.op.text
        val rhs = ctx.exp()

        val expResult = this.visit(rhs) ?: throw Exception("null rhs expression result")

        if (op == "=") {
            return assign(lhs, expResult)
        }

        with (codegen) {
            val lhsSymbol = currentScope().getSymbol(lhs) ?: throw Exception("undefined identifier $lhs")

            return when (op) {
                "+=" -> {
                    if (isConstant(expResult)) {
                        incrementBy(lhsSymbol, expResult.value as Int)
                    } else {
                        lhsSymbol.value = null
                        math.addTo(lhsSymbol, expResult)
                    }
                }
                "-=" -> {
                    if (isConstant(expResult)) {
                        incrementBy(lhsSymbol, -(expResult.value as Int))
                    } else {
                        lhsSymbol.value = null
                        math.subtractFrom(lhsSymbol, expResult)
                    }
                }
                "*=" -> math.multiplyBy(lhsSymbol, expResult)
                "/=" -> math.divideBy(lhsSymbol, expResult)
                "%=" -> math.modBy(lhsSymbol, expResult)
                else -> {
                    throw Exception("invalid assignment operator: " + op)
                }
            }
        }
    }

    private fun assign(lhs: String, rhs: Symbol): Symbol {
        // reassign symbol if sizes don't match
        var lhsSymbol = codegen.currentScope().getSymbol(lhs)
        if (lhsSymbol == null) {
            lhsSymbol = codegen.currentScope().createSymbol(lhs, rhs)
        } else if (lhsSymbol.size != rhs.size) {
            codegen.currentScope().delete(lhsSymbol)
            lhsSymbol = codegen.currentScope().createSymbol(lhs, rhs)
        }

        return when (rhs.type) {
            Type.INT -> {
                if (isConstant(rhs)) {
                    lhsSymbol.value = rhs.value
                    codegen.loadInt(lhsSymbol, rhs.value as Int)
                } else {
                    codegen.assign(lhsSymbol, rhs)
                }
            }
            Type.STRING -> {
                if (isConstant(rhs)) {
                    lhsSymbol.value = rhs.value
                    codegen.loadString(lhsSymbol, rhs.value as String)
                } else {
                    codegen.copyString(lhsSymbol, rhs)
                }
            }
            else -> {
                throw Exception("unsupported type")
            }
        }
    }

    override fun visitPrintStatement(ctx: PrintStatementContext?): Symbol? {
        ctx ?: throw Exception("null PrintStatementContext")
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
                val chars = unescape(str)
                codegen.io.printImmediate(chars)
                return null
            }
            else -> {
                val symbol = visit(exp) ?: throw Exception("null argument to print()")
                return codegen.print(symbol)
            }
        }
    }

    // TODO: improve
    private inline fun unescape(str: String): String {
        val withoutQuotes = str.trim().substring(1 .. str.length-2)
        val result = withoutQuotes.replace("\\n", "\n").replace("\\t", "\t")
        return result
    }

    override fun visitReadStatement(ctx: ReadStatementContext?): Symbol? {
        ctx ?: throw Exception("null ReadStatementContext")
        val id = ctx.Identifier().text
        return if (ctx.rd != null) {
            val sym = codegen.currentScope().getOrCreateSymbol(id, type = Type.INT)
            codegen.io.readChar(sym)
        } else if (ctx.rdint != null) {
            val sym = codegen.currentScope().getOrCreateSymbol(id, type = Type.INT)
            codegen.io.readInt(sym)
        } else {
            throw Exception("unsupported read call")
        }
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
        val chars = unescape(str)
        val tempSymbol = scope.getTempSymbol(Type.STRING, chars.length)
        codegen.loadString(tempSymbol, chars)
        return tempSymbol
    }

    override fun visitAtomInt(ctx: AtomIntContext?): Symbol? {
        val scope = codegen.currentScope()
        val valueStr = ctx?.IntegerLiteral()?.text ?: throw Exception("null integer literal")
        val value = Integer.parseInt(valueStr)
        if (value >= 256) throw Exception("integer overflow: $value")
        val tempSymbol = scope.getTempSymbol(Type.INT)
        codegen.loadInt(tempSymbol, value)
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
                val sym = codegen.currentScope().createSymbol(param.text, arguments[i])
                codegen.assign(sym, arguments[i])
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
            val cpy = codegen.currentScope().getTempSymbol(result.type, result.size)
            codegen.move(cpy, result)
        } else {
            return null
        }
    }

    override fun visitReturnStatement(ctx: ReturnStatementContext?): Symbol? {
        val ret = visit(ctx?.exp())
        codegen.emitReturn(ret)
        return ret
    }

    override fun visitOpExp(context: OpExpContext?): Symbol? {
        val ctx = checkNotNull(context, {" null op ctx"})
        val op = ctx.op.text
        val left  = visit(ctx.left)  ?: throw Exception("null exp result")
        val right = visit(ctx.right) ?: throw Exception("null exp result")

        if (isConstant(left) && isConstant(right)) {
            return constantFold(left, right, op)
        }

        with (codegen) {
            return when (op) {
                "+" ->  math.add(left, right)
                "-" ->  math.subtract(left, right)
                "*" ->  math.multiply(left, right)
                "/" ->  math.divide(left, right)
                "%" ->  math.mod(left, right)
                "<" ->  math.lessThan(left, right)
                ">" ->  math.greaterThan(left, right)
                "==" -> math.equal(left, right)
                "!=" -> math.notEqual(left, right)
                "<=" -> math.lessThanEqual(left, right)
                ">=" -> math.greaterThanEqual(left, right)
                "&&" -> math.and(left, right)
                "||" -> math.or(left, right)
                else -> throw Exception("invalid op $op")
            }
        }
    }

    private inline fun constantFold(lhs: Symbol, rhs: Symbol, op: String): Symbol {
        val left = lhs.value as Int
        val right = rhs.value as Int

        var result = when (op) {
            "+" -> left + right
            "-" -> left - right
            "*" -> left * right
            "/" -> left / right
            "%" -> left % right
            "<" ->  if (left < right)  1 else 0
            ">" ->  if (left > right)  1 else 0
            "==" -> if (left == right) 1 else 0
            "!=" -> if (left != right) 1 else 0
            "<=" -> if (left <= right) 1 else 0
            ">=" -> if (left >= right) 1 else 0
            "&&" -> if (left > 0 && right > 0) 1 else 0
            "||" -> if (left > 0 || right > 0) 1 else 0
            else -> throw Exception("invalid op $op")
        }

        result = Math.min(result, 255)
        result = Math.max(result, 0)

        val symbol = codegen.currentScope().getTempSymbol()
        symbol.value = result
        codegen.loadInt(symbol, result)
        return symbol
    }

    override fun visitNotExp(ctx: NotExpContext?): Symbol? {
        ctx ?: throw Exception("null NotExpContext")
        val right = visit(ctx.right) ?: throw Exception("null rhs")
        return codegen.math.not(right)
    }

    override fun visitIfStatement(context: IfStatementContext?): Symbol? {
        val ctx = checkNotNull(context, { "null IfStatementContext" })
        val condition = visit(ctx.condition) ?: throw Exception("null condition result")

        // constant branch elimination
        if (isConstant(condition)) {
            if (condition.value == 0) {
                if (ctx.falseStatements != null) visit(ctx.falseStatements)
            } else {
                visit(ctx.trueStatements)
            }
            return null
        }

        if (ctx.falseStatements != null && !ctx.falseStatements.isEmpty) {
            val tmp = codegen.startIf(condition)
            for (stmt in ctx.trueStatements.statement()) {
                visit(stmt)
            }
            codegen.endIf(tmp)
            codegen.startElse(condition)
            for (stmt in ctx.falseStatements.statement()) {
                visit(stmt)
            }
            codegen.endElse(condition)
        } else {
            val ifFlag = codegen.startIf(condition)
            visit(ctx.trueStatements)
            codegen.endIf(ifFlag)
        }
        codegen.currentScope().popConditionFlag()
        return null
    }

    override fun visitWhileStatement(ctx: WhileStatementContext?): Symbol? {
        ctx ?: throw Exception("null WhileStatementContext")
        var condition = visit(ctx.condition) ?: throw Exception("null condition result")

        codegen.startWhile(condition)
        for (stmt in ctx.body.statement()) {
            visit(stmt)
        }
        condition = visit(ctx.condition) ?: throw Exception("null condition result")
        codegen.endWhile(condition)
        return null
    }

//    private inline fun canUnrollLoop(condition: Symbol, body: List<StatementContext>): Boolean {
//        if (!isConstant(condition)) return false
//        if (condition.value as Int > 8) return false // TODO: make this a configurable setting
//        for (stmt in body) {
//            if (stmt is PrintStatementContext) return false
//            if (stmt is CallStatementContext)  return false
//        }
//        return true
//    }

//    private fun unrollLoop(condition: Symbol, body: List<StatementContext>) {
//        for (i in 1 .. condition.value as Int) {
//            body.forEach { visit(it) }
//        }
//    }

    private inline fun isConstant(symbol: Symbol): Boolean {
        return !codegen.currentScope().inConditionalScope() && symbol.value != null
    }

}