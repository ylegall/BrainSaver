package org.ygl

import java.io.OutputStream

class TeeOutputStream(private val stream: OutputStream): OutputStream()
{
    override fun write(i: Int) {
        stream.write(i)
        System.`out`.print(i.toChar())
    }

    override fun flush() {
        stream.flush()
        System.`out`.flush()
    }

    override fun close() {
        stream.close()
    }
}