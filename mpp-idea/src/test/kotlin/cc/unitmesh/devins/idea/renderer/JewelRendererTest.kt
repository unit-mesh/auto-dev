package cc.unitmesh.devins.idea.renderer

import cc.unitmesh.agent.render.TaskStatus
import cc.unitmesh.llm.compression.TokenInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for JewelRenderer.
 *
 * Tests the renderer's functionality including:
 * - Timeline management
 * - Message rendering
 * - Tool call handling
 * - Error handling
 * - Task completion
 *
 * These tests do not require IntelliJ Platform and can be run standalone.
 */
class JewelRendererTest {

    private lateinit var renderer: JewelRenderer

    @BeforeEach
    fun setUp() {
        renderer = JewelRenderer()
    }

    @Test
    fun testInitialState() = runBlocking {
        val timeline = renderer.timeline.first()
        assertTrue(timeline.isEmpty())

        val streamingOutput = renderer.currentStreamingOutput.first()
        assertTrue(streamingOutput.isEmpty())

        val isProcessing = renderer.isProcessing.first()
        assertFalse(isProcessing)
    }

    @Test
    fun testAddUserMessage() = runBlocking {
        renderer.addUserMessage("Hello, world!")

        val timeline = renderer.timeline.first()
        assertEquals(1, timeline.size)

        val item = timeline.first()
        assertTrue(item is JewelRenderer.TimelineItem.MessageItem)
        assertEquals(JewelRenderer.MessageRole.USER, (item as JewelRenderer.TimelineItem.MessageItem).role)
        assertEquals("Hello, world!", item.content)
    }

    @Test
    fun testLLMResponseStreaming() = runBlocking {
        renderer.renderLLMResponseStart()

        val isProcessing = renderer.isProcessing.first()
        assertTrue(isProcessing)

        renderer.renderLLMResponseChunk("Hello ")
        renderer.renderLLMResponseChunk("world!")

        val streamingOutput = renderer.currentStreamingOutput.first()
        assertTrue(streamingOutput.contains("Hello"))
        assertTrue(streamingOutput.contains("world"))

        renderer.renderLLMResponseEnd()

        val finalProcessing = renderer.isProcessing.first()
        assertFalse(finalProcessing)

        val timeline = renderer.timeline.first()
        assertTrue(timeline.isNotEmpty())
    }

    @Test
    fun testToolCallRendering() = runBlocking {
        renderer.renderToolCall("read-file", "path=\"/test/file.txt\"")

        val currentToolCall = renderer.currentToolCall.first()
        assertNotNull(currentToolCall)
        assertTrue(currentToolCall!!.toolName.contains("file.txt"))

        val timeline = renderer.timeline.first()
        assertEquals(1, timeline.size)
        assertTrue(timeline.first() is JewelRenderer.TimelineItem.ToolCallItem)
    }

    @Test
    fun testToolResultRendering() = runBlocking {
        renderer.renderToolCall("read-file", "path=\"/test/file.txt\"")
        renderer.renderToolResult(
            toolName = "read-file",
            success = true,
            output = "File content here",
            fullOutput = "Full file content here with more details",
            metadata = mapOf("execution_time_ms" to "100")
        )

        val currentToolCall = renderer.currentToolCall.first()
        assertNull(currentToolCall)

        val timeline = renderer.timeline.first()
        assertEquals(1, timeline.size)

        val toolItem = timeline.first() as JewelRenderer.TimelineItem.ToolCallItem
        assertTrue(toolItem.success == true)
        assertNotNull(toolItem.output)
    }

    @Test
    fun testErrorRendering() = runBlocking {
        renderer.renderError("Something went wrong!")

        val errorMessage = renderer.errorMessage.first()
        assertEquals("Something went wrong!", errorMessage)

        val timeline = renderer.timeline.first()
        assertEquals(1, timeline.size)
        assertTrue(timeline.first() is JewelRenderer.TimelineItem.ErrorItem)
    }

    @Test
    fun testTaskCompletion() = runBlocking {
        renderer.renderFinalResult(true, "Task completed successfully", 5)

        val timeline = renderer.timeline.first()
        assertEquals(1, timeline.size)

        val item = timeline.first() as JewelRenderer.TimelineItem.TaskCompleteItem
        assertTrue(item.success)
        assertEquals("Task completed successfully", item.message)
        assertEquals(5, item.iterations)
    }

    @Test
    fun testClearTimeline() = runBlocking {
        renderer.addUserMessage("Message 1")
        renderer.addUserMessage("Message 2")
        renderer.renderError("An error")

        var timeline = renderer.timeline.first()
        assertEquals(3, timeline.size)

        renderer.clearTimeline()

        timeline = renderer.timeline.first()
        assertTrue(timeline.isEmpty())

        val errorMessage = renderer.errorMessage.first()
        assertNull(errorMessage)
    }

    @Test
    fun testIterationTracking() = runBlocking {
        renderer.renderIterationHeader(3, 10)

        val currentIteration = renderer.currentIteration.first()
        assertEquals(3, currentIteration)

        val maxIterations = renderer.maxIterations.first()
        assertEquals(10, maxIterations)
    }

    @Test
    fun testTokenInfoUpdate() = runBlocking {
        val tokenInfo = TokenInfo(
            totalTokens = 100,
            inputTokens = 60,
            outputTokens = 40
        )
        renderer.updateTokenInfo(tokenInfo)

        val totalTokenInfo = renderer.totalTokenInfo.first()
        assertEquals(100, totalTokenInfo.totalTokens)
        assertEquals(60, totalTokenInfo.inputTokens)
        assertEquals(40, totalTokenInfo.outputTokens)
    }

    @Test
    fun testForceStop() = runBlocking {
        renderer.renderLLMResponseStart()
        renderer.renderLLMResponseChunk("Partial output")

        assertTrue(renderer.isProcessing.first())

        renderer.forceStop()

        assertFalse(renderer.isProcessing.first())

        // Verify the interrupted message was added
        val timeline = renderer.timeline.first()
        assertTrue(timeline.isNotEmpty())
        val lastItem = timeline.last()
        assertTrue(lastItem is JewelRenderer.TimelineItem.MessageItem)
        assertTrue((lastItem as JewelRenderer.TimelineItem.MessageItem).content.contains("[Interrupted]"))
    }

    @Test
    fun testTaskBoundaryToolUpdatesTaskList() = runBlocking {
        renderer.renderToolCall("task-boundary", "taskName=\"Build\" status=\"WORKING\" summary=\"Building project\"")

        val tasks = renderer.tasks.first()
        assertEquals(1, tasks.size)
        assertEquals("Build", tasks.first().taskName)
        assertEquals(TaskStatus.WORKING, tasks.first().status)
    }
}

