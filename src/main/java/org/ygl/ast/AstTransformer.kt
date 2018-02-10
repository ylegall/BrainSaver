package org.ygl.ast

/**
 *
 */
open class AstTransformer: AstWalker<AstNode>() {

    override fun visit(node: ArrayLiteralNode): AstNode {
        val items = MutableList(node.children.size, { idx -> visit(node.children[idx]) })
        return ArrayLiteralNode(node.array, node.storage, items)
    }

    override fun visit(node: ArrayReadExpNode): AstNode {
        return ArrayReadExpNode(node.array, visit(node.idx))
    }

    override fun visit(node: ArrayWriteNode): AstNode {
        return ArrayWriteNode(node.array, visit(node.idx), visit(node.rhs))
    }

    override fun visit(node: AssignmentNode): AstNode {
        return AssignmentNode(node.lhs, visit(node.rhs), node.sourceInfo)
    }

    override fun visit(node: BinaryExpNode): AstNode {
        return BinaryExpNode(node.op, visit(node.left), visit(node.right))
    }

    override fun visit(node: CallExpNode): AstNode {
        val params = MutableList(node.children.size, { idx -> visit(node.children[idx]) })
        return CallExpNode(node.name, params, node.sourceInfo)
    }

    override fun visit(node: CallStatementNode): AstNode {
        val params = MutableList(node.children.size, { idx -> visit(node.children[idx]) })
        return CallStatementNode(node.name, params, node.sourceInfo)
    }

    override fun visit(node: ConditionExpNode): AstNode {
        return ConditionExpNode(visit(node.condition), visit(node.trueExp), visit(node.falseExp))
    }

    override fun visit(node: ConstantNode): AstNode {
        return ConstantNode(node.lhs, visit(node.rhs), node.sourceInfo)
    }

    override fun visit(node: DeclarationNode): AstNode {
        return DeclarationNode(node.storage, node.lhs, visit(node.rhs), node.sourceInfo)
    }

    override fun visit(node: ForStatementNode): AstNode {
        return ForStatementNode(node.counter,
                visit(node.start),
                visit(node.stop),
                visit(node.inc),
                visitList(node.statements)
        )
    }

    override fun visit(node: FunctionNode): AstNode {
        return FunctionNode(node.name,
                node.params,
                visitList(node.statements),
                node.ret?.let { ReturnNode(visit(it)) }
        )
    }

    override fun visit(node: GlobalVariableNode): AstNode {
        return GlobalVariableNode(node.lhs, visit(node.rhs), node.sourceInfo)
    }

    override fun visit(node: IfStatementNode): AstNode {
        return IfStatementNode(visit(node.condition),
                visitList(node.trueStatements),
                visitList(node.falseStatements)
        )
    }

    override fun visit(node: StatementNode): AstNode {
        return StatementNode(visitList(node.children), node.sourceInfo)
    }

    override fun visit(node: NotExpNode): AstNode {
        return NotExpNode(visit(node.right))
    }

    override fun visit(node: ProgramNode): AstNode {
        return ProgramNode(visitList(node.children))
    }

    override fun visit(node: WhileStatementNode): AstNode {
        return WhileStatementNode(
                visit(node.condition),
                visitList(node.statements)
        )
    }

    override fun visitList(children: MutableList<AstNode>): MutableList<AstNode> {
        return children.map { visit(it) }
                .filter{ it != EmptyNode }
                .toCollection(mutableListOf())
    }

    override fun defaultValue(node: AstNode) = node
}