package org.ygl.transformation

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Test
import org.ygl.BrainSaverLexer
import org.ygl.BrainSaverParser
import org.ygl.CompileErrorListener
import org.ygl.ast.AstBuilder
import org.ygl.ast.AstNode
import org.ygl.ast.AtomIntNode
import org.ygl.parse
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals

internal class ExpressionEvaluatorTest {

    private val expEval = ExpressionEvaluator()

    @Test
    fun testExp() {
        val node = ast("""(3 * 4 - 8) * 10 + 2""")
        val result = expEval.evaluate(node)
        assertEquals(42, (result as AtomIntNode).value)
    }

    @Test
    fun testSymbolExp() {
        val node = ast("""(50 / x) + (y % 4)""")
        val symbols = mutableMapOf(Pair("x", AtomIntNode(5)), Pair("y", AtomIntNode(6)))
        val result = expEval.evaluate(node, symbols)
        assertEquals(12, (result as AtomIntNode).value)
    }

    @Test
    fun testNotExp() {
        val node = ast("""!(5 <= 6)""")
        val result = expEval.evaluate(node)
        assertEquals(0, (result as AtomIntNode).value)
    }

    @Test
    fun testConditionalExp() {
        run {
            val node = ast(""" (4 > 3)? 17 : 13 """)
            val result = expEval.evaluate(node)
            assertEquals(17, (result as AtomIntNode).value)
        }
        run {
            val node = ast(""" (4 == 3)? 17 : 13 """)
            val result = expEval.evaluate(node)
            assertEquals(13, (result as AtomIntNode).value)
        }
    }

    @Test
    fun testArrayReadExp() {
        val node = ast("""a[x] + a[0]""")
        val symbols = mutableMapOf(
                Pair("x", AtomIntNode(1)),
                Pair("a[0]", AtomIntNode(6)),
                Pair("a[1]", AtomIntNode(-2))
        )
        val result = expEval.evaluate(node, symbols)
        assertEquals(4, (result as AtomIntNode).value)
    }

    private fun ast(program: String): AstNode {
        val lexer = BrainSaverLexer(CharStreams.fromStream(program.byteInputStream()))
        val tokens = CommonTokenStream(lexer)
        val parser = BrainSaverParser(tokens)
        parser.addErrorListener(CompileErrorListener.INSTANCE)
        return AstBuilder().visit(parser.exp())
    }
}