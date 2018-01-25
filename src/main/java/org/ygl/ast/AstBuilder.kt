package org.ygl.ast

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.RuleNode
import org.ygl.BrainSaverBaseVisitor
import org.ygl.BrainSaverParser.*
import org.ygl.model.Op
import org.ygl.model.StorageType

/**
 *
 */
class AstBuilder: BrainSaverBaseVisitor<AstNode>()
{
    override fun visitProgram(ctx: ProgramContext?): AstNode {
        val children = visit(ctx!!.declList()).children
        return ProgramNode(children)
    }

    override fun visitConstant(ctx: ConstantContext?): AstNode {
        return ConstantNode(ctx!!.Identifier().text, visit(ctx.rhs), SourceInfo(ctx))
    }

    override fun visitGlobalVariable(ctx: GlobalVariableContext?): AstNode {
        return GlobalVariableNode(ctx!!.Identifier().text, visit(ctx.rhs), SourceInfo(ctx))
    }

    override fun visitFunction(ctx: FunctionContext?): AstNode {
        val name = ctx!!.name.text
        val params = ctx.params?.mapNotNull { it.text } ?: listOf()
        val stmts = toNodeList(ctx.body.statement())
        var ret: AstNode? = null
        if (ctx.body.ret != null) {
            ret = visit(ctx.body.ret)
        }
        return FunctionNode(name, params, stmts, ret)
    }

    override fun visitStatement(ctx: StatementContext?): AstNode {
        return StatementNode(children = mutableListOf(super.visitChildren(ctx!!)))
    }

    override fun visitIfStatement(ctx: IfStatementContext?): AstNode {
        val condition = visit(ctx!!.condition) as ExpNode
        val trueStatements = toNodeList(ctx.trueStmts)
        val falseStatements = toNodeList(ctx.falseStmts)
        return IfStatementNode(condition, trueStatements, falseStatements)
    }

    override fun visitWhileStatement(ctx: WhileStatementContext?): AstNode {
        val condition = visit(ctx!!.condition) as ExpNode
        val stmts = toNodeList(ctx.body)
        return WhileStatementNode(condition, stmts)
    }

    override fun visitForStatement(ctx: ForStatementContext?): AstNode {
        val start = visit(ctx!!.start) as AtomNode
        val stop = visit(ctx.stop) as AtomNode
        val inc = if (ctx.step != null) visit(ctx.step) as AtomNode else AtomIntNode(1)
        val statements = toNodeList(ctx.body)
        return ForStatementNode(ctx.loopVar.text, start, stop, inc, statements)
    }

    override fun visitDeclarationStatement(ctx: DeclarationStatementContext?): AstNode {
        val storage = StorageType.parse(ctx!!.storage().text)
        val name = ctx.lhs.text
        return DeclarationNode(storage, name, visit(ctx.rhs), SourceInfo(ctx))
    }

    override fun visitAssignmentStatement(ctx: AssignmentStatementContext?): AstNode {
        val op = ctx!!.op.text
        val lhs = ctx.lhs.text

        return if (op == "=") {
            AssignmentNode(lhs, visit(ctx.rhs), SourceInfo(ctx))
        } else {
            val left = AtomIdNode(lhs)
            val right = visit(ctx.rhs)
            return AssignmentNode(lhs, BinaryExpNode(Op.parse(op.substring(0, 1)), left, right), SourceInfo(ctx))
        }
    }

    override fun visitCallStatement(ctx: CallStatementContext?): AstNode {
        val name = ctx!!.funcName.text
        val args = toNodeList(ctx.args?.exp() ?: mutableListOf())
        return CallStatementNode(name, args)
    }

    override fun visitReturnStatement(ctx: ReturnStatementContext?): AstNode {
        return ReturnNode(visit(ctx!!.exp()))
    }

    override fun visitArrayConstructor(ctx: ArrayConstructorContext?): AstNode {
        val storage = StorageType.parse(ctx!!.storage().text)
        return ArrayConstructorNode(ctx.lhs.text, storage, ctx.arraySize.text.toInt())
    }

    override fun visitArrayLiteral(ctx: ArrayLiteralContext?): AstNode {
        val storage = StorageType.parse(ctx!!.storage().text)
        val items = MutableList(ctx.exp().size, { idx -> visit(ctx.exp(idx)) })
        return ArrayLiteralNode(ctx.lhs.text, storage, items)
    }

    override fun visitArrayWriteStatement(ctx: ArrayWriteStatementContext?): AstNode {
        return ArrayWriteNode(ctx!!.array.text, visit(ctx.idx), visit(ctx.rhs))
    }

    override fun visitParenExp(ctx: ParenExpContext?): AstNode {
        return visit(ctx!!.exp())
    }

    override fun visitConditionalExp(ctx: ConditionalExpContext?): AstNode {
        val condition = visit(ctx!!.condition)
        val trueExp = visit(ctx.trueExp)
        val falseExp = visit(ctx.falseExp)
        return ConditionExpNode(condition, trueExp, falseExp)
    }

    override fun visitArrayReadExp(ctx: ArrayReadExpContext?): ExpNode {
        val idx = visit(ctx!!.exp())
        return ArrayReadExpNode(ctx.array.text, idx)
    }

    override fun visitCallExp(ctx: CallExpContext?): AstNode {
        val params = toNodeList(ctx!!.expList()?.exp() ?: emptyList())
        return CallExpNode(ctx.funcName.text, params)
    }

    override fun visitOpExp(ctx: OpExpContext?): ExpNode {
        val op = ctx!!.op.text
        return BinaryExpNode(Op.parse(op), visit(ctx.left), visit(ctx.right))
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

    private fun toNodeList(input: List<ParserRuleContext>): MutableList<AstNode> {
        return MutableList(input.size, { idx -> visit(input[idx]) })
    }

    override fun visitChildren(rule: RuleNode): AstNode {
        val children = mutableListOf<AstNode>()
        for (i in 0 until rule.childCount) {
            val c = rule.getChild(i)
            val childResult = c.accept<AstNode>(this)
            if (childResult != null) children.add(childResult)
        }
        return AstNode(children = children)
    }
}