package cc.unitmesh.devins.ui.compose.sketch

import cc.unitmesh.agent.parser.ToolCallParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DevInBlockRendererTest {
    private val parser = ToolCallParser()

    @Test
    fun `should parse simple read-file tool call from devin block`() {
        val devinContent = """
            /read-file
            ```json
            {
              "path": "mpp-viewer-web/src/commonMain/kotlin/cc/unitmesh/viewer/web/MermaidRenderer.kt"
            }
            ```
        """.trimIndent()

        val wrappedContent = "<devin>\n$devinContent\n</devin>"
        val toolCalls = parser.parseToolCalls(wrappedContent)

        assertEquals(1, toolCalls.size)
        assertEquals("read-file", toolCalls[0].toolName)
        assertEquals("mpp-viewer-web/src/commonMain/kotlin/cc/unitmesh/viewer/web/MermaidRenderer.kt", 
            toolCalls[0].params["path"])
    }

    @Test
    fun `should parse read-file with key-value parameter`() {
        val devinContent = """
            /read-file path="config.yaml"
        """.trimIndent()

        val wrappedContent = "<devin>\n$devinContent\n</devin>"
        val toolCalls = parser.parseToolCalls(wrappedContent)

        assertEquals(1, toolCalls.size)
        assertEquals("read-file", toolCalls[0].toolName)
        assertEquals("config.yaml", toolCalls[0].params["path"])
    }

    @Test
    fun `should parse write-file with JSON parameters`() {
        val devinContent = """
            /write-file
            ```json
            {
              "path": "test.txt",
              "content": "Hello, World!"
            }
            ```
        """.trimIndent()

        val wrappedContent = "<devin>\n$devinContent\n</devin>"
        val toolCalls = parser.parseToolCalls(wrappedContent)

        assertEquals(1, toolCalls.size)
        assertEquals("write-file", toolCalls[0].toolName)
        assertEquals("test.txt", toolCalls[0].params["path"])
        assertEquals("Hello, World!", toolCalls[0].params["content"])
    }

    @Test
    fun `should parse shell command`() {
        val devinContent = """
            /shell command="./gradlew build"
        """.trimIndent()

        val wrappedContent = "<devin>\n$devinContent\n</devin>"
        val toolCalls = parser.parseToolCalls(wrappedContent)

        assertEquals(1, toolCalls.size)
        assertEquals("shell", toolCalls[0].toolName)
        assertEquals("./gradlew build", toolCalls[0].params["command"])
    }

    @Test
    fun `should parse grep with multiple parameters`() {
        val devinContent = """
            /grep pattern="ToolCall" path="mpp-core/"
        """.trimIndent()

        val wrappedContent = "<devin>\n$devinContent\n</devin>"
        val toolCalls = parser.parseToolCalls(wrappedContent)

        assertEquals(1, toolCalls.size)
        assertEquals("grep", toolCalls[0].toolName)
        assertEquals("ToolCall", toolCalls[0].params["pattern"])
        assertEquals("mpp-core/", toolCalls[0].params["path"])
    }

    @Test
    fun `should return empty list for empty devin block`() {
        val devinContent = ""

        val wrappedContent = "<devin>\n$devinContent\n</devin>"
        val toolCalls = parser.parseToolCalls(wrappedContent)

        assertTrue(toolCalls.isEmpty())
    }

    @Test
    fun `should return empty list for devin block without tool calls`() {
        val devinContent = """
            This is just some text
            without any tool calls
        """.trimIndent()

        val wrappedContent = "<devin>\n$devinContent\n</devin>"
        val toolCalls = parser.parseToolCalls(wrappedContent)

        assertTrue(toolCalls.isEmpty())
    }
}

