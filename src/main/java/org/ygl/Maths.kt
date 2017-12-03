package org.ygl

class Maths(private val cg: CodeGen)
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
            val result = currentScope().getTempSymbol()
            assign(result, s1)
            op(result, s2)
            return result
        }
    }

    fun addTo(s1: Symbol, s2: Symbol): Symbol {
        with (cg) {
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
        with (cg) {
            commentLine("subtract $s2 from $s1")
            val cs = cg.currentScope()

            if (cg.options.wrapping) {
                val breakFlag = currentScope().getTempSymbol()
                val tmp = cs.getTempSymbol()
                val tmp2 = cs.getTempSymbol()
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

                cs.delete(tmp2)
                cs.delete(tmp)
                cs.delete(breakFlag)
            } else {
                val tmp = cs.getTempSymbol()
                assign(tmp, s2)

                loop(tmp, {
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
            val cs = cg.currentScope()

            val t1 = cs.getTempSymbol()
            val t2 = cs.getTempSymbol()
            assign(t1, s1)
            assign(t2, s2)
            setZero(s1)

            loop(t2, {
                dec(t2)
                addTo(s1, t1)
            })

            cs.delete(t2)
            cs.delete(t1)
            commentLine("end multiply $s1 by $s2")
            return s1
        }
    }

    fun divideBy(s1: Symbol, s2: Symbol): Symbol {
        with (cg) {
            commentLine("$s1 /= $s2")
            val cs = cg.currentScope()

            val cpy = cs.getTempSymbol()
            val div = cs.getTempSymbol()
            val flag = cs.getTempSymbol()

            setZero(div)
            setZero(flag)

            assign(cpy, s1)

            loop(cpy, {
                subtractFrom(cpy, s2)
                inc(div)
            })

            cs.delete(cpy)

            val remainder = multiply(div, s2)
            subtractFrom(remainder, s1)

            onlyIf(remainder, {
                inc(flag)
                moveTo(remainder)
            })

            cs.delete(remainder)
            subtractFrom(div, flag)
            cs.delete(flag)
            assign(s1, div)

            cs.delete(div)
            commentLine("end $s1 /= $s2")
            return s1
        }
    }

    fun modBy(s1: Symbol, s2: Symbol): Symbol {
        with (cg) {
            commentLine("$s1 %= $s2")
            val cs = cg.currentScope()

            val tmp = cs.getTempSymbol()
            assign(tmp, s1)

            divideBy(tmp, s2)
            val prod = multiply(tmp, s2)
            subtractFrom(s1, prod)

            cs.delete(prod)
            cs.delete(tmp)

            commentLine("end $s1 %= $s2")
            return s1
        }
    }

    fun equal(lhs: Symbol, rhs: Symbol): Symbol {
        with (cg) {
            commentLine("$lhs == $rhs")
            val cs = cg.currentScope()
            val x = cs.createSymbol("diff")
            val z = cs.getTempSymbol()

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

            cs.delete(x)
            return z
        }
    }

    fun notEqual(lhs: Symbol, rhs: Symbol): Symbol {
        with (cg) {
            commentLine("$lhs not equal $rhs")
            val cs = cg.currentScope()
            val x = equal(lhs, rhs)
            val result = cs.getTempSymbol()

            loadInt(result, 1)

            onlyIf(x, {
                setZero(result)
            })

            cs.delete(x)
            return result
        }
    }

    fun lessThan(lhs: Symbol, rhs: Symbol): Symbol {
        with (cg) {
            commentLine("$lhs less than $rhs")
            val cs = cg.currentScope()
            val ret = cs.getTempSymbol()
            val x = cs.getTempSymbol()

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
        with (cg) {
            commentLine("$lhs less than or equal $rhs")
            val cs = cg.currentScope()
            val ret = cs.getTempSymbol()
            val x = cs.getTempSymbol()

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
        cg.commentLine("${lhs.name} less than or equal ${rhs.name}")
        return lessThanEqual(rhs, lhs)
    }

    fun greaterThan(lhs: Symbol, rhs: Symbol): Symbol {
        with (cg) {
            commentLine("${lhs.name} greater than ${rhs.name}")
            val cs = cg.currentScope()
            val ret = cs.getTempSymbol()
            setZero(ret)
            val z = math.subtract(lhs, rhs)

            onlyIf(z, {
                loadInt(ret, 1)
            })

            cs.delete(z)
            return ret
        }
    }

    fun and(lhs: Symbol, rhs: Symbol): Symbol {
        with (cg) {
            commentLine("${lhs.name} && ${rhs.name}")
            val cs = cg.currentScope()
            val x = cs.getTempSymbol()
            val y = cs.getTempSymbol()
            val ret = cs.getTempSymbol()

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

            cs.delete(x)
            cs.delete(y)

            return ret
        }
    }

    fun or(lhs: Symbol, rhs: Symbol): Symbol {
        with (cg) {
            commentLine("${lhs.name} || ${rhs.name}")
            val cs = cg.currentScope()
            val x = cs.getTempSymbol()
            val ret = cs.getTempSymbol()

            assign(x, lhs)

            onlyIf(x, {
                loadInt(ret, 1)
            })

            assign(x, rhs)

            onlyIf(x, {
                loadInt(ret, 1)
            })

            cs.delete(x)
            return ret
        }
    }

    fun not(rhs: Symbol): Symbol {
        with (cg) {
            commentLine("not $rhs")
            val cs = cg.currentScope()
            val tmp = cs.getTempSymbol()
            val ret = cs.getTempSymbol()
            assign(tmp, rhs)
            loadInt(ret, 1)

            onlyIf(tmp, {
                setZero(ret)
            })

            cs.delete(tmp)
            return ret
        }
    }
}