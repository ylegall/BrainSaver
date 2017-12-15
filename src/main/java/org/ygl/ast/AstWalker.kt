package org.ygl.ast

abstract class AstWalker<T>
{
    fun visit(node: AstNode): T {
        return when (node) {
            is ArrayConstructorNode -> visit(node)
            is ArrayLiteralNode -> visit(node)
            is ArrayReadExpNode -> visit(node)
            is ArrayWriteNode -> visit(node)
            is AssignmentNode -> visit(node)
            is AtomIdNode -> visit(node)
            is AtomIntNode -> visit(node)
            is AtomStringNode -> visit(node)
            is BinaryExpNode -> visit(node)
            is CallExpNode -> visit(node)
            is CallStatementNode -> visit(node)
            is DebugStatementNode -> visit(node)
            is ForStatementNode -> visit(node)
            is FunctionNode -> visit(node)
            is IfStatementNode -> visit(node)
            is PrintStatementNode -> visit(node)
            is ReadStatementNode -> visit(node)
            is NotExpNode -> visit(node)
            is WhileStatementNode -> visit(node)
            else -> visitChildren(node)
        }
    }

     open fun visit(node: ArrayConstructorNode):  T = visitChildren(node)
     open fun visit(node: ArrayLiteralNode):  T = visitChildren(node)
     open fun visit(node: ArrayReadExpNode):  T = visitChildren(node)
     open fun visit(node: ArrayWriteNode):  T = visitChildren(node)
     open fun visit(node: AssignmentNode):  T = visitChildren(node)
     open fun visit(node: AtomIdNode):  T = visitChildren(node)
     open fun visit(node: AtomIntNode):  T = visitChildren(node)
     open fun visit(node: AtomStringNode):  T = visitChildren(node)
     open fun visit(node: BinaryExpNode):  T = visitChildren(node)
     open fun visit(node: CallExpNode):  T = visitChildren(node)
     open fun visit(node: CallStatementNode):  T = visitChildren(node)
     open fun visit(node: DebugStatementNode):  T = visitChildren(node)
     open fun visit(node: ForStatementNode):  T = visitChildren(node)
     open fun visit(node: FunctionNode):  T = visitChildren(node)
     open fun visit(node: IfStatementNode):  T = visitChildren(node)
     open fun visit(node: PrintStatementNode):  T = visitChildren(node)
     open fun visit(node: ReadStatementNode):  T = visitChildren(node)
     open fun visit(node: NotExpNode):  T = visitChildren(node)
     open fun visit(node: WhileStatementNode):  T = visitChildren(node)

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

    abstract fun aggregateResult(agg: T, next: T): T

    abstract fun defaultValue(): T
}
