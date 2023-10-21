package cc.unitmesh.devti.custom.team;

import cc.unitmesh.cf.core.llms.LlmMsg
import io.kotest.matchers.shouldBe
import junit.framework.TestCase.assertEquals
import org.junit.Test

class TeamActionPromptTest {

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
        val prompt = TeamActionPrompt.fromContent(content)

        // then
        assertEquals(InteractionType.AppendCursorStream, prompt.interaction)
        assertEquals(1, prompt.priority)
        assertEquals(mapOf("key1" to "value1", "key2" to "value2"), prompt.other)
        prompt.msgs shouldBe listOf(
            LlmMsg.ChatMessage(LlmMsg.ChatRole.System, "Chat message 1\n", null),
            LlmMsg.ChatMessage(LlmMsg.ChatRole.User, "Chat message 2\n", null),
        )
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
        val prompt = TeamActionPrompt.fromContent(content)

        // then
        assertEquals(InteractionType.AppendCursorStream, prompt.interaction)
        assertEquals(0, prompt.priority)
        assertEquals(emptyMap<String, Any>(), prompt.other)
        prompt.msgs shouldBe listOf(
            LlmMsg.ChatMessage(LlmMsg.ChatRole.System, "Chat message 1\n", null),
            LlmMsg.ChatMessage(LlmMsg.ChatRole.User, "Chat message 2\n", null),
        )
    }
}
