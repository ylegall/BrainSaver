package org.ygl.runtime

import org.ygl.model.StorageType

open class Symbol(
        val name: String,
        val storage: StorageType = StorageType.VAL,
        val value: Any,
        val address: Int
) {
    val size: Int get() = when (value) {
        is String -> value.length
        is Int -> 1
        else -> 0
    }

    open fun isConstant() = false
    open fun isTemp() = false
    open fun hasAddress() = true
}

class TempSymbol(
        name: String,
        value: Any,
        address: Int
) : Symbol(name, StorageType.VAR, value, address) {
    override fun isTemp() = true
}

class ConstantSymbol(
        value: Any
) : Symbol("\$const($value)", StorageType.VAL, value, -1) {

    override fun isConstant() = true
    override fun hasAddress() = false
}

object UnknownSymbol : Symbol("", StorageType.VAL, Unit, -1)

fun Symbol.offset(offset: Int, name: String = this.name): Symbol {
    // TODO: arrays

    assert(this.address >= 0)
    return Symbol(
            "$name($offset)",
            this.storage,
            Unit,
            this.address.plus(offset)
    )
}

// TODO: move this
typealias BinaryOp = (Symbol, Symbol) -> Symbol