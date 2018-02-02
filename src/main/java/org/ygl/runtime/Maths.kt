package org.ygl.runtime

import org.ygl.model.StorageType

private typealias BinaryOp = (Symbol, Symbol) -> Symbol

class Maths(
        private val cg: CodeGen,
        private val runtime: Runtime
)
{
    fun divide(s1: Symbol, s2: Symbol): Symbol {
        return binaryOp(s1, s2, this::divideBy)
    }

    fun multiply(s1: Symbol, s2: Symbol): Symbol {
        return binaryOp(s1, s2, this::multiplyBy)
    }

    fun mod(s1: Symbol, s2: Symbol): Symbol {
        return binaryOp(s1, s2, this::modBy)
    }

    fun subtract(s1: Symbol, s2: Symbol): Symbol {
        return binaryOp(s1, s2, this::subtractFrom)
    }

    fun add(s1: Symbol, s2: Symbol): Symbol {
        return binaryOp(s1, s2, this::addTo)
    }

    private inline fun binaryOp(s1: Symbol, s2: Symbol, op: BinaryOp): Symbol {
        with (cg) {
            val result = runtime.createTempSymbol()
            copyInt(result, s1)
            op(result, s2)
            return result
        }
    }

    fun addTo(s1: Symbol, s2: Symbol): Symbol {
        with (cg) {
            commentLine("add $s2 to $s1")
            val tmp = runtime.createTempSymbol()
            cg.copyInt(tmp, s2)

            cf.loop(tmp, {
                dec(tmp)
                inc(s1)
            })

            runtime.delete(tmp)
            commentLine("end add $s2 to $s1")
            return s1
        }
    }

    fun subtractFrom(s1: Symbol, s2: Symbol): Symbol {
        with (cg) {
            commentLine("subtract $s2 from $s1")
            val cs = runtime.currentScope()

            if (cg.options.wrapping) {
                val breakFlag = runtime.createTempSymbol()
                val tmp = runtime.createTempSymbol()
                val tmp2 = runtime.createTempSymbol()
                copyInt(tmp, s2)

                cf.loop(tmp, {

                    load(breakFlag, 1)
                    copyInt(tmp2, s1)

                    cf.onlyIf(tmp2, {
                        dec(s1)
                        dec(tmp)
                        zero(breakFlag)
                    })

                    cf.onlyIf(breakFlag, {
                        zero(tmp)
                    })

                })

                cs.delete(tmp2)
                cs.delete(tmp)
                cs.delete(breakFlag)
            } else {
                val tmp = runtime.createTempSymbol()
                copyInt(tmp, s2)

                cf.loop(tmp, {
                    dec(s1)
                    dec(tmp)
                })

                cs.delete(tmp)
            }

            commentLine("end subtract $s2 from $s1")
            return s1
        }
    }

    fun multiplyBy(s1: Symbol, s2: Symbol): Symbol {
        with (cg) {
            commentLine("multiply $s1 by $s2")

            val t1 = runtime.createTempSymbol()
            val t2 = runtime.createTempSymbol()
            copyInt(t1, s1)
            copyInt(t2, s2)
            zero(s1)

            cf.loop(t2, {
                dec(t2)
                addTo(s1, t1)
            })

            runtime.delete(t2)
            runtime.delete(t1)
            commentLine("end multiply $s1 by $s2")
            return s1
        }
    }

    fun divideBy(s1: Symbol, s2: Symbol): Symbol {
        with (cg) {
            commentLine("$s1 /= $s2")

            val cpy = runtime.createTempSymbol()
            val div = runtime.createTempSymbol()
            val flag = runtime.createTempSymbol()

            zero(div)
            zero(flag)

            copyInt(cpy, s1)

            cf.loop(cpy, {
                subtractFrom(cpy, s2)
                inc(div)
            })

            runtime.delete(cpy)

            val remainder = multiply(div, s2)
            subtractFrom(remainder, s1)

            cf.onlyIf(remainder, {
                inc(flag)
                moveTo(remainder)
            })

            runtime.delete(remainder)
            subtractFrom(div, flag)
            runtime.delete(flag)
            copyInt(s1, div)

            runtime.delete(div)
            commentLine("end $s1 /= $s2")
            return s1
        }
    }

    fun modBy(s1: Symbol, s2: Symbol): Symbol {
        with (cg) {
            commentLine("$s1 %= $s2")

            val tmp = runtime.createTempSymbol()
            copyInt(tmp, s1)

            divideBy(tmp, s2)
            val prod = multiply(tmp, s2)
            subtractFrom(s1, prod)

            runtime.delete(prod)
            runtime.delete(tmp)

            commentLine("end $s1 %= $s2")
            return s1
        }
    }

    fun equal(lhs: Symbol, rhs: Symbol): Symbol {
        with (cg) {
            commentLine("$lhs == $rhs")
            val x = runtime.createSymbol("diff", StorageType.VAR)
            val z = runtime.createTempSymbol()

            load(z, 1)
            copyInt(x, lhs)
            subtractFrom(x, rhs)

            cf.onlyIf(x, {
                zero(z)
            })

            copyInt(x, rhs)
            subtractFrom(x, lhs)

            cf.onlyIf(x, {
                zero(z)
            })

            runtime.delete(x)
            return z
        }
    }

    fun notEqual(lhs: Symbol, rhs: Symbol): Symbol {
        with (cg) {
            commentLine("$lhs not equal $rhs")
            val cs = runtime.currentScope()
            val x = equal(lhs, rhs)
            val result = runtime.createTempSymbol()

            load(result, 1)

            cf.onlyIf(x, {
                zero(result)
            })

            cs.delete(x)
            return result
        }
    }

    fun lessThan(lhs: Symbol, rhs: Symbol): Symbol {
        with (cg) {
            commentLine("$lhs less than $rhs")
            val ret = runtime.createTempSymbol()
            val x = runtime.createTempSymbol()

            zero(ret)

            copyInt(x, lhs)
            subtractFrom(x, rhs)

            cf.onlyIf(x, {
                zero(ret)
            })

            copyInt(x, rhs)
            subtractFrom(x, lhs)

            cf.onlyIf(x, {
                load(ret, 1)
            })

            commentLine("end $lhs less than $rhs")
            return ret
        }
    }

    fun lessThanEqual(lhs: Symbol, rhs: Symbol): Symbol {
        with (cg) {
            commentLine("$lhs less than or equal $rhs")
            val ret = runtime.createTempSymbol()
            val x = runtime.createTempSymbol()

            load(ret, 1)
            copyInt(x, lhs)
            subtractFrom(x, rhs)

            cf.onlyIf(x, {
                zero(ret)
            })

            return ret
        }
    }

    fun greaterThanEqual(lhs: Symbol, rhs: Symbol): Symbol {
        cg.commentLine("${lhs.name} less than or equal ${rhs.name}")
        return lessThanEqual(rhs, lhs)
    }

    fun greaterThan(lhs: Symbol, rhs: Symbol): Symbol {
        with (cg) {
            commentLine("${lhs.name} greater than ${rhs.name}")
            val cs = runtime.currentScope()
            val ret = runtime.createTempSymbol()
            zero(ret)
            val z = subtract(lhs, rhs)

            cf.onlyIf(z, {
                load(ret, 1)
            })

            cs.delete(z)
            return ret
        }
    }

    fun and(lhs: Symbol, rhs: Symbol): Symbol {
        with (cg) {
            commentLine("${lhs.name} && ${rhs.name}")
            val cs = runtime.currentScope()
            val x = runtime.createTempSymbol()
            val y = runtime.createTempSymbol()
            val ret = runtime.createTempSymbol()

            copyInt(x, lhs)
            copyInt(y, rhs)

            moveTo(x)
            cf.startLoop()
                moveTo(y)
                cf.startLoop()
                    load(ret, 1)
                    zero(y)
                cf.endLoop()
                zero(x)
            cf.endLoop()

            cs.delete(x)
            cs.delete(y)

            return ret
        }
    }

    fun or(lhs: Symbol, rhs: Symbol): Symbol {
        with (cg) {
            commentLine("${lhs.name} || ${rhs.name}")
            val x = runtime.createTempSymbol()
            val ret = runtime.createTempSymbol()

            copyInt(x, lhs)

            cf.onlyIf(x, {
                load(ret, 1)
            })

            copyInt(x, rhs)

            cf.onlyIf(x, {
                load(ret, 1)
            })

            runtime.delete(x)
            return ret
        }
    }

    fun not(rhs: Symbol): Symbol {
        with (cg) {
            commentLine("not $rhs")
            val cs = runtime.currentScope()
            val tmp = runtime.createTempSymbol()
            val ret = runtime.createTempSymbol()
            copyInt(tmp, rhs)
            load(ret, 1)

            cf.onlyIf(tmp, {
                zero(ret)
            })

            cs.delete(tmp)
            return ret
        }
    }
}