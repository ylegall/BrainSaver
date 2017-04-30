package org.ygl

/**
 * Contains listeners that are invoked as the parse tree is traversed.
 * http://stackoverflow.com/questions/23092081/antlr4-visitor-pattern-on-simple-arithmetic-example
 */
class TreeWalkerImpl(val codegen: CodeGen) : BrainLoveBaseListener()
{

    override fun enterProgram(ctx: BrainLoveParser.ProgramContext?) {
        println("program begin")
    }

    override fun exitProgram(ctx: BrainLoveParser.ProgramContext?) {
        println("program end")
    }

    override fun enterFunction(ctx: BrainLoveParser.FunctionContext?) {
        println("enter function " + ctx?.name?.text)
        codegen.enterScope()
    }

    override fun exitFunction(ctx: BrainLoveParser.FunctionContext?) {
        println("exit function " + ctx?.name?.text)
        codegen.exitScope()
    }

//    override fun enterAssignmentStatement(ctx: BrainLoveParser.AssignmentStatementContext?) {
//        if (ctx == null) throw Exception("null AssignmentStatementContext")
//        val lhs = ctx.lhs.text
//        val op = ctx.op.text
//        val rhs = ctx.exp()
//
//        val expResult = evaluteExpression(rhs)
//
//        when (op) {
//            "="  -> codegen.assign(lhs, expResult)
//            //"+=" -> print("x == 2")
//            //"-=" -> print("x == 2")
//            //"*=" -> print("x == 2")
//            //"/=" -> print("x == 2")
//            //"%=" -> print("x == 2")
//            else -> {
//                throw Exception("unknown assignment operator: " + op)
//            }
//        }
//    }


    // TODO
    fun evaluteExpression(ctx: BrainLoveParser.ExpContext): Symbol {
        return Symbol("1", 1, 1, Type.INT, null)
    }

    override fun enterAtomExp(ctx: BrainLoveParser.AtomExpContext?) {
        super.enterAtomExp(ctx)
    }

    override fun enterParenExp(ctx: BrainLoveParser.ParenExpContext?) {
    }

    override fun enterOpExp(ctx: BrainLoveParser.OpExpContext?) {
    }
}