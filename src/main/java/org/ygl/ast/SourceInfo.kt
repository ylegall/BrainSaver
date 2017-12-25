package org.ygl.ast

import org.antlr.v4.runtime.ParserRuleContext

class SourceInfo(
        val line: Int,
        val col: Int
) {
    constructor(ctx: ParserRuleContext) : this(ctx.start.line, ctx.start.charPositionInLine)
}