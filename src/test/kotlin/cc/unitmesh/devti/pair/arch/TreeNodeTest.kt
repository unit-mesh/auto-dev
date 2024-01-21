package cc.unitmesh.devti.pair.arch;

import org.junit.Assert.assertEquals
import org.junit.Test

class TreeNodeTest {

    @Test
    fun shouldPrintSingleNode() {
        // given
        val node = TreeNode("A")

        // when
        val result = node.print()

        // then
        assertEquals("├── A\n", result)
    }

    @Test
    fun shouldPrintTreeWithChildren() {
        // given
        val nodeA = TreeNode("A")
        val nodeB = TreeNode("B")
        val nodeC = TreeNode("C")
        val nodeD = TreeNode("D")
        nodeA.addChild(nodeB)
        nodeA.addChild(nodeC)
        nodeC.addChild(nodeD)

        // when
        val result = nodeA.print()

        // then
        val expected = """
            ├── A
            │   ├── B
            │   └── C
            │       └── D

            """.trimIndent()
        assertEquals(expected, result)
    }

    @Test
    fun shouldPrintTreeWithMultipleLevels() {
        // given
        val nodeA = TreeNode("A")
        val nodeB = TreeNode("B")
        val nodeC = TreeNode("C")
        val nodeD = TreeNode("D")
        val nodeE = TreeNode("E")
        val nodeF = TreeNode("F")
        nodeA.addChild(nodeB)
        nodeA.addChild(nodeC)
        nodeB.addChild(nodeD)
        nodeB.addChild(nodeE)
        nodeC.addChild(nodeF)

        // when
        val result = nodeA.print()

        // then
        val expected = """
        ├── A
        │   ├── B
        │   │   ├── D
        │   │   └── E
        │   └── C
        │       └── F

        """.trimIndent()
        assertEquals(expected, result)
    }
}
