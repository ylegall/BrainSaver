package org.ygl

/**
 *
 */
class Function(
        val name: String,
        val ctx: BrainSaverParser.FunctionContext,
        val isVoid: Boolean
) {

    override fun toString(): String {
        return "Function(name='$name')"
    }
}