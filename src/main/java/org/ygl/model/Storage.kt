package org.ygl.model

import com.sun.org.apache.xalan.internal.xsltc.compiler.CompilerException

enum class StorageType
{
    VAL,
    VAR
    ;

    companion object {
        fun parse(str: String): StorageType {
            return when (str) {
                "val" -> VAL
                "var" -> VAR
                else -> throw CompilerException("unknown storage type: $str")
            }
        }
    }
}