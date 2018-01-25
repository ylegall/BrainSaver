package org.ygl.runtime

import org.ygl.model.*

/**
 *
 */
open class Symbol private constructor(
        val name: String,
        val storage: StorageType = StorageType.VAL,
        val size: Int = 1,
        val type: Type = IntType,
        val address: Int = -1,
        val value: Any = Unit,
        val isTemp: Boolean = false
) {
    val isConstant = value != Unit
    val hasAddress = address >= 0

    open fun copy(newName: String = name) = Symbol(newName, storage, size, type, address, value, isTemp)

    override fun toString(): String {
        val addr = if (size > 1) "$address~${address + size - 1}" else "$address"
        val contents = when (value) {
            is String, Int -> "=$value"
            else -> ""
        }
        return "$name{$addr}$contents"
    }

    object NullSymbol: Symbol("", StorageType.VAL, -1, NullType, -1, Unit, false) {
        override fun copy(newName: String): Symbol {
            return this
        }
        override fun toString() = "(nil)"
    }

    fun offset(offset: Int): Symbol {
        // TODO: arrays
        assert(address >= 0)
        return Symbol(
                "$name($offset)",
                storage,
                size,
                type,
                address.plus(offset)
        )
    }

    companion object {
        fun new(name: String,
                storage: StorageType = StorageType.VAL,
                size: Int = 1,
                type: Type = IntType,
                address: Int = -1
        ) = Symbol(name, storage, size, type, address, Unit, false)

        fun temp(name: String,
                       storage: StorageType = StorageType.VAL,
                       size: Int = 1,
                       type: Type = IntType,
                       address: Int = -1) =
                Symbol(name, storage, size, type, address, Unit, true)

        fun constant(value: Any): Symbol {
            return Symbol("\$const($value)", StorageType.VAL, getSize(value), getType(value), -1, value, false)
        }
    }
}

//open class Symbol(
//        val name: String,
//        val storage: StorageType = StorageType.VAL,
//        val size: Int = 1,
//        val type: Type = IntType,
//        val address: Int = -1,
//        val isTemp: Boolean = false
//) {
//
////    constructor(value: Any): this("\$const($value)", StorageType.VAL, getSize(value), getType(value), -1) {
////        this.value = value
////    }
//
//    open fun isConstant() = false
//    open val value: Any = Unit
//
//    open fun copy(newName: String = name): Symbol {
//        return Symbol(name, storage, size, type, address)
//    }
//
//    override fun toString(): String {
//        val addr = if (size > 1) "$address~${address + size - 1}" else "$address"
////        val contents = when (value) {
////            is String, Int -> "=$value"
////            else -> ""
////        }
////        return "$name{$addr}$contents"
//        return "$name{$addr}"
//    }
//}
//
//class ConstantSymbol(
//        override val value: Any
//) : Symbol("\$const($value)", StorageType.VAL, getSize(value), getType(value), -1) {
//
//    override fun isConstant() = true
//    override fun copy(newName: String) = ConstantSymbol(value)
//    override fun toString() = name
//}

//object NullSymbol : Symbol("", StorageType.VAL, -1, NullType, -1) {
//    override fun copy(newName: String): Symbol {
//        return this
//    }
//    override fun toString() = "(nil)"
//}

//fun Symbol.offset(offset: Int, name: String = this.name): Symbol {
//    // TODO: arrays
//
//    assert(this.address >= 0)
//    return Symbol(
//            "$name($offset)",
//            this.storage,
//            this.size,
//            this.type,
//            this.address.plus(offset)
//    )
//}

fun getSize(value: Any): Int {
    return when (value) {
        is Int -> 1
        is String -> value.length
        else -> -1
    }
}