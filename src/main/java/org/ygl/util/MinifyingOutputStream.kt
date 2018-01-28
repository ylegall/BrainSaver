package org.ygl.util

import java.io.Closeable
import java.io.OutputStream


class MinifyingOutputStream(
        private val output: OutputStream,
        private val marginSize: Int = 64) : OutputStream(), Closeable
{
    private var bi = 0
    private val buffer = ByteArray(1024)
    private val validSet: Set<Char> = "[]<>+-,.".toSet()

    init {
        if (marginSize <= 0) throw Exception("positive margin size required")
    }

    override fun write(c: Int) {
        val ch = c.toChar()
        if (ch !in validSet) return

        if (bi > 0) {
            val prev = buffer[bi-1].toChar()
            if ((ch == '<' && prev == '>') ||
                (ch == '>' && prev == '<')
            ) {
                bi -= 1
                return
            }
        }

        buffer[bi] = ch.toByte()
        bi += 1
        if (bi == marginSize) {
            output.write(buffer.sliceArray(0 until bi))
            output.write('\n'.toInt())
            bi = 0
        }
    }

    override fun flush() {
        if (bi > 0) output.write(buffer.sliceArray(0 until bi))
        bi = 0
        output.flush()
    }

    override fun close() {
        flush()
        output.close()
    }
}