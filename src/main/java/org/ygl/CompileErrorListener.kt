package org.ygl

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.ygl.ast.SourceInfo


class CompileErrorListener : BaseErrorListener() {

    @Throws(ParseCancellationException::class)
    override fun syntaxError(
            recognizer: Recognizer<*, *>,
            offendingSymbol: Any,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException)
    {
        throw ParseCancellationException("line $line:$charPositionInLine $msg")
    }

    companion object {
        val INSTANCE = CompileErrorListener()
    }
}

class CompileException(
        message: String,
        sourceInfo: SourceInfo? = null
) : Exception(
        if (sourceInfo != null) {
            "compilation error: '$message' at line ${sourceInfo.line} col ${sourceInfo.col}"
        } else {
            message
        }
) {
    constructor(message: String, ctx: ParserRuleContext): this(message, SourceInfo(ctx))
}