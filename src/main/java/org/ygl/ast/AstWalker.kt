package org.ygl.ast

abstract class AstWalker<T>
{
    fun visit(node: AstNode): T {
        return when(node) {
            is StatementNode -> when(node) {
                is ArrayConstructorNode -> visit(node)
                is ArrayLiteralNode -> visit(node)
                is ArrayWriteNode -> visit(node)
                is StoreNode -> when(node) {
                    is AssignmentNode -> visit(node)
                    is DeclarationNode -> visit(node)
                    else -> visit(node)
                }
                is CallStatementNode -> visit(node)
                //is DebugStatementNode -> visit(node)
                is ForStatementNode -> visit(node)
                is IfStatementNode -> visit(node)
                is WhileStatementNode -> visit(node)
                else -> visit(node)
            }
            is AtomNode -> when(node) {
                is AtomIdNode -> visit(node)
                is AtomIntNode -> visit(node)
                is AtomStrNode -> visit(node)
                else -> visitChildren(node)
            }
            is ExpNode -> when(node) {
                is ArrayReadExpNode -> visit(node)
                is BinaryExpNode -> visit(node)
                is CallExpNode -> visit(node)
                is ConditionExpNode -> visit(node)
                is NotExpNode -> visit(node)
                else -> visitChildren(node)
            }
            is ConstantNode -> visit(node)
            is GlobalVariableNode -> visit(node)
            is FunctionNode -> visit(node)
            is ProgramNode -> visit(node)
            else -> visitChildren(node)
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
    //open fun visit(node: DebugStatementNode): T = visitChildren(node)
    open fun visit(node: DeclarationNode): T = visitChildren(node)
    open fun visit(node: ForStatementNode): T = visitChildren(node)
    open fun visit(node: FunctionNode): T = visitChildren(node)
    open fun visit(node: GlobalVariableNode): T = visitChildren(node)
    open fun visit(node: IfStatementNode): T = visitChildren(node)
    open fun visit(node: ProgramNode): T = visitChildren(node)
    open fun visit(node: StatementNode): T = visitChildren(node)
    open fun visit(node: NotExpNode): T = visitChildren(node)
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
