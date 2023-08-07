package cc.unitmesh.devti.gui.chat.block

import com.intellij.temporary.gui.block.BorderType
import com.intellij.temporary.gui.block.MessageBlockType
import com.intellij.temporary.gui.block.MessageCodeBlockCharProcessor
import com.intellij.temporary.gui.block.Parameters
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
        val currentContextType = MessageBlockType.PlainText
        val blockStart = 0

        // when
        val contextChange = messageCodeBlockCharProcessor.suggestTypeChange(parameters, currentContextType, blockStart)

        // then
        assert(contextChange != null)
        assert(contextChange!!.contextType == MessageBlockType.CodeEditor)
        assert(contextChange.borderType == BorderType.START)
    }

    @Test
    fun should_return_text_when_is_not_a_markdown_code() {
        val messageCodeBlockCharProcessor = MessageCodeBlockCharProcessor()
        val parameters = Parameters('#', 0, """## Hello World!
            | some text
            | some text
        """.trimMargin())
        val currentContextType = MessageBlockType.CodeEditor
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
        val currentContextType = MessageBlockType.PlainText
        val blockStart = 0

        // when
        val contextChange = messageCodeBlockCharProcessor.suggestTypeChange(parameters, currentContextType, blockStart)

        // then
        assert(contextChange != null)
        assert(contextChange!!.contextType == MessageBlockType.CodeEditor)
        assert(contextChange.borderType == BorderType.START)
    }
}