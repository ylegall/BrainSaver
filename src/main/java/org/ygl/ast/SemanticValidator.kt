package org.ygl.ast

import org.ygl.CompileException
import org.ygl.model.StorageType
import org.ygl.runtime.StdLib
import java.util.*

/**
 *
 */
class SemanticValidator(
        private val stdlib: StdLib
): AstWalker<Unit>() {

    private val context = ArrayDeque<MutableMap<String, AstNode>>()
    private val errors = mutableListOf<CompileException>()
    private val functions = mutableSetOf<String>()

    fun validate(node: AstNode): List<CompileException> {
        context.clear()
        errors.clear()
        functions.clear()

        visit(node)

        return errors
    }

    override fun visit(node: ProgramNode) {
        context.push(mutableMapOf())

        node.children.filterIsInstance<ConstantNode>().forEach { visit(it) }
        node.children.filterIsInstance<GlobalVariableNode>().forEach { visit(it) }
        node.children.filterIsInstance<FunctionNode>().forEach { visit(it) }

        context.pop()
    }

    override fun visit(node: FunctionNode) {
        if (node.name in functions) {
            errors.add(CompileException("duplicate declaration: ${node.name}", node))
        }
        functions.add(node.name)
        context.push(mutableMapOf())
        node.params.forEach { addSymbol(it, DeclarationNode(StorageType.VAL, it, EmptyNode)) }
        node.statements.forEach { visit(it) }
        context.pop()
    }

    override fun visit(node: ConstantNode) = addSymbol(node.lhs, node)
    override fun visit(node: GlobalVariableNode) = addSymbol(node.lhs, node)
    override fun visit(node: ArrayConstructorNode) = addSymbol(node.array, node)
    override fun visit(node: ArrayLiteralNode) = addSymbol(node.array, node)
    override fun visit(node: AssignmentNode) = validateWrite(node.lhs, node)

    private fun validateWrite(name: String, node: AstNode) {
        val symbol = resolveSymbol(name)
        when (symbol) {
            EmptyNode -> errors.add(CompileException("undefined identifier $name", node))
            is DeclarationNode -> if (symbol.storage == StorageType.VAL) {
                errors.add(CompileException("val $name cannot be reassigned", node))
            }
            is ArrayLiteralNode -> if (symbol.storage == StorageType.VAL) {
                errors.add(CompileException("val $name cannot be reassigned", node))
            }
            is ArrayConstructorNode -> if (symbol.storage == StorageType.VAL) {
                errors.add(CompileException("val $name cannot be reassigned", node))
            }
        }
    }

    override fun visit(node: CallExpNode) {
        if (node.name !in functions) {
            errors.add(CompileException("undefined function ${node.name}", node))
        }
    }

    override fun visit(node: CallStatementNode) {
        if (node.name !in stdlib.functions && node.name !in functions) {
            errors.add(CompileException("undefined function ${node.name}", node))
        }
    }

    override fun visit(node: AtomIdNode) {
        val symbol = resolveSymbol(node.identifier)
        if (symbol == EmptyNode) {
            errors.add(CompileException("undefined identifier ${node.identifier}", node))
        }
    }

    override fun visit(node: DeclarationNode) {
        val symbol = context.peek()[node.lhs] ?: EmptyNode
        if (symbol != EmptyNode) {
            errors.add(CompileException("duplicate declaration: ${node.lhs}", node))
        }
        addSymbol(node.lhs, node)
    }

    private fun addSymbol(name: String, node: AstNode) {
        val symbol = context.peek()[name] ?: EmptyNode
        if (symbol != EmptyNode) {
            errors.add(CompileException("duplicate declaration: $name", node))
        }
        context.peek()[name] = node
    }

    private fun resolveSymbol(name: String): AstNode {
        return context.find { name in it }?.get(name) ?: EmptyNode
    }

    override fun defaultValue(node: AstNode) {}
}