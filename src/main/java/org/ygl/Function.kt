package org.ygl

class Function(val name: String, ctx: BrainLoveParser.FunctionContext) {

    override fun toString(): String {
        return "Function(name='$name')"
    }
}