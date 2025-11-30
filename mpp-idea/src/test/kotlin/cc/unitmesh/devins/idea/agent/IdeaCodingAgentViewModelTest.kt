package cc.unitmesh.devins.idea.agent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for IdeaCodingAgentViewModel.
 * Note: These tests are limited since we don't have a mock Project instance.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IdeaCodingAgentViewModelTest {

    @Test
    fun `IdeaAgentRenderer should add user message correctly`() {
        val renderer = IdeaAgentRenderer()
        
        renderer.addUserMessage("Hello, World!")
        
        assertEquals(1, renderer.timeline.size)
        val item = renderer.timeline[0] as TimelineItem.MessageItem
        assertTrue(item.isUser)
        assertEquals("Hello, World!", item.content)
    }

    @Test
    fun `IdeaAgentRenderer should add assistant message correctly`() {
        val renderer = IdeaAgentRenderer()
        
        renderer.addAssistantMessage("I'm here to help!")
        
        assertEquals(1, renderer.timeline.size)
        val item = renderer.timeline[0] as TimelineItem.MessageItem
        assertFalse(item.isUser)
        assertEquals("I'm here to help!", item.content)
    }

    @Test
    fun `IdeaAgentRenderer should handle streaming correctly`() {
        val renderer = IdeaAgentRenderer()
        
        renderer.startStreaming()
        assertTrue(renderer.isProcessing)
        assertEquals("", renderer.currentStreamingOutput)
        
        renderer.appendStreamingContent("Hello ")
        renderer.appendStreamingContent("World!")
        assertEquals("Hello World!", renderer.currentStreamingOutput)
        
        renderer.endStreaming()
        assertFalse(renderer.isProcessing)
        assertEquals("", renderer.currentStreamingOutput)
        assertEquals(1, renderer.timeline.size)
    }

    @Test
    fun `IdeaAgentRenderer should handle tool calls correctly`() {
        val renderer = IdeaAgentRenderer()
        
        renderer.startToolCall("ReadFile", "Reading configuration", "config.json")
        
        assertEquals(1, renderer.timeline.size)
        val item = renderer.timeline[0] as TimelineItem.ToolCallItem
        assertEquals("ReadFile", item.toolName)
        assertEquals(null, item.success) // Still executing
        
        renderer.completeToolCall(true, "Read 100 bytes", "file content...", 50)
        
        val updatedItem = renderer.timeline[0] as TimelineItem.ToolCallItem
        assertTrue(updatedItem.success == true)
        assertEquals("Read 100 bytes", updatedItem.summary)
    }

    @Test
    fun `IdeaAgentRenderer should handle errors correctly`() {
        val renderer = IdeaAgentRenderer()
        
        renderer.addError("Something went wrong")
        
        assertEquals("Something went wrong", renderer.errorMessage)
        assertEquals(1, renderer.timeline.size)
        val item = renderer.timeline[0] as TimelineItem.ErrorItem
        assertEquals("Something went wrong", item.error)
        
        renderer.clearError()
        assertEquals(null, renderer.errorMessage)
    }

    @Test
    fun `IdeaAgentRenderer should clear all state`() {
        val renderer = IdeaAgentRenderer()
        
        renderer.addUserMessage("Test")
        renderer.startStreaming()
        renderer.appendStreamingContent("Streaming...")
        renderer.addError("Error")
        
        renderer.clear()
        
        assertTrue(renderer.timeline.isEmpty())
        assertEquals("", renderer.currentStreamingOutput)
        assertFalse(renderer.isProcessing)
        assertEquals(null, renderer.errorMessage)
    }

    @Test
    fun `IdeaAgentRenderer should add task complete item`() {
        val renderer = IdeaAgentRenderer()
        
        renderer.addTaskComplete(true, "Task completed successfully in 3 iterations")
        
        assertEquals(1, renderer.timeline.size)
        val item = renderer.timeline[0] as TimelineItem.TaskCompleteItem
        assertTrue(item.success)
        assertEquals("Task completed successfully in 3 iterations", item.message)
        assertFalse(renderer.isProcessing)
    }

    @Test
    fun `IdeaAgentRenderer should force stop correctly`() {
        val renderer = IdeaAgentRenderer()
        
        renderer.startStreaming()
        renderer.appendStreamingContent("In progress...")
        renderer.startToolCall("SomeTask", "Doing something", null)
        
        renderer.forceStop()
        
        assertEquals("", renderer.currentStreamingOutput)
        assertEquals(null, renderer.currentToolCall)
        assertFalse(renderer.isProcessing)
    }

    @Test
    fun `TokenInfo should calculate total correctly`() {
        val tokenInfo = TokenInfo(inputTokens = 100, outputTokens = 50)
        
        assertEquals(150, tokenInfo.totalTokens)
    }
}

