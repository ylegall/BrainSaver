package org.ygl

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.TerminalNode
import org.ygl.BrainSaverParser.*
import java.util.*

/**
 * http://stackoverflow.com/questions/23092081/antlr4-visitor-pattern-on-simple-arithmetic-example
 */
class TreeWalker(
        val cg: CodeGen,
        val usageInfo: UsageInfoMap
) : BrainSaverBaseVisitor<Symbol?>()
{
    private val libraryFunctions = LibraryFunctions(cg, this)
    private val staleSymbols = ArrayList<String>()

    /**
     * scan and register functions. look for the org.ygl.main function
     */
    override fun visitProgram(tree: BrainSaverParser.ProgramContext?): Symbol? {
        val mainFunction = usageInfo["main"]!!.function.ctx
        cg.enterScope("main")
        val ret = visit(mainFunction)
        return ret
    }

    override fun visitStatement(ctx: StatementContext?): Symbol? {
        ctx ?: throw Exception("null StatementContext")
        val result = super.visitStatement(ctx)
        cg.currentScope().deleteTemps()
        if (cg.options.optimize) {
            val staleVariables = usageInfo[currentFunction()]?.lastSymbolsUsedMap?.get(ctx)
            staleVariables?.let {
                if (cg.options.verbose) {
                    println("line ${ctx.start.line}: auto deleting $staleVariables")
                }
                scheduleDeletion(it)
            }
        }
        return result
    }

    override fun visitAssignmentStatement(ctx: AssignmentStatementContext?): Symbol? {
        ctx ?: throw Exception("null AssignmentStatementContext")

        val lhs = ctx.lhs.text
        val op = ctx.op.text
        val rhs = ctx.exp()

        if (lhs in usageInfo[currentFunction()]!!.unusedSymbols) {
            if (cg.options.verbose) println("ignoring unused symbol $lhs")
            return null
        }

        val expResult = this.visit(rhs) ?: throw Exception("null rhs expression result")

        if (op == "=") {
            return assign(lhs, expResult)
        }

        with (cg) {
            val lhsSymbol = currentScope().getSymbol(lhs) ?: throw Exception("undefined identifier $lhs")

            if (isConstant(lhsSymbol) && isConstant(expResult)) {
                return constantAssignOp(lhsSymbol, expResult, op)
            }

            lhsSymbol.value = null

            return when (op) {
                "+=" -> {
                    if (isConstant(expResult)) {
                        incrementBy(lhsSymbol, expResult.value as Int)
                    } else {
                        math.addTo(lhsSymbol, expResult)
                    }
                }
                "-=" -> {
                    if (isConstant(expResult)) {
                        incrementBy(lhsSymbol, -(expResult.value as Int))
                    } else {
                        math.subtractFrom(lhsSymbol, expResult)
                    }
                }
                "*=" -> if (isConstant(expResult)) {
                    strengthReduce(lhsSymbol, op, expResult)
                } else {
                    math.multiplyBy(lhsSymbol, expResult)
                }
                "/=" -> if (isConstant(expResult)) {
                    strengthReduce(lhsSymbol, op, expResult)
                } else {
                    math.divideBy(lhsSymbol, expResult)
                }
                "%=" -> math.modBy(lhsSymbol, expResult)
                else -> {
                    throw Exception("invalid assignment operator: " + op)
                }
            }
        }
    }

    private fun strengthReduce(lhs: Symbol, op: String, rhs: Symbol): Symbol {
        return when (Pair(op, rhs.value)) {
            Pair("*=", 0) -> {
                cg.setZero(lhs)
            }
            Pair("*=", 1) -> {
                lhs // no-op
            }
            Pair("*=", 2) -> {
                cg.math.addTo(lhs, lhs)
            }
            Pair("/=", 1) -> {
                lhs // no-op
            }
            else -> {
                if (op == "*=") {
                    cg.math.multiplyBy(lhs, rhs)
                } else if (op == "/=") {
                    cg.math.divideBy(lhs, rhs)
                } else {
                    throw Exception("invalid assignment operator: " + op)
                }
            }
        }
    }

    private fun assign(lhs: String, rhs: Symbol): Symbol {
        if (lhs == rhs.name) return rhs // no-op

        // reassign symbol if sizes don't match
        var lhsSymbol = cg.currentScope().getSymbol(lhs)
        if (lhsSymbol == null) {
            lhsSymbol = cg.currentScope().createSymbol(lhs, rhs)
        } else if (lhsSymbol.size != rhs.size) {
            cg.currentScope().delete(lhsSymbol)
            lhsSymbol = cg.currentScope().createSymbol(lhs, rhs)
        }
        return assign(lhsSymbol, rhs)
    }

    private fun assign(lhs: Symbol, rhs: Symbol): Symbol {
        val lc = cg.currentScope().loopContexts
        return if (isConstant(rhs)) {
            if ( lc.isEmpty() || (lc.peek() !is IfStatementContext)) {
                lhs.value = rhs.value
                if (rhs.isTemp()) {
                    rename(rhs, lhs)
                } else {
                    assignConstant(lhs, rhs)
                }
            } else {
                assignConstant(lhs, rhs)
            }
        } else {
            assignVariable(lhs, rhs)
        }
    }

    private fun assignVariable(lhs: Symbol, rhs: Symbol): Symbol {
        lhs.value = null
        return when (rhs.type) {
            Type.INT    -> cg.assign(lhs, rhs)
            Type.STRING -> if (rhs.isTemp()) rename(rhs, lhs) else cg.copyString(lhs, rhs)
            else        -> throw Exception("unsupported type")
        }
    }

    private fun assignConstant(lhs: Symbol, rhs: Symbol): Symbol {
        return when (rhs.type) {
            Type.INT    -> cg.loadInt(lhs, rhs.value as Int)
            Type.STRING -> cg.loadString(lhs, rhs.value as String)
            else        -> throw Exception("unsupported type")
        }
    }

    private fun rename(rhs: Symbol, lhs: Symbol): Symbol {
        with (cg) {
            commentLine("rename $rhs to ${lhs.name}")
            currentScope().delete(lhs)
            currentScope().rename(rhs, lhs.name)
        }
        return rhs
    }

    override fun visitPrintStatement(ctx: PrintStatementContext?): Symbol? {
        ctx ?: throw Exception("null PrintStatementContext")
        val scope = cg.currentScope()

        val exp = if (ctx.exp().childCount == 1 ) ctx.exp().getChild(0) else ctx.exp()
        when (exp) {
            is AtomIdContext -> {
                val symbol = scope.getSymbol(ctx.exp().text) ?:
                        throw Exception("undefined identifier: ${ctx.exp().text}")
                if (isConstant(symbol) && symbol.type == Type.INT) {
                    cg.io.printImmediate(symbol.value.toString())
                } else {
                    cg.io.print(symbol)
                }
            }
            is AtomStrContext -> {
                cg.io.printImmediate(unescape(exp.text))
            }
            is AtomIntContext -> {
                cg.io.printImmediate(exp.text)
            }
            else -> {
                val symbol = visit(exp) ?: throw Exception("null argument to print()")
                cg.io.print(symbol)
            }
        }
        return null
    }

    private fun unescape(str: String): String {
        val withoutQuotes = str.trim().substring(1 .. str.length-2)
        val result = withoutQuotes.replace("\\n", "\n").replace("\\t", "\t")
        return result
    }

    override fun visitReadStatement(ctx: ReadStatementContext?): Symbol? {
        ctx ?: throw Exception("null ReadStatementContext")
        val id = ctx.Identifier().text
        return if (ctx.rd != null) {
            val sym = cg.currentScope().getOrCreateSymbol(id, type = Type.INT)
            cg.io.readChar(sym)
        } else if (ctx.rdint != null) {
            val sym = cg.currentScope().getOrCreateSymbol(id, type = Type.INT)
            cg.io.readInt(sym)
        } else {
            throw Exception("unsupported read call")
        }
    }

    override fun visitAtomId(ctx: AtomIdContext?): Symbol? {
        val scope = cg.currentScope()
        val symbolName = ctx?.Identifier()?.text ?: throw Exception("null atom identifier")
        // check for undefined identifier
        val symbol = scope.getSymbol(symbolName) ?: throw Exception("undefined identifier: $symbolName")
        return symbol
    }

    override fun visitAtomStr(ctx: AtomStrContext?): Symbol? {
        val scope = cg.currentScope()
        val str = ctx?.StringLiteral()?.text ?: throw Exception("null string literal")
        val chars = unescape(str)
        val tempSymbol = scope.getTempSymbol(Type.STRING, chars.length)
        cg.loadString(tempSymbol, chars)
        return tempSymbol
    }

    override fun visitAtomInt(ctx: AtomIntContext?): Symbol? {
        val scope = cg.currentScope()
        val valueStr = ctx?.IntegerLiteral()?.text ?: throw Exception("null integer literal")
        val value = Integer.parseInt(valueStr)
        if (value >= 256) throw Exception("integer overflow: $value")
        val tempSymbol = scope.getTempSymbol(Type.INT)
        cg.loadInt(tempSymbol, value)
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
        val function = usageInfo[name]?.function ?: throw Exception("unrecognized function: $name")

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
        val function = usageInfo[name]?.function ?: throw Exception("unrecognized function: $name")
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

            cg.enterScope(function.name)
            cg.commentLine("call ${function.name}")

            // create new scope and copy expression args into function param variables
            for (i in 0 until params.size) {
                val param = params[i]
                val sym = cg.currentScope().createSymbol(param.text, arguments[i])
                assignVariable(sym, arguments[i])
            }

        } else {
            cg.enterScope(function.name)
            cg.commentLine("call ${function.name}")
        }

        // execute statements in function body:
        function.ctx.body.statement()?.forEach {
            visit(it)
        }

        var result: Symbol? = null
        if (function.ctx.body.ret != null) {
            val ret = visit(function.ctx.body.ret) ?: throw Exception("null return value")
            cg.exitScope()
            val cpy = cg.currentScope().getTempSymbol(ret.type, ret.size)
            result = cg.move(cpy, ret)
        }
        cg.commentLine("end call ${function.name}")
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
        return evalOpExp(left, op, right)
    }

    private fun evalOpExp(left: Symbol, op: String, right: Symbol): Symbol? {

        // TODO: explicit error for string types:
        if (isConstant(left) && isConstant(right)) {
            return constantFold(left, right, op)
        }

        with (cg) {
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

    private fun constantAssignOp(lhs: Symbol, rhs: Symbol, op: String): Symbol {
        val left = lhs.value as Int
        val right = rhs.value as Int

        var result = when (op) {
            "+=" -> left + right
            "-=" -> left - right
            "*=" -> left * right
            "/=" -> left / right
            "%=" -> left % right
            else -> throw Exception("invalid op $op")
        }

        result = Math.min(result, 255)
        result = Math.max(result, 0)

        lhs.value = result
        return cg.loadInt(lhs, result)
    }

    private fun constantFold(lhs: Symbol, rhs: Symbol, op: String): Symbol {
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

        val symbol = cg.currentScope().getTempSymbol()
        symbol.value = result
        cg.loadInt(symbol, result)
        return symbol
    }

    override fun visitNotExp(ctx: NotExpContext?): Symbol? {
        ctx ?: throw Exception("null NotExpContext")
        val right = visit(ctx.right) ?: throw Exception("null rhs")
        return cg.math.not(right)
    }

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
            doIfElse(condition, ctx)
        } else {
            doIf(condition, ctx)
        }

        return null
    }

    private fun doIf(condition: Symbol, ctx: IfStatementContext) {
        with (cg) {
            val scope = currentScope()
            val cpy = scope.createSymbol("&${condition.name}")
            assign(cpy, condition)

            scope.loopContexts.push(ctx)
            startIf(cpy)
            visit(ctx.trueStatements)
            endIf(cpy)
            scope.loopContexts.pop()

            scope.delete(cpy)
        }
    }

    private fun doIfElse(condition: Symbol, ctx: IfStatementContext) {
        with (cg) {
            val cs = currentScope()
            val cpy = cs.createSymbol("&${condition.name}")
            assign(cpy, condition)

            val elseFlag = currentScope().createSymbol("${cpy.name}_else")
            loadInt(elseFlag, 1)
            onlyIf(cpy, {
                setZero(elseFlag)
            })
            assign(cpy, condition)

            cs.loopContexts.push(ctx)
            startIf(cpy)
            visit(ctx.trueStatements)
            endIf(cpy)

            startElse(elseFlag)
            visit(ctx.falseStatements)
            endElse(elseFlag)
            cs.loopContexts.pop()

            cs.delete(cpy)
            cs.delete(elseFlag)
        }
    }

    override fun visitWhileStatement(ctx: WhileStatementContext?): Symbol? {
        ctx ?: throw Exception("null WhileStatementContext")
        var condition = visit(ctx.condition) ?: throw Exception("null condition result")

        if (isConstant(condition) && (condition.value as Int) == 0) {
            return null
        }

        val cpy = cg.currentScope().createSymbol("&${ctx.sourceInterval.a}")
        if (condition.isTemp()) {
            cg.move(cpy, condition)
        } else {
            cg.assign(cpy, condition)
        }

        cg.startWhile(cpy)
        cg.currentScope().loopContexts.push(ctx)
        for (stmt in ctx.body.statement()) {
            visit(stmt)
        }

        condition = visit(ctx.condition) ?: throw Exception("null condition result")
        if (condition.isTemp()) {
            cg.move(cpy, condition)
        } else {
            cg.assign(cpy, condition)
        }
        cg.currentScope().loopContexts.pop()
        cg.endWhile(cpy)

        cg.currentScope().delete(cpy)
        return null
    }

    override fun visitForStatement(ctx: ForStatementContext?): Symbol? {
        ctx ?: throw Exception("null ForStatementContext")

        val start = visit(ctx.start)!!
        val stop = visit(ctx.stop)!!
        val step = if (ctx.step != null) {
            visit(ctx.step)!!
        } else {
            cg.loadInt(cg.currentScope().createSymbol("step${ctx.sourceInterval.a}", value=1), 1)
        }

        val condition = cg.currentScope().createSymbol("&${ctx.sourceInterval.a}")

        // hack to prevent temp symbols from being deleted:
        val deleteSet = HashSet<Symbol>()
        listOf(start, stop, step).forEach {
            val sym = cg.currentScope().getSymbol(it.name)!!
            if (sym.isTemp()) {
                cg.currentScope().rename(sym, "&${sym.name}")
                deleteSet.add(it)
            }
        }

        val loopVar = cg.currentScope().createSymbol(ctx.loopVar.text)

        // try to unroll loop
        if (canUnrollLoop(start, stop, step, ctx.body.statement())) {
            cg.commentLine("unrolling loop")
            cg.loadInt(loopVar, start.value as Int)
            for (i in (start.value as Int) .. (stop.value as Int) step (step.value as Int)) {
                for (stmt in ctx.body.statement()) {
                    visit(stmt)
                }
                cg.incrementBy(loopVar, step.value as Int)
            }
        } else {
            cg.startFor(loopVar, start, stop, condition)
            cg.currentScope().loopContexts.push(ctx)
            for (stmt in ctx.body.statement()) {
                visit(stmt)
            }
            cg.endFor(loopVar, stop, step, condition)
            cg.currentScope().loopContexts.pop()
        }
        // can't delete these if they refer to an existing symbol
        deleteSet.forEach { cg.currentScope()::delete }
        cg.currentScope().delete(condition)
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

    private fun createArray(name: String, size: Int, values: List<TerminalNode>? = null): Symbol {
        if (size < 1 || size >= 256) throw Exception("array size must be between 1 and 256")
        val array = cg.currentScope().createSymbol(name, size + 4, Type.INT)
        if (values != null) {
            values.map { Integer.parseInt(it.text) }.
                    forEachIndexed { i, v -> cg.loadInt(array.offset(i), value = v) }
        } else {
            cg.setZero(array)
        }
        return array
    }

    override fun visitArrayWriteStatement(ctx: ArrayWriteStatementContext?): Symbol? {
        ctx ?: throw Exception("null ArrayWriteStatementContext")
        val array = checkNotNull(cg.currentScope().getSymbol(ctx.array.text), { "null ${ctx.array.text}" })
        val index = checkNotNull(visit(ctx.idx), { "null ${ctx.idx.text}" })
        val value = checkNotNull(visit(ctx.rhs), { "null ${ctx.rhs.text}" })

        // TODO: type checking?

        if (isConstant(index)) {
            if (isConstant(value)) {
                if (value.value is String) {
                    val char = (value.value as String)[0]
                    return cg.loadInt(array.offset(index.value as Int), char.toInt())
                } else {
                    return cg.loadInt(array.offset(index.value as Int), value.value as Int)
                }
            } else {
                return cg.assign(array.offset(index.value as Int), value)
            }
        } else {
            cg.writeArray(array, index, value)
            return null
        }
    }

    override fun visitArrayReadExp(ctx: ArrayReadExpContext?): Symbol? {
        ctx ?: throw Exception("null ArrayReadExpContext")
        val array = checkNotNull(cg.currentScope().getSymbol(ctx.array.text), { "null ${ctx.array.text}" })
        val index = checkNotNull(visit(ctx.idx), { "null ${ctx.idx.text}" })

        if (isConstant(index)) {
            val ret = cg.currentScope().getTempSymbol()
            return cg.assign(ret, array.offset(index.value as Int))
        } else {
            return cg.readArray(array, index)
        }
    }

    override fun visitDebugStatement(ctx: DebugStatementContext?): Symbol? {
        ctx ?: throw Exception("null DebugStatementContext")
        ctx.idList.Identifier().map {
            cg.currentScope().getSymbol( it.text )
        }
        .filterNotNull()
        .forEach {
            if (it.size == 1) {
                cg.debug(it, "$it = ")
            } else {
                (0 until it.size)
                        .map { i -> it.offset(i) }
                        .forEach { cg.debug(it, "$it = ") }
            }
        }
        return null
    }

    private fun scheduleDeletion(symbolNames: Iterable<String>) {
        staleSymbols.addAll(symbolNames)
        if (cg.currentScope().loopContexts.isEmpty()) {
            staleSymbols.forEach { name ->
                cg.currentScope().getSymbol(name)?.let {
                    cg.currentScope().delete(it)
                }
            }
        }
    }

    fun isConstant(symbol: Symbol): Boolean {
        if (!cg.options.optimize) return false
        if (symbol.value == null) return false
        if (cg.currentScope().loopContexts.isNotEmpty()) {
            cg.currentScope().loopContexts.forEach { ctx ->
                val modifiedSymbols = usageInfo[currentFunction()]!!.loopSymbolsWritten[ctx]
                if (modifiedSymbols != null && symbol.name in modifiedSymbols) return false
            }
        }
        return true
    }

    private inline fun currentFunction() = cg.currentScope().functionName

}
