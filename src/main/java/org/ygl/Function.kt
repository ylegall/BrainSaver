package org.ygl

/**
 *
 */
class Function(val name: String, val ctx: BrainStoolParser.FunctionContext) {

    override fun toString(): String {
        return "Function(name='$name')"
    }
}