package org.ygl

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.pattern.ParseTreeMatch
import org.antlr.v4.runtime.tree.pattern.ParseTreePattern
import org.ygl.BrainSaverParser.*

private class Transform(
        val pattern: ParseTreePattern,
        val action: (ParseTreeMatch) -> String
)

class SourceTransformations(private val parser: BrainSaverParser) : BrainSaverBaseListener()
{
    private val expressionTransforms = listOf(
            Transform(pattern("<exp> + 0", RULE_exp), { match -> match["exp"].text }),
            Transform(pattern("<exp> - 0", RULE_exp), { match -> match["exp"].text }),
            Transform(pattern("<exp> * 0", RULE_exp), { _ -> "0" }),
            Transform(pattern("<exp> * 1", RULE_exp), { match -> match["exp"].text }),
            Transform(pattern("<exp> / 1", RULE_exp), { match -> match["exp"].text }),
            Transform(pattern("<exp> * 2", RULE_exp), { match -> "${match["exp"].text} + ${match["exp"].text}" })
    )

    private val conditionTransforms = listOf(
            Transform(pattern("if (0) { <statementList> }", RULE_ifStatement), { _ -> "" }),
            Transform(pattern("if (1) { <statementList> }", RULE_ifStatement), { match -> match["statementList"].text }),
            Transform(pattern("if (!<exp>) { <a:statementList> } else { <b:statementList> }", RULE_ifStatement),
                    { match -> "if (${match["exp"].text}) { ${match["b"].text} } else { ${match["a"].text} }" })
    )

    private fun pattern(str: String, type: Int): ParseTreePattern {
        return parser.compileParseTreePattern(str, type)
    }

    override fun enterIfStatement(ctx: IfStatementContext?) {
        for (transform in conditionTransforms) {
            val match = transform.pattern.match(ctx)
            if (match.succeeded()) {
                println(transform.action(match))
            }
            return
        }
        println(ctx!!.text)
    }

    override fun enterOpExp(ctx: OpExpContext?) {
        for (transform in expressionTransforms) {
            val match = transform.pattern.match(ctx)
            if (match.succeeded()) {
                println(transform.action(match))
            }
            return
        }
        println(ctx!!.text)
    }

    override fun enterEveryRule(ctx: ParserRuleContext?) {
        println(ctx!!.text)
    }
}