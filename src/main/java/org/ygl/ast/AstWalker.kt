package org.ygl.ast

abstract class AstWalker<T>
{
    fun visit(node: AstNode): T {
        return when(node) {
            is StatementNode -> when(node) {
                is ArrayConstructorNode -> visit(node)
                is ArrayLiteralNode -> visit(node)
                is ArrayWriteNode -> visit(node)
                is AssignmentNode -> visit(node)
                is CallStatementNode -> visit(node)
                is DebugStatementNode -> visit(node)
                is DeclarationNode -> visit(node)
                is ForStatementNode -> visit(node)
                is IfStatementNode -> visit(node)
                is PrintStatementNode -> visit(node)
                is ReadStatementNode -> visit(node)
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
                is NotExpNode -> visit(node)
                else -> visitChildren(node)
            }
            is ConstantNode -> visit(node)
            is GlobalVariableNode -> visit(node)
            is FunctionNode -> visit(node)
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
    open fun visit(node: ConstantNode): T = visitChildren(node)
    open fun visit(node: DebugStatementNode): T = visitChildren(node)
    open fun visit(node: DeclarationNode): T = visitChildren(node)
    open fun visit(node: ForStatementNode): T = visitChildren(node)
    open fun visit(node: FunctionNode): T = visitChildren(node)
    open fun visit(node: GlobalVariableNode): T = visitChildren(node)
    open fun visit(node: IfStatementNode): T = visitChildren(node)
    open fun visit(node: PrintStatementNode): T = visitChildren(node)
    open fun visit(node: ReadStatementNode): T = visitChildren(node)
    open fun visit(node: StatementNode): T = visitChildren(node)
    open fun visit(node: NotExpNode): T = visitChildren(node)
    open fun visit(node: WhileStatementNode): T = visitChildren(node)

    open fun visitChildren(node: AstNode): T {
        return if (node.children.isEmpty()) {
            defaultValue()
        } else {
            var result = visit(node.children[0])
            for (i in 1 until node.children.size) {
                val nextValue = visit(node.children[i])
                result = aggregateResult(result, nextValue)
            }
            result
        }
    }

    open fun aggregateResult(agg: T, next: T): T = agg

    abstract fun defaultValue(): T

    fun visit(nodes: Iterable<AstNode>): T {
        var result = defaultValue()
        for (node in nodes) {
            val nextValue = visit(node)
            result = aggregateResult(result, nextValue)
        }
        return result
    }
}
