package org.ygl.runtime

import org.ygl.model.NullValue
import org.ygl.model.StorageType
import org.ygl.model.Value

open class NamedSymbol(
        val name: String
)

open class ValueSymbol(
        name: String,
        val storage: StorageType = StorageType.VAL
) : NamedSymbol(name) {

    var value: Value = NullValue
    val size: Int get() = value.getSize()
    val type: Type get() = value.getType()

    constructor(name: String, storage: StorageType, value: Value): this(name, storage) {
        this.value = value
    }

}

open class Symbol(
        name: String,
        storage: StorageType,
        value: Value,
        val address: Int
) : ValueSymbol(name, storage, value)

object UnknownSymbol : Symbol("", StorageType.VAL, NullValue, -1)
