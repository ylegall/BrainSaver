package org.ygl

import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer


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