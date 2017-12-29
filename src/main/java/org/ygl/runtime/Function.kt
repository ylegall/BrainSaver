package org.ygl.runtime

import org.ygl.ast.AstNode

class Function(
        val name: String,
        val node: AstNode,
        val params: List<AstNode>,
        val returnType: Type
)