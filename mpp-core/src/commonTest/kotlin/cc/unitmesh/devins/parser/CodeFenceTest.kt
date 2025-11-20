package cc.unitmesh.devins.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodeFenceTest {

    @Test
    fun testParseSimpleMarkdown() {
        val content = "This is plain text"
        val result = CodeFence.parseAll(content)

        assertEquals(1, result.size)
        assertEquals("markdown", result[0].languageId)
        assertEquals(content, result[0].text)
        assertTrue(result[0].isComplete)
    }

    @Test
    fun testParseCodeBlock() {
        val content = """
            ```kotlin
            fun main() {
                println("Hello")
            }
            ```
        """.trimIndent()

        val result = CodeFence.parseAll(content)

        assertEquals(1, result.size)
        assertEquals("kotlin", result[0].languageId)
        assertTrue(result[0].text.contains("fun main()"))
        assertTrue(result[0].isComplete)
    }

    @Test
    fun testParseDevinBlock() {
        val content = """
            <devin>
            /file:test.kt
            </devin>
        """.trimIndent()

        val result = CodeFence.parseAll(content)

        assertEquals(1, result.size)
        assertEquals("devin", result[0].languageId)
        assertTrue(result[0].text.contains("/file:test.kt"))
        assertTrue(result[0].isComplete)
    }

    @Test
    fun testParseThinkingBlock() {
        val content = """
            <thinking>
            This is my reasoning process
            </thinking>
        """.trimIndent()

        val result = CodeFence.parseAll(content)

        assertEquals(1, result.size)
        assertEquals("thinking", result[0].languageId)
        assertEquals("This is my reasoning process", result[0].text)
        assertTrue(result[0].isComplete)
    }

    @Test
    fun testParseIncompleteThinkingBlock() {
        val content = """
            <thinking>
            This is incomplete reasoning
        """.trimIndent()

        val result = CodeFence.parseAll(content)

        assertEquals(1, result.size)
        assertEquals("thinking", result[0].languageId)
        assertEquals("This is incomplete reasoning", result[0].text)
        assertFalse(result[0].isComplete)
    }

    @Test
    fun testParseMixedContentWithThinking() {
        val content = """
            Here is some analysis:
            
            <thinking>
            Let me think about this problem step by step:
            1. First consideration
            2. Second consideration
            </thinking>
            
            Based on my analysis, here's the solution:
            
            ```kotlin
            fun solution() {
                // implementation
            }
            ```
        """.trimIndent()

        val result = CodeFence.parseAll(content)

        // Should have 3 blocks: markdown, thinking, markdown, code
        assertTrue(result.size >= 3, "Expected at least 3 blocks, got ${result.size}")
        
        // Find the thinking block
        val thinkingBlock = result.find { it.languageId == "thinking" }
        assertTrue(thinkingBlock != null, "Should have a thinking block")
        assertTrue(thinkingBlock!!.text.contains("step by step"))
        assertTrue(thinkingBlock.isComplete)

        // Find the code block
        val codeBlock = result.find { it.languageId == "kotlin" }
        assertTrue(codeBlock != null, "Should have a kotlin code block")
        assertTrue(codeBlock!!.text.contains("fun solution()"))
    }

    @Test
    fun testParseMultipleThinkingBlocks() {
        val content = """
            <thinking>
            First thought
            </thinking>
            
            Some text in between
            
            <thinking>
            Second thought
            </thinking>
        """.trimIndent()

        val result = CodeFence.parseAll(content)

        val thinkingBlocks = result.filter { it.languageId == "thinking" }
        assertEquals(2, thinkingBlocks.size, "Should have 2 thinking blocks")
        assertEquals("First thought", thinkingBlocks[0].text)
        assertEquals("Second thought", thinkingBlocks[1].text)
        assertTrue(thinkingBlocks.all { it.isComplete })
    }

    @Test
    fun testParseThinkingAndDevinBlocks() {
        val content = """
            <thinking>
            I need to analyze this
            </thinking>
            
            <devin>
            /file:test.kt
            </devin>
        """.trimIndent()

        val result = CodeFence.parseAll(content)

        assertEquals(2, result.size)
        assertEquals("thinking", result[0].languageId)
        assertEquals("devin", result[1].languageId)
        assertTrue(result.all { it.isComplete })
    }

    @Test
    fun testParseThinkingWithMarkdownContent() {
        val content = """
            <thinking>
            ## Analysis
            
            - Point 1
            - Point 2
            
            **Conclusion**: This is important
            </thinking>
        """.trimIndent()

        val result = CodeFence.parseAll(content)

        assertEquals(1, result.size)
        assertEquals("thinking", result[0].languageId)
        assertTrue(result[0].text.contains("## Analysis"))
        assertTrue(result[0].text.contains("**Conclusion**"))
        assertTrue(result[0].isComplete)
    }

    @Test
    fun testParseEmptyThinkingBlock() {
        val content = """
            <thinking>
            </thinking>
        """.trimIndent()

        val result = CodeFence.parseAll(content)

        // Empty blocks might be filtered out or kept as empty
        val thinkingBlock = result.find { it.languageId == "thinking" }
        if (thinkingBlock != null) {
            assertTrue(thinkingBlock.text.isEmpty() || thinkingBlock.text.isBlank())
            assertTrue(thinkingBlock.isComplete)
        }
    }

    @Test
    fun testParseSingleThinkingTag() {
        val content = "<thinking>"
        
        val fence = CodeFence.parse(content)
        
        assertEquals("thinking", fence.languageId)
        assertTrue(fence.text.isEmpty())
        assertFalse(fence.isComplete)
    }

    @Test
    fun testParseCompleteThinkingTag() {
        val content = "<thinking>reasoning</thinking>"
        
        val fence = CodeFence.parse(content)
        
        assertEquals("thinking", fence.languageId)
        assertEquals("reasoning", fence.text)
        assertTrue(fence.isComplete)
    }

    @Test
    fun testParseThinkingBeforeOtherContent() {
        val content = """
            <thinking>
            Initial analysis
            </thinking>
            
            Now let's implement:
            
            ```python
            def hello():
                print("world")
            ```
        """.trimIndent()

        val result = CodeFence.parseAll(content)

        assertTrue(result.isNotEmpty())
        assertEquals("thinking", result[0].languageId)
        assertEquals("Initial analysis", result[0].text)
        
        val codeBlock = result.find { it.languageId == "python" }
        assertTrue(codeBlock != null)
    }
}

