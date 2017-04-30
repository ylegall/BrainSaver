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
        //println("enter function: ${ctx?.name?.text}")
        val result = visitChildren(ctx)
        //println("exit function: ${ctx?.name?.text}")
        return result
    }

    override fun visitAssignmentStatement(ctx: AssignmentStatementContext?): Symbol? {
        if (ctx == null) throw Exception("null AssignmentStatementContext")
        val lhs = ctx.lhs.text
        val op = ctx.op.text
        val rhs = ctx.exp()

        val expResult = this.visit(rhs) ?: throw Exception("null rhs expression result")

        var lhsSymbol = codegen.currentScope().getSymbol(lhs)
        if (lhsSymbol == null) {
            lhsSymbol = codegen.currentScope().createSymbol(lhs, expResult.size, expResult.type)
        } else {
            if (lhsSymbol.type != expResult.type) throw Exception("type mismatch")
        }

        // TODO: implement operators
        when (op) {
            "="  -> assign(lhsSymbol, expResult)
        //"+=" -> print("x == 2")
        //"-=" -> print("x == 2")
        //"*=" -> print("x == 2")
        //"/=" -> print("x == 2")
        //"%=" -> print("x == 2")
            else -> {
                throw Exception("unknown assignment operator: " + op)
            }
        }
        return lhsSymbol
    }

    private fun assign(lhs: Symbol, rhs: Symbol): Symbol {
        if (rhs.isConstant()) {
            when (rhs.type) {
                Type.STRING -> codegen.loadString(lhs, rhs.value as String)
                Type.INT    -> codegen.set(lhs, rhs.value as Int)
            }
        } else {
            codegen.assign(lhs, rhs)
        }
        return lhs
    }

    override fun visitPrintStatement(ctx: PrintStatementContext?): Symbol? {
        if (ctx == null) throw Exception("null PrintStatementContext")
        val scope = codegen.currentScope()
        var sym = if (ctx.exp() is AtomExpContext) {
            scope.getSymbol(ctx.exp().text)
        } else {
            visit(ctx.exp())
        } ?: throw Exception("null argument to print()")

        return codegen.print(sym)
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
        val str = ctx?.StringLiteral()?.text
        val chars = str?.substring(1 .. str.length-2) ?: throw Exception("null string literal")
        val tempSymbol = scope.getTempSymbol(Type.STRING, chars.length)
        //codegen.loadString(tempSymbol, chars)
        tempSymbol.value = chars
        return tempSymbol
    }

    override fun visitAtomInt(ctx: AtomIntContext?): Symbol? {
        val scope = codegen.currentScope()
        val valueStr = ctx?.IntegerLiteral()?.text ?: throw Exception("null integer literal")
        val value = Integer.parseInt(valueStr) % 256
        val tempSymbol = scope.getTempSymbol(Type.INT)
        //codegen.set(tempSymbol, value)
        tempSymbol.value = value
        return tempSymbol
    }

    override fun visitParenExp(ctx: ParenExpContext?): Symbol? {
        return visit(ctx?.exp())
    }

    // TODO
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

}