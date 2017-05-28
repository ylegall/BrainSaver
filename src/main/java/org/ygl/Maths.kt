package org.ygl

class Maths(val codegen: CodeGen)
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

    inline fun binaryOp(s1: Symbol, s2: Symbol, op: BinaryOp): Symbol {
        with (codegen) {
            val result = currentScope().getTempSymbol()
            assign(result, s1)
            op(result, s2)
            return result
        }
    }

    fun addTo(s1: Symbol, s2: Symbol): Symbol {
        with (codegen) {
            commentLine("add $s2 to $s1")
            val tmp = currentScope().getTempSymbol()
            assign(tmp, s2)

            loop(tmp, {
                dec(tmp)
                inc(s1)
            })

            currentScope().delete(tmp)
            commentLine("end add $s2 to $s1")
            return s1
        }
    }

    fun subtractFrom(s1: Symbol, s2: Symbol): Symbol {
        with (codegen) {
            commentLine("subtract $s2 from $s1")

            if (codegen.options.wrapping) {
                val breakFlag = currentScope().getTempSymbol()
                val tmp = currentScope().getTempSymbol()
                val tmp2 = currentScope().getTempSymbol()
                assign(tmp, s2)

                loop(tmp, {

                    loadInt(breakFlag, 1)
                    assign(tmp2, s1)

                    onlyIf(tmp2, {
                        dec(s1)
                        dec(tmp)
                        setZero(breakFlag)
                    })

                    onlyIf(breakFlag, {
                        setZero(tmp)
                    })

                })

                currentScope().delete(tmp2)
                currentScope().delete(tmp)
                currentScope().delete(breakFlag)
            } else {
                val tmp = currentScope().getTempSymbol()
                assign(tmp, s2)

                loop(tmp, {
                    dec(s1)
                    dec(tmp)
                })

                currentScope().delete(tmp)
            }

            commentLine("end subtract $s2 from $s1")
            return s1
        }
    }

    fun multiplyBy(s1: Symbol, s2: Symbol): Symbol {
        with (codegen) {
            commentLine("multiply $s1 by $s2")

            val t1 = currentScope().getTempSymbol()
            val t2 = currentScope().getTempSymbol()
            assign(t1, s1)
            assign(t2, s2)
            setZero(s1)

            loop(t2, {
                dec(t2)
                addTo(s1, t1)
            })

            currentScope().delete(t2)
            currentScope().delete(t1)
            commentLine("end multiply $s1 by $s2")
            return s1
        }
    }

    fun divideBy(s1: Symbol, s2: Symbol): Symbol {
        with (codegen) {
            commentLine("$s1 /= $s2")

            val cpy = currentScope().getTempSymbol()
            val div = currentScope().getTempSymbol()
            val flag = currentScope().getTempSymbol()

            setZero(div)
            setZero(flag)

            assign(cpy, s1)

            loop(cpy, {
                subtractFrom(cpy, s2)
                inc(div)
            })

            currentScope().delete(cpy)

            val remainder = multiply(div, s2)
            subtractFrom(remainder, s1)

            onlyIf(remainder, {
                inc(flag)
                moveTo(remainder)
            })

            currentScope().delete(remainder)
            subtractFrom(div, flag)
            currentScope().delete(flag)
            assign(s1, div)

            currentScope().delete(div)
            commentLine("end $s1 /= $s2")
            return s1
        }
    }

    fun modBy(s1: Symbol, s2: Symbol): Symbol {
        with (codegen) {
            commentLine("$s1 %= $s2")

            val tmp = currentScope().getTempSymbol()
            assign(tmp, s1)

            divideBy(tmp, s2)
            val prod = multiply(tmp, s2)
            subtractFrom(s1, prod)

            currentScope().delete(prod)
            currentScope().delete(tmp)

            commentLine("end $s1 %= $s2")
            return s1
        }
    }

    fun equal(lhs: Symbol, rhs: Symbol): Symbol {
        with (codegen) {
            commentLine("$lhs == $rhs")

            val x = currentScope().createSymbol("diff")
            val z = currentScope().getTempSymbol()

            loadInt(z, 1)

            assign(x, lhs)
            math.subtractFrom(x, rhs)

            onlyIf(x, {
                setZero(z)
            })

            assign(x, rhs)
            math.subtractFrom(x, lhs)

            onlyIf(x, {
                setZero(z)
            })

            currentScope().delete(x)
            return z
        }
    }

    fun notEqual(lhs: Symbol, rhs: Symbol): Symbol {
        with (codegen) {
            commentLine("$lhs not equal $rhs")
            val x = equal(lhs, rhs)
            val result = currentScope().getTempSymbol()

            loadInt(result, 1)

            onlyIf(x, {
                setZero(result)
            })

            currentScope().delete(x)
            return result
        }
    }

    fun lessThan(lhs: Symbol, rhs: Symbol): Symbol {
        with (codegen) {
            commentLine("$lhs less than $rhs")

            val ret = currentScope().getTempSymbol()
            val x = currentScope().getTempSymbol()

            setZero(ret)

            assign(x, lhs)
            math.subtractFrom(x, rhs)

            onlyIf(x, {
                setZero(ret)
            })

            assign(x, rhs)
            math.subtractFrom(x, lhs)

            onlyIf(x, {
                loadInt(ret, 1)
            })

            commentLine("end $lhs less than $rhs")
            return ret
        }
    }

    fun lessThanEqual(lhs: Symbol, rhs: Symbol): Symbol {
        with (codegen) {
            commentLine("$lhs less than or equal $rhs")

            val ret = currentScope().getTempSymbol()
            val x = currentScope().getTempSymbol()

            loadInt(ret, 1)
            assign(x, lhs)
            math.subtractFrom(x, rhs)

            onlyIf(x, {
                setZero(ret)
            })

            return ret
        }
    }

    fun greaterThanEqual(lhs: Symbol, rhs: Symbol): Symbol {
        codegen.commentLine("${lhs.name} less than or equal ${rhs.name}")
        return lessThanEqual(rhs, lhs)
    }

    // TODO: optimize
    fun greaterThan(lhs: Symbol, rhs: Symbol): Symbol {
        with (codegen) {
            commentLine("${lhs.name} greater than ${rhs.name}")

            val ret = currentScope().getTempSymbol()
            setZero(ret)
            val z = math.subtract(lhs, rhs)

            onlyIf(z, {
                loadInt(ret, 1)
            })

            currentScope().delete(z)

            return ret
        }
    }

    fun and(lhs: Symbol, rhs: Symbol): Symbol {
        with (codegen) {
            commentLine("${lhs.name} && ${rhs.name}")
            val x = currentScope().getTempSymbol()
            val y = currentScope().getTempSymbol()
            val ret = currentScope().getTempSymbol()

            assign(x, lhs)
            assign(y, rhs)

            moveTo(x)
            startLoop()
                moveTo(y)
                startLoop()
                    loadInt(ret, 1)
                    setZero(y)
                endLoop()
                setZero(x)
            endLoop()

            currentScope().delete(x)
            currentScope().delete(y)

            return ret
        }
    }

    fun or(lhs: Symbol, rhs: Symbol): Symbol {
        with (codegen) {
            commentLine("${lhs.name} || ${rhs.name}")
            val x = currentScope().getTempSymbol()
            val ret = currentScope().getTempSymbol()

            assign(x, lhs)

            onlyIf(x, {
                loadInt(ret, 1)
            })

            assign(x, rhs)

            onlyIf(x, {
                loadInt(ret, 1)
            })

            currentScope().delete(x)

            return ret
        }
    }

    fun not(rhs: Symbol): Symbol {
        with (codegen) {
            commentLine("not $rhs")
            val tmp = currentScope().getTempSymbol()
            val ret = currentScope().getTempSymbol()
            assign(tmp, rhs)
            loadInt(ret, 1)

            onlyIf(tmp, {
                setZero(ret)
            })

            currentScope().delete(tmp)

            return ret
        }
    }
}