package org.ygl

import java.io.Closeable
import java.io.OutputStream


class MinifyingOutputStream(
        val output: OutputStream,
        val marginSize: Int = 64) : OutputStream(), Closeable
{
    var bi = 0
    val buffer = ByteArray(1024)

    val validSet: Set<Byte> = "[]<>+-,.".map { it.toByte() }.toSet()

    override fun write(c: Int) {
        val byte = c.toByte()
        if (byte !in validSet) return

        if (marginSize == 0) {
            output.write(c)
        } else {
            buffer[bi] = byte
            bi += 1
            if (bi == marginSize) {
                output.write(buffer.sliceArray(0 .. bi))
                output.write('\n'.toInt())
                bi = 0
            }
        }
    }

    override fun flush() {
        output.write(buffer.sliceArray(0 .. bi))
        output.flush()
    }

    override fun close() {
        output.close()
    }
}