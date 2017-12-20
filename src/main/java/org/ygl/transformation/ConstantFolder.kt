package org.ygl.transformation

import org.ygl.ast.*
import java.util.*

/**
 *
 */
class ConstantFolder(

): AstWalker<AstNode>()
{
    private val scopeSymbols = ArrayDeque<MutableMap<String, AstNode>>()

    private fun AstNode.isConstant(): Boolean {
        return (this is AtomIntNode) || (this is AtomStrNode)
    }

    override fun visit(node: AssignmentNode): AstNode {
        val rhs = visit(node.rhs)
        return if (rhs.isConstant()) {
            scopeSymbols.peek().put(node.lhs, rhs)
            AssignmentNode(node.lhs, rhs)
        } else {
            node
        }
    }

    override fun visit(node: AtomIdNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: AtomStrNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: BinaryExpNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: CallExpNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: CallStatementNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: ConstantNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: DebugStatementNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: DeclarationNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: ForStatementNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: FunctionNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: GlobalVariableNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: IfStatementNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: PrintStatementNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: ReadStatementNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: StatementNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: NotExpNode): AstNode {
        return super.visit(node)
    }

    override fun visit(node: WhileStatementNode): AstNode {
        return super.visit(node)
    }

    override fun visitChildren(node: AstNode): AstNode {
        super.visitChildren(node)
        return node
    }

    override fun aggregateResult(agg: AstNode, next: AstNode): AstNode {
        return super.aggregateResult(agg, next)
    }

    override fun defaultValue(): AstNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}