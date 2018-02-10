package org.ygl.ast

abstract class AstWalker<T>
{
    fun visit(node: AstNode): T {
        return when (node.nodeType) {
            NodeType.ARRAY_CTOR -> visit(node as ArrayConstructorNode)
            NodeType.ARRAY_LITERAL -> visit(node as ArrayLiteralNode)
            NodeType.ARRAY_READ_EXP -> visit(node as ArrayReadExpNode)
            NodeType.ARRAY_WRITE -> visit(node as ArrayWriteNode)
            NodeType.ASSIGN -> visit(node as AssignmentNode)
            NodeType.ATOM_ID -> visit(node as AtomIdNode)
            NodeType.ATOM_INT -> visit(node as AtomIntNode)
            NodeType.ATOM_STR -> visit(node as AtomStrNode)
            NodeType.BINARY_EXP -> visit(node as BinaryExpNode)
            NodeType.CALL -> visit(node as CallStatementNode)
            NodeType.CALL_EXP -> visit(node as CallExpNode)
            NodeType.CONSTANT -> visit(node as ConstantNode)
            NodeType.DECLARE -> visit(node as DeclarationNode)
            NodeType.FOR -> visit(node as ForStatementNode)
            NodeType.FUNCTION -> visit(node as FunctionNode)
            NodeType.GLOBAL -> visit(node as GlobalVariableNode)
            NodeType.IF -> visit(node as IfStatementNode)
            NodeType.IF_EXP -> visit(node as ConditionExpNode)
            NodeType.NONE -> defaultValue(node)
            NodeType.PROGRAM -> visit(node as ProgramNode)
            NodeType.RETURN -> visit(node as ReturnNode)
            NodeType.STATEMENT -> visit(node as StatementNode)
            NodeType.UNARY_EXP -> visit(node as NotExpNode)
            NodeType.WHILE -> visit(node as WhileStatementNode)
        }
    }

    open fun visit(node: ArrayConstructorNode): T = visitChildren(node)
    open fun visit(node: ArrayLiteralNode): T = visitChildren(node)
    open fun visit(node: ArrayReadExpNode): T = visitChildren(node)
    open fun visit(node: ArrayWriteNode): T = visitChildren(node)
    open fun visit(node: AssignmentNode): T = visitChildren(node)
    open fun visit(node: AtomIdNode): T = visitChildren(node)
    open fun visit(node: AtomIntNode): T = visitChildren(node)
    open fun visit(node: AtomStrNode): T = visitChildren(node)
    open fun visit(node: BinaryExpNode): T = visitChildren(node)
    open fun visit(node: CallExpNode): T = visitChildren(node)
    open fun visit(node: CallStatementNode): T = visitChildren(node)
    open fun visit(node: ConditionExpNode): T = visitChildren(node)
    open fun visit(node: ConstantNode): T = visitChildren(node)
    open fun visit(node: DeclarationNode): T = visitChildren(node)
    open fun visit(node: ForStatementNode): T = visitChildren(node)
    open fun visit(node: FunctionNode): T = visitChildren(node)
    open fun visit(node: GlobalVariableNode): T = visitChildren(node)
    open fun visit(node: IfStatementNode): T = visitChildren(node)
    open fun visit(node: ProgramNode): T = visitChildren(node)
    open fun visit(node: StatementNode): T = visitChildren(node)
    open fun visit(node: NotExpNode): T = visitChildren(node)
    open fun visit(node: ReturnNode): T = visitChildren(node)
    open fun visit(node: WhileStatementNode): T = visitChildren(node)

    open fun visitChildren(node: AstNode): T {
        return if (node.children.isEmpty()) {
            defaultValue(node)
        } else {
            return node.children
                    .map { visit(it) }
                    .reduce { acc, next -> aggregateResult(acc, next) }
        }
    }

    open fun aggregateResult(agg: T, next: T): T = agg

    abstract fun defaultValue(node: AstNode): T

    fun visit(nodes: Iterable<AstNode>): T {
        return nodes.map { visit(it) }
                .reduce { acc, next -> aggregateResult(acc, next) }
    }

    open fun visitList(children: MutableList<AstNode>): MutableList<T> {
        return MutableList(children.size, { idx -> visit(children[idx]) })
    }
}
