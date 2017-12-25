package org.ygl.runtime

import org.ygl.Type
import org.ygl.model.NullValue
import org.ygl.model.StorageType
import org.ygl.model.Value

open class NamedSymbol(val name: String) {
    fun getKey(): String = name
}

open class StoredSymbol(
        name: String,
        val storage: StorageType
): NamedSymbol(name) {
    fun isMutable() = storage == StorageType.VAR
}

class ValuedSymbol(
        name: String,
        storage: StorageType,
        var value: Value = NullValue
): StoredSymbol(name, storage)

class Symbol(
        name: String,
        storage: StorageType,
        val type: Type = Type.INT,
        val address: Int
) : StoredSymbol(name, storage)
