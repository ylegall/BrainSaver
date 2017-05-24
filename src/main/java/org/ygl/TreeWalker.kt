package org.ygl

import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.ygl.BrainSaverParser.*

/**
 * http://stackoverflow.com/questions/23092081/antlr4-visitor-pattern-on-simple-arithmetic-example
 */
class TreeWalker(val codegen: CodeGen) : BrainSaverBaseVisitor<Symbol?>()
{
    private val libraryFunctions = LibraryFunctions(codegen, this)

    /**
     * scan and register functions. look for the org.ygl.main function
     */
    override fun visitProgram(tree: BrainSaverParser.ProgramContext?): Symbol? {
        val functionList = tree!!.getChild(0) as FunctionListContext
        var mainFunction: ParseTree? = null
        for (child in functionList.children) {
            val function = (child as FunctionContext)
            val functionName = function.name.text
            val isVoid = function.functionBody().ret == null
            if (codegen.functions.containsKey(functionName)) {
                throw Exception("duplicate function: $functionName")
            } else {
                val newFunction = Function(functionName, function, isVoid)
                codegen.functions.put(functionName, newFunction)
            }

            if (functionName == "main") {
                mainFunction = function
            }
        }
        mainFunction ?: throw Exception("no main function found")
        codegen.enterScope()
        return visit(mainFunction)
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
        return assign(lhsSymbol, rhs)
    }

    private fun assign(lhs: Symbol, rhs: Symbol): Symbol {
        return when (rhs.type) {
            Type.INT -> {
                if (isConstant(rhs)) {
                    lhs.value = rhs.value
                    codegen.loadInt(lhs, rhs.value as Int)
                } else {
                    codegen.assign(lhs, rhs)
                }
            }
            Type.STRING -> {
                if (rhs.isTemp()) {
                    // move operation
                    codegen.commentLine("rename $rhs to $lhs")
                    codegen.currentScope().delete(lhs)
                    codegen.currentScope().rename(rhs, lhs.name)
                    return rhs
                } else if (isConstant(rhs)) {
                    lhs.value = rhs.value
                    codegen.loadString(lhs, rhs.value as String)
                } else {
                    codegen.copyString(lhs, rhs)
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

        val exp = if (ctx.exp().childCount == 1 ) ctx.exp().getChild(0) else ctx.exp()
        when (exp) {
            is AtomIdContext -> {
                val symbol = scope.getSymbol(ctx.exp().text) ?:
                        throw Exception("undefined identifier: ${ctx.exp().text}")
                codegen.io.print(symbol)
            }
            is AtomStrContext -> {
                val chars = unescape(exp.text)
                codegen.io.printImmediate(chars)
            }
            else -> {
                val symbol = visit(exp) ?: throw Exception("null argument to print()")
                codegen.io.print(symbol)
            }
        }
        return null
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

        if (name in libraryFunctions.procedures) {
            val expList = args?.map { visit(it) } ?: listOf()
            return libraryFunctions.invoke(name, expList)
        }

        // lookup matching function and its params
        val function = codegen.functions[name] ?: throw Exception("unrecognized function: $name")

        functionCall(function, args)
        return null
    }

    override fun visitCallExp(ctx: CallExpContext?): Symbol? {
        val name = ctx?.funcName?.text ?: throw Exception("null CallExpContext")
        val args = ctx.expList()?.exp()

        if (name in libraryFunctions.procedures) {
            val expList = args?.map { visit(it) } ?: listOf()
            return libraryFunctions.invoke(name, expList)
        }

        // lookup matching function and its params
        val function = codegen.functions[name] ?: throw Exception("unrecognized function: $name")
        if (function.isVoid) {
            throw ParseCancellationException("line ${ctx.start.line}: function '$name' is void")
        }

        return functionCall(function, args)
    }

    private fun functionCall(function: Function, args: List<BrainSaverParser.ExpContext>?): Symbol? {

        // collect arguments for function call
        if (args != null) {
            val params = function.ctx.params?.identifierList()?.Identifier()
            if (params == null || args.size != params.size) {
                throw Exception("wrong number of arguments to ${function.name}")
            }

            val arguments = ArrayList<Symbol>(params.size)
            for (exp in args) {
                val expResult = visit(exp) ?: throw Exception("null call argument: ${exp.text}")
                arguments.add(expResult)
            }

            codegen.enterScope()
            codegen.commentLine("call ${function.name}")
            // create new scope and copy expression args into function param variables
            for (i in 0 until params.size) {
                val param = params[i]
                val sym = codegen.currentScope().createSymbol(param.text, arguments[i])
                assign(sym, arguments[i])
            }
        } else {
            codegen.enterScope()
            codegen.commentLine("call ${function.name}")
        }

        // execute statements in function body:
        function.ctx.body.statement()?.forEach {
            visit(it)
        }

        var result: Symbol? = null
        if (function.ctx.body.ret != null) {
            val ret = visit(function.ctx.body.ret) ?: throw Exception("null return value")
            codegen.exitScope()
            val cpy = codegen.currentScope().getTempSymbol(ret.type, ret.size)
            result = codegen.move(cpy, ret)
        }
        codegen.commentLine("end call ${function.name}")
        return result
    }

    override fun visitReturnStatement(ctx: ReturnStatementContext?): Symbol? {
        return visit(ctx?.exp())
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

    // TODO: rename variables to prevent them from being collected
    override fun visitIfStatement(ctx: IfStatementContext?): Symbol? {
        ctx ?: throw Exception("null IfStatementContext")

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
            codegen.currentScope().rename(tmp, "&${tmp.name}")
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

        if (isConstant(condition) && (condition.value as Int) == 0) {
            return null
        }

        codegen.currentScope().rename(condition, "&" + condition.name)
        codegen.startWhile(condition)
        for (stmt in ctx.body.statement()) {
            visit(stmt)
        }
        val c2 = visit(ctx.condition) ?: throw Exception("null condition result")
        codegen.assign(condition, c2)
        codegen.endWhile(condition)
        codegen.currentScope().delete(condition)
        return null
    }

    override fun visitForStatement(ctx: ForStatementContext?): Symbol? {
        ctx ?: throw Exception("null ForStatementContext")

        val start = visit(ctx.start)!!
        val stop = visit(ctx.stop)!!
        val step = if (ctx.step != null) {
            visit(ctx.step)!!
        } else {
            codegen.loadInt(codegen.currentScope().createSymbol("step${ctx.sourceInterval.a}", value=1), 1)
        }

        // TODO: hack to prevent temp symbols from being deleted:
        val deleteSet = HashSet<Symbol>()
        listOf(start, stop, step).forEach {
            val sym = codegen.currentScope().getSymbol(it.name)!!
            if (sym.isTemp()) {
                codegen.currentScope().rename(sym, "&${sym.name}")
                deleteSet.add(it)
            }
        }

        val loopVar = codegen.currentScope().createSymbol(ctx.loopVar.text)

        // try to unroll loop
        if (canUnrollLoop(start, stop, step, ctx.body.statement())) {
            codegen.commentLine("unrolling loop")
            codegen.loadInt(loopVar, start.value as Int)
            for (i in (start.value as Int) .. (stop.value as Int) step (step.value as Int)) {
                for (stmt in ctx.body.statement()) {
                    visit(stmt)
                }
                codegen.incrementBy(loopVar, step.value as Int)
            }
        } else {
            val condition = codegen.currentScope().pushConditionFlag()
            codegen.startFor(loopVar, start, stop, condition)
            for (stmt in ctx.body.statement()) {
                visit(stmt)
            }
            codegen.endFor(loopVar, stop, step, condition)
            codegen.currentScope().popConditionFlag()
        }
        // can't delete these if they refer to an existing symbol
        deleteSet.forEach { codegen.currentScope()::delete }
        return null
    }

    private fun canUnrollLoop(start: Symbol, stop: Symbol, step: Symbol, body: List<StatementContext>): Boolean {
        if (body.size > 8) return false
        if (!isConstant(start) || !isConstant(stop) || !isConstant(step)) return false
        if ((stop.value as Int - start.value as Int) / step.value as Int >= 10) return false
        for (stmt in body) {
            val child = stmt.getChild(0)
            when (child) {
                is PrintStatementContext -> return false
                is CallStatementContext  -> return false
                is ForStatementContext   -> return false
                is WhileStatementContext -> return false
            }
        }
        return true
    }

    override fun visitArrayConstructor(ctx: ArrayConstructorContext?): Symbol? {
        ctx ?: throw Exception("null ArrayConstructorContext")
        val name = ctx.lhs.text
        val size = Integer.parseInt(ctx.arraySize.text)
        return createArray(name, size)
    }

    override fun visitArrayLiteral(ctx: ArrayLiteralContext?): Symbol? {
        ctx ?: throw Exception("null ArrayLiteralContext")
        val name = ctx.lhs.text
        val values = ctx.integerList().IntegerLiteral()
        val size = values.size
        return createArray(name, size, values)
    }

    private inline fun createArray(name: String, size: Int, values: List<TerminalNode>? = null): Symbol {
        if (size < 1 || size >= 256) throw Exception("array size must be between 1 and 256")
        val array = codegen.currentScope().createSymbol(name, size + 4, Type.INT)
        if (values != null) {
            values.map { Integer.parseInt(it.text) }.
                    forEachIndexed { i, v -> codegen.loadInt(array.offset(i), value = v) }
        } else {
            codegen.setZero(array)
        }
        return array
    }

    override fun visitArrayWriteStatement(ctx: ArrayWriteStatementContext?): Symbol? {
        ctx ?: throw Exception("null ArrayWriteStatementContext")
        val array = checkNotNull(codegen.currentScope().getSymbol(ctx.array.text), { "null ${ctx.array.text}" })
        val index = checkNotNull(visit(ctx.idx), { "null ${ctx.idx.text}" })
        val value = checkNotNull(visit(ctx.rhs), { "null ${ctx.rhs.text}" })

        // TODO: type checking?

        if (isConstant(index)) {
            if (isConstant(value)) {
                if (value.value is String) {
                    val char = (value.value as String)[0]
                    return codegen.loadInt(array.offset(index.value as Int), char.toInt())
                } else {
                    return codegen.loadInt(array.offset(index.value as Int), value.value as Int)
                }
            } else {
                return codegen.assign(array.offset(index.value as Int), value)
            }
        } else {
            codegen.writeArray(array, index, value)
            return null
        }
    }

    override fun visitArrayReadExp(ctx: ArrayReadExpContext?): Symbol? {
        ctx ?: throw Exception("null ArrayReadExpContext")
        val array = checkNotNull(codegen.currentScope().getSymbol(ctx.array.text), { "null ${ctx.array.text}" })
        val index = checkNotNull(visit(ctx.idx), { "null ${ctx.idx.text}" })

        if (isConstant(index)) {
            val ret = codegen.currentScope().getTempSymbol()
            return codegen.assign(ret, array.offset(index.value as Int))
        } else {
            return codegen.readArray(array, index)
        }
    }

    override fun visitDebugStatement(ctx: DebugStatementContext?): Symbol? {
        ctx ?: throw Exception("null DebugStatementContext")
        ctx.idList.Identifier().map {
            codegen.currentScope().getSymbol( it.text )
        }
        .filterNotNull()
        .forEach {
            codegen.debug(it, "$it = ")
        }
        return null
    }

    inline fun isConstant(symbol: Symbol): Boolean {
        return codegen.options.optimize &&
                !codegen.currentScope().inConditionalScope() &&
                symbol.value != null
    }

}
