package cc.unitmesh.devti.gui.chat.block

import org.junit.Test

class MessageCodeBlockCharProcessorTest {
    @Test
    fun should_get_suggest_type_when_is_a_markdown_code() {
        val messageCodeBlockCharProcessor = MessageCodeBlockCharProcessor()
        val parameters = Parameters('`', 0, """```kotlin
            |fun main() {
            |    println("Hello World!")
            |}
            |```""".trimMargin())
        val currentContextType = BlockType.PlainText
        val blockStart = 0

        // when
        val contextChange = messageCodeBlockCharProcessor.suggestTypeChange(parameters, currentContextType, blockStart)

        // then
        assert(contextChange != null)
        assert(contextChange!!.contextType == BlockType.CodeEditor)
        assert(contextChange.borderType == BorderType.START)
    }

    @Test
    fun should_return_text_when_is_not_a_markdown_code() {
        val messageCodeBlockCharProcessor = MessageCodeBlockCharProcessor()
        val parameters = Parameters('#', 0, """## Hello World!
            | some text
            | some text
        """.trimMargin())
        val currentContextType = BlockType.CodeEditor
        val blockStart = 0

        // when
        val contextChange = messageCodeBlockCharProcessor.suggestTypeChange(parameters, currentContextType, blockStart)

        // then
        assert(contextChange == null)
    }

    @Test
    fun should_handle_code_block_when_code_not_complete() {
        val messageCodeBlockCharProcessor = MessageCodeBlockCharProcessor()
        val parameters = Parameters('`', 0, """```kotlin
            |fun main() {
            |    println("Hello World!")
            |}
            |""".trimMargin())
        val currentContextType = BlockType.PlainText
        val blockStart = 0

        // when
        val contextChange = messageCodeBlockCharProcessor.suggestTypeChange(parameters, currentContextType, blockStart)

        // then
        assert(contextChange != null)
        assert(contextChange!!.contextType == BlockType.CodeEditor)
        assert(contextChange.borderType == BorderType.START)
    }
}