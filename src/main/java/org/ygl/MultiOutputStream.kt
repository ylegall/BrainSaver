package org.ygl

import java.io.OutputStream

/**
 */
class MultiOutputStream(val os1: OutputStream, val os2: OutputStream) : OutputStream(), AutoCloseable
{
    init {
        if (os1 === os2) throw Exception("streams refer to the same object")
    }

    override fun write(i: Int) {
        os1.write(i)
        os2.write(i)
    }

    override fun flush() {
        os1.flush()
        os2.flush()
    }

    override fun close() {
        os1.close()
        os2.close()
    }
}