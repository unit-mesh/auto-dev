package cc.unitmesh.devti.observer.plan

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanReviewActionTest {
    @Test
    fun `test removeAllMarkdownCode with empty content`() {
        val result = removeAllMarkdownCode("")
        assertEquals("", result)
    }

    @Test
    fun `test removeAllMarkdownCode with no code blocks`() {
        val content = """
            # Title
            This is a paragraph.
            * List item
        """.trimIndent()

        val result = removeAllMarkdownCode(content)
        assertEquals(content, result)
    }

    @Test
    fun `test removeAllMarkdownCode with one code fence`() {
        val content = """
            # Title
            This is a paragraph.
            
            ```kotlin
            val x = 10
            ```
            
            More text.
        """.trimIndent()

        val expected = """
            # Title
            This is a paragraph.
            
            ```kotlin
            // you can skip this part of the code.
            ```
            
            More text.
        """.trimIndent()

        val result = removeAllMarkdownCode(content)
        assertEquals(expected, result)
    }

    @Test
    fun `test removeAllMarkdownCode with multiple code blocks`() {
        val content = """
            # Title
            
            ```kotlin
            val x = 10
            ```
            
            Some text.
            
            ```java
            int y = 20;
            ```
            
            More text.
        """.trimIndent()

        val expected = """
            # Title
            
            ```kotlin
            // you can skip this part of the code.
            ```
            
            Some text.
            
            ```java
            // you can skip this part of the code.
            ```
            
            More text.
        """.trimIndent()

        val result = removeAllMarkdownCode(content)
        assertEquals(expected, result)
    }

    @Test
    fun `test removeAllMarkdownCode with indented code block`() {
        val content = """
            # Title
            
                This is a code block
                More code
            
            Text after.
        """.trimIndent()

        val expected = """
            # Title
            
            ```
            // you can skip this part of the code.
            ```
            
            Text after.
        """.trimIndent()

        val result = removeAllMarkdownCode(content)
        assertEquals(expected, result)
    }
}

