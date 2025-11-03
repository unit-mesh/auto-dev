package cc.unitmesh.devins.ui.compose.editor.highlighting

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * DevInSyntaxHighlighter 单元测试
 */
class DevInSyntaxHighlighterTest {
    private val highlighter = DevInSyntaxHighlighter()

    @Test
    fun `should highlight agent syntax`() {
        val text = "@clarify What is this?"
        val result = highlighter.highlight(text)

        assertTrue(result.text == text, "Text should match")
        assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles")
    }

    @Test
    fun `should highlight command syntax`() {
        val text = "/file:src/main/kotlin/Main.kt"
        val result = highlighter.highlight(text)

        assertTrue(result.text == text, "Text should match")
        assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles")
    }

    @Test
    fun `should highlight variable syntax`() {
        val text = "Value: \$input"
        val result = highlighter.highlight(text)

        assertTrue(result.text == text, "Text should match")
        assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles")
    }

    @Test
    fun `should highlight frontmatter`() {
        val text =
            """
            ---
            name: "Test"
            value: 123
            ---
            """.trimIndent()

        val result = highlighter.highlight(text)

        assertTrue(result.text == text, "Text should match")
        assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles")
    }

    @Test
    fun `should highlight code block`() {
        val text =
            """
            ```kotlin
            fun main() {
                println("Hello")
            }
            ```
            """.trimIndent()

        val result = highlighter.highlight(text)

        assertTrue(result.text == text, "Text should match")
        assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles")
    }

    @Test
    fun `should handle empty text`() {
        val text = ""
        val result = highlighter.highlight(text)

        assertTrue(result.text == text, "Text should match")
        assertTrue(result.spanStyles.isEmpty(), "Should have no span styles")
    }

    @Test
    fun `should handle invalid syntax gracefully`() {
        val text = "@@@///\$\$\$"
        val result = highlighter.highlight(text)

        // Should not throw exception and return the text
        assertTrue(result.text.isNotEmpty(), "Should return some text")
    }

    @Test
    fun `should highlight complex mixed content`() {
        val text =
            """
            ---
            name: "Complex Test"
            ---
            
            # Header
            
            @clarify Please review /file:test.kt
            
            The variable is ${"$"}input and output is ${"$"}output.
            
            ```kotlin
            fun test() {
                // comment
                val x = 123
            }
            ```
            """.trimIndent()

        val result = highlighter.highlight(text)

        assertTrue(result.text == text, "Text should match")
        assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles")
    }
}
