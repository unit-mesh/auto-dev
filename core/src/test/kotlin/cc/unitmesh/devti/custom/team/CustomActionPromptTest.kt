package cc.unitmesh.devti.custom.team;

import cc.unitmesh.cf.core.llms.LlmMsg
import junit.framework.TestCase.assertEquals
import org.junit.Test

class CustomActionPromptTest {

    @Test
    fun `should create TeamActionPrompt object from content with frontmatter`() {
        // given
        val content = """
            ---
            interaction: AppendCursorStream
            priority: 1
            key1: value1
            key2: value2
            ---
            ```system```
            Chat message 1
            ```user```
            Chat message 2
        """.trimIndent()

        // when
        val prompt = CustomActionPrompt.fromContent(content)

        // then
        assertEquals(InteractionType.AppendCursorStream, prompt.interaction)
        assertEquals(1, prompt.priority)
        assertEquals(mapOf("key1" to "value1", "key2" to "value2"), prompt.other)
        assertEquals(2, prompt.msgs.size)
        assertEquals(LlmMsg.ChatMessage(LlmMsg.ChatRole.System, "Chat message 1\n", null), prompt.msgs[0])
    }

    @Test
    fun `should create TeamActionPrompt object from content without frontmatter`() {
        // given
        val content = """
            ```system```
            Chat message 1
            ```user```
            Chat message 2
        """.trimIndent()

        // when
        val prompt = CustomActionPrompt.fromContent(content)

        // then
        assertEquals(InteractionType.AppendCursorStream, prompt.interaction)
        assertEquals(0, prompt.priority)
        assertEquals(emptyMap<String, Any>(), prompt.other)
        assertEquals(2, prompt.msgs.size)
        assertEquals(LlmMsg.ChatMessage(LlmMsg.ChatRole.System, "Chat message 1\n", null), prompt.msgs[0])
        assertEquals(LlmMsg.ChatMessage(LlmMsg.ChatRole.User, "Chat message 2\n", null), prompt.msgs[1])
    }

    @Test
    fun `should_handle_for_notion_like_summarize`() {
        val content = """
            ---
            type: QuickAction
            name: Summarize
            category: Generate
            interaction: AppendCursorStream
            ---

            ```System```

            You are an assistant helping summarize a document. Use this format, replacing text in brackets with the result. Do not include the brackets in the output: 

            Summary in [Identified language of the document]: 

            [One-paragaph summary of the document using the identified language.].

            ```User```
        """.trimIndent()

        val prompt = CustomActionPrompt.fromContent(content)
        assertEquals(prompt.type, CustomActionType.QuickAction)
    }
}
