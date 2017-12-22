package org.ygl.model

enum class Op(val text: String)
{
    ADD("+"),
    SUB("-"),
    MUL("+"),
    DIV("/"),
    MOD("%"),
    AND("&&"),
    OR("||"),
    EQ("=="),
    NEQ("!="),
    GEQ(">="),
    LEQ("<="),
    LT("<"),
    GT(">"),
    NOT("!")
    ;

    override fun toString() = text

    companion object {
        fun parse(str: String): Op {
            return when(str) {
                "+" -> Op.ADD
                "-" -> Op.SUB
                "*" -> Op.MUL
                "/" -> Op.DIV
                "%" -> Op.MOD
                "||" -> Op.OR
                "&&" -> Op.AND
                "==" -> Op.EQ
                "!=" -> Op.NEQ
                ">=" -> Op.GEQ
                "<=" -> Op.LEQ
                "<" -> Op.LT
                ">" -> Op.GT
                "!" -> Op.NOT
                else -> throw Exception("invalid op: $str")
            }
        }
    }

}