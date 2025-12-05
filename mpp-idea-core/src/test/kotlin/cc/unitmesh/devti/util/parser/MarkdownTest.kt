package cc.unitmesh.devti.util.parser

import org.assertj.core.api.Assertions.assertThat
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownHelperTest {
    @Test
    fun should_return_original_string_when_markdown_contains_no_code_block() {
        // Given
        val markdown = "This is a simple markdown text without any code blocks."

        // When
        val result = MarkdownCodeHelper.parseCodeFromString(markdown)

        // Then
        assertThat(result).containsExactly(markdown)
    }

    @Test
    fun should_remove_all_markdown_code_blocks_and_replace_with_placeholder() {
        // Given
        val markdown = """
            Here is some text with a code block:
            ```kotlin
            fun main() {}
            ```
        """.trimIndent()

        // When
        val result = MarkdownCodeHelper.removeAllMarkdownCode(markdown)

        // Then
        val expected = """
            Here is some text with a code block:
            ```kotlin
            // you can skip this part of the code.
            ```
        """.trimIndent()
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun should_return_original_string_when_markdown_is_empty() {
        // Given
        val markdown = ""

        // When
        val result = MarkdownCodeHelper.removeAllMarkdownCode(markdown)

        // Then
        assertThat(result).isEqualTo(markdown)
    }

    @Test
    fun should_extract_code_fence_language_when_language_is_specified() {
        // Given
        val markdown = "```kotlin\nfun main() {}\n```"
        val flavour = GFMFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)
        val codeFenceNode = parsedTree.children.first { it.type == MarkdownElementTypes.CODE_FENCE }

        // When
        val result = MarkdownCodeHelper.extractCodeFenceLanguage(codeFenceNode, markdown)

        // Then
        assertThat(result).isEqualTo("kotlin")
    }

    @Test
    fun should_return_empty_string_when_code_fence_language_is_not_specified() {
        // Given
        val markdown = "```\nfun main() {}\n```"
        val flavour = GFMFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)
        val codeFenceNode = parsedTree.children.first { it.type == MarkdownElementTypes.CODE_FENCE }

        // When
        val result = MarkdownCodeHelper.extractCodeFenceLanguage(codeFenceNode, markdown)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `test removeAllMarkdownCode with empty content`() {
        val result = MarkdownCodeHelper.removeAllMarkdownCode("")
        assertEquals("", result)
    }

    @Test
    fun `test removeAllMarkdownCode with no code blocks`() {
        val content = """
            # Title
            This is a paragraph.
            * List item
        """.trimIndent()

        val result = MarkdownCodeHelper.removeAllMarkdownCode(content)
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

        val result = MarkdownCodeHelper.removeAllMarkdownCode(content)
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

        val result = MarkdownCodeHelper.removeAllMarkdownCode(content)
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

        val result = MarkdownCodeHelper.removeAllMarkdownCode(content)
        assertEquals(expected, result)
    }
}
