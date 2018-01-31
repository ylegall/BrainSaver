package org.ygl.runtime

class ControlFlow(
        val cg: CodeGen
)
{
    fun startIf(condition: Symbol) {
        with (cg) {
            commentLine("if $condition")
            moveTo(condition)
            startLoop()
            setZero(condition)
        }
    }

    // is passed the tmp variable from the startIf
    fun endIf(condition: Symbol) {
        with (cg) {
            moveTo(condition)
            endLoop()
            commentLine("end if")
        }
    }

    fun startElse(elseFlag: Symbol) {
        with (cg) {
            commentLine("else $elseFlag")
            moveTo(elseFlag)
            startLoop()
            setZero(elseFlag)
        }
    }

    fun endElse(elseFlag: Symbol) {
        with (cg) {
            moveTo(elseFlag)
            endLoop()
            commentLine("end else $elseFlag")
        }
    }

    inline fun onlyIf(symbol: Symbol, body: () -> Unit, comment: String = "") {
        with (cg) {
            moveTo(symbol)
            startLoop(comment)
            body()
            setZero(symbol)
            endLoop()
        }
    }

    fun startLoop(comment: String = "") {
        with (cg) {
            newline()
            emit("[", comment)
            nestLevel = Math.min(nestLevel + 1, 10)
            newline()
        }
    }

    fun endLoop(comment: String = "") {
        with (cg) {
            nestLevel -= 1
            assert(nestLevel >= 0, { "negative nest level" })
            newline()
            emit("]", comment)
            newline()
        }
    }

    inline fun loop(symbol: Symbol, body: () -> Unit, comment: String = "") {
        with (cg) {
            moveTo(symbol)
            startLoop(comment)
            body()
            moveTo(symbol)
            endLoop()
        }
    }
}