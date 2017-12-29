package org.ygl.runtime

import org.ygl.model.NullValue
import org.ygl.model.StorageType
import org.ygl.model.Value

open class Symbol(
        val name: String,
        val storage: StorageType = StorageType.VAL,
        var value: Value,
        var address: Int
) {
    val size: Int get() = value.getSize()
    val type: Type get() = value.getType()

    open fun isConstant() = false
    open fun isTemp() = false
    open fun hasAddress() = true

}

class TempSymbol(
        name: String,
        value: Value,
        address: Int
) : Symbol(name, StorageType.VAR, value, address) {
    override fun isTemp() = true
}

class ConstantSymbol(
        value: Value
) : Symbol("\$const($value)", StorageType.VAL, value, -1) {

    override fun isConstant() = true
    override fun hasAddress() = false
}

object UnknownSymbol : Symbol("", StorageType.VAL, NullValue, -1)

fun Symbol.offset(offset: Int, name: String = this.name): Symbol {
    // TODO: arrays

    assert(this.address >= 0)
    return Symbol(
            "$name($offset)",
            this.storage,
            NullValue,
            this.address.plus(offset)
    )
}

typealias BinaryOp = (Symbol, Symbol) -> Symbol