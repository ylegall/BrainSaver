package org.ygl.ast

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.RuleNode
import org.ygl.BrainSaverBaseVisitor
import org.ygl.BrainSaverParser.*
import org.ygl.model.StorageType

/**
 *
 */
class AstBuilder : BrainSaverBaseVisitor<AstNode>()
{
    override fun visitProgram(ctx: ProgramContext?): AstNode {
        return AstNode(children = visit(ctx!!.declList()).children)
    }

    override fun visitConstant(ctx: ConstantContext?): AstNode {
        return ConstantNode(ctx!!.Identifier().text, visit(ctx.rhs))
    }

    override fun visitGlobalVariable(ctx: GlobalVariableContext?): AstNode {
        val storage = StorageType.parse(ctx!!.storage().text)
        return GlobalVariableNode(storage, ctx.Identifier().text, visit(ctx.rhs))
    }

    override fun visitFunction(ctx: FunctionContext?): AstNode {
        val params = ctx!!.params?.identifierList()?.Identifier()?.mapNotNull { it.text } ?: listOf()
        val stmts = ctx.body.statement().map { visit(it) }.toCollection(mutableListOf())
        if (ctx.body.ret != null) stmts.add(visit(ctx.body.ret))
        return FunctionNode(ctx.name.text, params, stmts)
    }

    override fun visitDebugStatement(ctx: DebugStatementContext?): AstNode {
        val params = ctx!!.identifierList()?.Identifier()?.mapNotNull { it.text } ?: listOf()
        return DebugStatementNode(params)
    }

    override fun visitIfStatement(ctx: IfStatementContext?): AstNode {
        val condition = visit(ctx!!.condition) as ExpNode
        val trueStatements = mutableListOf<AstNode>()
        val falseStatements = mutableListOf<AstNode>()
        ctx.trueStatements?.statement()?.forEach { trueStatements.add(visit(it)) }
        ctx.falseStatements?.statement()?.forEach { falseStatements.add(visit(it)) }
        return IfStatementNode(condition, trueStatements, falseStatements)
    }

    override fun visitWhileStatement(ctx: WhileStatementContext?): AstNode {
        val condition = visit(ctx!!.condition) as ExpNode
        val stmts = toNodeList<AstNode>(ctx.statementList().statement())
        return WhileStatementNode(condition, stmts)
    }

    override fun visitForStatement(ctx: ForStatementContext?): AstNode {
        val start = visit(ctx!!.start) as AtomNode
        val stop = visit(ctx.stop) as AtomNode
        val inc = if (ctx.step != null) visit(ctx.step) as AtomNode else null
        val stmts = toNodeList<AstNode>(ctx.statementList().statement())
        return ForStatementNode(ctx.loopVar.text, start, stop, inc, stmts)
    }

    override fun visitDeclarationStatement(ctx: DeclarationStatementContext?): AstNode {
        val storage = StorageType.parse(ctx!!.storage().text)
        return DeclarationNode(storage, ctx.lhs.text, visit(ctx.rhs))
    }

    override fun visitAssignmentStatement(ctx: AssignmentStatementContext?): AstNode {
        val op = ctx!!.op.text
        val lhs = ctx.lhs.text

        return if (op == "=") {
            AssignmentNode(lhs, visit(ctx.rhs))
        } else {
            val left = AtomIdNode(lhs)
            val right = visit(ctx.rhs)
            return AssignmentNode(lhs, BinaryExpNode(op.substring(0, 1), left, right))
        }
    }

    override fun visitCallStatement(ctx: CallStatementContext?): AstNode {
        val name = ctx!!.funcName.text
        val args = toNodeList<AstNode>(ctx.args?.exp() ?: mutableListOf())
        return CallStatementNode(name, args)
    }

    override fun visitReturnStatement(ctx: ReturnStatementContext?): AstNode {
        return ReturnNode(visit(ctx!!.exp()))
    }

    override fun visitReadStatement(ctx: ReadStatementContext?): AstNode {
        return ReadStatementNode(ctx!!.Identifier().text)
    }

    override fun visitPrintStatement(ctx: PrintStatementContext?): AstNode {
        return PrintStatementNode(visit(ctx!!.exp()))
    }

    override fun visitArrayConstructor(ctx: ArrayConstructorContext?): AstNode {
        return ArrayConstructorNode(ctx!!.lhs.text, ctx.arraySize.text.toInt())
    }

    override fun visitArrayLiteral(ctx: ArrayLiteralContext?): AstNode {
        val items = mutableListOf<Int>()
        ctx!!.integerList().IntegerLiteral().forEach { items.add(it.text.toInt()) }
        return ArrayLiteralNode(ctx.lhs.text, items)
    }

    override fun visitArrayWriteStatement(ctx: ArrayWriteStatementContext?): AstNode {
        return ArrayWriteNode(ctx!!.array.text, visit(ctx.idx), visit(ctx.rhs))
    }

    override fun visitParenExp(ctx: ParenExpContext?): AstNode {
        return visit(ctx!!.exp())
    }

    override fun visitArrayReadExp(ctx: ArrayReadExpContext?): ExpNode {
        val idx = visit(ctx!!.exp())
        return ArrayReadExpNode(ctx.array.text, idx)
    }

    override fun visitCallExp(ctx: CallExpContext?): AstNode {
        val params = toNodeList<AstNode>(ctx!!.expList().exp())
        return CallExpNode(ctx.funcName.text, params)
    }

    override fun visitOpExp(ctx: OpExpContext?): ExpNode {
        val op = ctx!!.op.text
        return BinaryExpNode(op, visit(ctx.left), visit(ctx.right))
    }

    override fun visitNotExp(ctx: NotExpContext?): ExpNode {
        return NotExpNode(visit(ctx!!.right) as ExpNode)
    }

    override fun visitAtomExp(ctx: AtomExpContext?): AstNode {
        return visit(ctx!!.atom())
    }

    override fun visitAtomId(ctx: AtomIdContext?): AstNode {
        return AtomIdNode(ctx!!.Identifier().text)
    }

    override fun visitAtomStr(ctx: AtomStrContext?): AstNode {
        return AtomStrNode(ctx!!.StringLiteral().text)
    }

    override fun visitAtomInt(ctx: AtomIntContext?): AstNode {
        return AtomIntNode(ctx!!.IntegerLiteral().text.toInt())
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T: AstNode> toNodeList(input: List<ParserRuleContext>): MutableList<T> {
        return MutableList(input.size, { idx -> visit(input[idx]) as T })
    }

    override fun visitChildren(rule: RuleNode): AstNode {
        val ctx = rule as ParserRuleContext
        val children = mutableListOf<AstNode>()
        for (i in 0 until rule.childCount) {
            val c = rule.getChild(i)
            val childResult = c.accept<AstNode>(this)
            if (childResult != null) children.add(childResult)
        }
        return AstNode(children = children)
    }
}