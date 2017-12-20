package org.ygl

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.misc.ParseCancellationException


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
        ctx: ParserRuleContext? = null
) : Exception(
        if (ctx != null) {
            "compilation error: '$message' at line ${ctx.start.line} col ${ctx.start.charPositionInLine}"
        } else {
            message
        }
)