package org.ygl.transformation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.ygl.ast.*
import org.ygl.parse

internal class AssignmentResolverTest {

    @Test
    fun testComplex() {
        val program = """
            fn main() {
                val z = 3;
                var x = 1;
                while (x > 0) {
                    var y = 0;
                    if (x == 1) {
                        y = 7;
                    } else {
                        y = z + 4;
                    }
                    x = 3;
                    print(y);
                }
            }
        """

        val env = buildEnv(program)

        val whileEnv = env.entries.find { it.key.children[0] is WhileStatementNode }
                ?.value
                ?: fail("while stmt env not found")
        assertEquals(EmptyNode, whileEnv["x"])

        val callEnv = env.entries.find { it.key.children[0] is CallStatementNode }
                ?.value
                ?: fail("print stmt env not found")
        assertEquals(AtomIntNode(7), callEnv["y"])
    }

    @Test
    fun testIfElse() {
        val program = """
            fn main() {
                var x = readInt();
                var y = 1;
                if (x) {
                    x += 1;
                    y *= 2;
                } else {
                    x += 3;
                    y -= 1;
                }
                print(x, y);
            }
        """

        val env = buildEnv(program)

        val printEnv = env.entries.find { it.key.children[0] is CallStatementNode }
                ?.value
                ?: fail("call stmt env not found")
        assertEquals(EmptyNode, printEnv["x"])
        assertEquals(EmptyNode, printEnv["y"])
    }

    private fun buildEnv(program: String): Map<AstNode, Map<String, AstNode>> {
        val ast = parse(program)
        val main = ast.children[0] as FunctionNode
        return AssignmentResolver().resolveAssignments(main)
    }
}