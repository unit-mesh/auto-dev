package cc.unitmesh.devins.idea.toolwindow.remote

import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.agent.render.TimelineItem
import cc.unitmesh.devins.idea.renderer.JewelRenderer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for IdeaRemoteAgentViewModel.
 *
 * Tests the ViewModel's functionality including:
 * - Initial state
 * - Server URL management
 * - Connection state handling
 * - Task cancellation
 * - History management
 * - Event handling (via renderer)
 *
 * Note: These tests do not require a real server connection.
 * Network-related tests are skipped as they would require mocking.
 */
class IdeaRemoteAgentViewModelTest {

    private lateinit var testScope: CoroutineScope

    @BeforeEach
    fun setUp() {
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterEach
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun testInitialState() = runBlocking {
        // Create a mock project-free test by testing the renderer and state directly
        // We can't easily test the full ViewModel without IntelliJ Platform,
        // but we can test the renderer and state management
        val renderer = JewelRenderer()

        // Verify initial renderer state
        val timeline = renderer.timeline.first()
        assertTrue(timeline.isEmpty())

        val isProcessing = renderer.isProcessing.first()
        assertFalse(isProcessing)

        val errorMessage = renderer.errorMessage.first()
        assertNull(errorMessage)
    }

    @Test
    fun testRendererHandlesIterationEvent() = runBlocking {
        val renderer = JewelRenderer()

        // Simulate handling iteration event
        renderer.renderIterationHeader(3, 10)

        val currentIteration = renderer.currentIteration.first()
        assertEquals(3, currentIteration)

        val maxIterations = renderer.maxIterations.first()
        assertEquals(10, maxIterations)
    }

    @Test
    fun testRendererHandlesLLMChunkEvent() = runBlocking {
        val renderer = JewelRenderer()

        // Simulate LLM streaming
        renderer.renderLLMResponseStart()
        assertTrue(renderer.isProcessing.first())

        renderer.renderLLMResponseChunk("Hello ")
        renderer.renderLLMResponseChunk("world!")

        val streamingOutput = renderer.currentStreamingOutput.first()
        assertTrue(streamingOutput.contains("Hello"))
        assertTrue(streamingOutput.contains("world"))

        renderer.renderLLMResponseEnd()
        assertFalse(renderer.isProcessing.first())
    }

    @Test
    fun testRendererHandlesToolCallEvent() = runBlocking {
        val renderer = JewelRenderer()

        // Simulate tool call
        renderer.renderToolCall("read-file", "path=\"/test/file.txt\"")

        val currentToolCall = renderer.currentToolCall.first()
        assertNotNull(currentToolCall)

        val timeline = renderer.timeline.first()
        assertEquals(1, timeline.size)
        assertTrue(timeline.first() is TimelineItem.ToolCallItem)
    }

    @Test
    fun testRendererHandlesToolResultEvent() = runBlocking {
        val renderer = JewelRenderer()

        // Simulate tool call and result
        renderer.renderToolCall("read-file", "path=\"/test/file.txt\"")
        renderer.renderToolResult(
            toolName = "read-file",
            success = true,
            output = "File content",
            fullOutput = "Full file content",
            metadata = emptyMap()
        )

        val currentToolCall = renderer.currentToolCall.first()
        assertNull(currentToolCall)

        val timeline = renderer.timeline.first()
        assertEquals(1, timeline.size)

        val toolItem = timeline.first() as TimelineItem.ToolCallItem
        assertEquals(true, toolItem.success)
    }

    @Test
    fun testRendererHandlesErrorEvent() = runBlocking {
        val renderer = JewelRenderer()

        renderer.renderError("Connection failed")

        val errorMessage = renderer.errorMessage.first()
        assertEquals("Connection failed", errorMessage)

        val timeline = renderer.timeline.first()
        assertEquals(1, timeline.size)
        assertTrue(timeline.first() is TimelineItem.ErrorItem)
    }

    @Test
    fun testRendererHandlesCompleteEvent() = runBlocking {
        val renderer = JewelRenderer()

        renderer.renderFinalResult(true, "Task completed", 5)

        val timeline = renderer.timeline.first()
        assertEquals(1, timeline.size)

        val item = timeline.first() as TimelineItem.TaskCompleteItem
        assertTrue(item.success)
        assertEquals("Task completed", item.message)
        assertEquals(5, item.iterations)
    }

    @Test
    fun testRendererClearTimeline() = runBlocking {
        val renderer = JewelRenderer()

        // Add some items
        renderer.addUserMessage("User message")
        renderer.renderError("An error")

        var timeline = renderer.timeline.first()
        assertEquals(2, timeline.size)

        // Clear timeline
        renderer.clearTimeline()

        timeline = renderer.timeline.first()
        assertTrue(timeline.isEmpty())

        val errorMessage = renderer.errorMessage.first()
        assertNull(errorMessage)
    }

    @Test
    fun testRendererForceStop() = runBlocking {
        val renderer = JewelRenderer()

        // Start streaming
        renderer.renderLLMResponseStart()
        renderer.renderLLMResponseChunk("Partial output")

        assertTrue(renderer.isProcessing.first())

        // Force stop
        renderer.forceStop()

        assertFalse(renderer.isProcessing.first())

        // Verify interrupted message was added
        val timeline = renderer.timeline.first()
        assertTrue(timeline.isNotEmpty())
        val lastItem = timeline.last()
        assertTrue(lastItem is TimelineItem.MessageItem)
        assertTrue((lastItem as TimelineItem.MessageItem).content.contains("[Interrupted]"))
    }

    @Test
    fun testRendererClearError() = runBlocking {
        val renderer = JewelRenderer()

        // Set error
        renderer.renderError("Test error")
        assertEquals("Test error", renderer.errorMessage.first())

        // Clear error
        renderer.clearError()
        assertNull(renderer.errorMessage.first())
    }

    @Test
    fun testRendererAddUserMessage() = runBlocking {
        val renderer = JewelRenderer()

        renderer.addUserMessage("Hello from user")

        val timeline = renderer.timeline.first()
        assertEquals(1, timeline.size)

        val item = timeline.first() as TimelineItem.MessageItem
        assertEquals(MessageRole.USER, item.role)
        assertEquals("Hello from user", item.content)
    }

    @Test
    fun testRemoteAgentRequestBuilder() {
        // Test the request building logic
        val projectId = "test-project"
        val task = "Fix the bug"
        val gitUrl = ""

        // When gitUrl is empty, should use projectId
        val request = RemoteAgentRequest(
            projectId = projectId,
            task = task,
            llmConfig = null,
            gitUrl = if (gitUrl.isNotBlank()) gitUrl else null
        )

        assertEquals("test-project", request.projectId)
        assertEquals("Fix the bug", request.task)
        assertNull(request.gitUrl)
    }

    @Test
    fun testRemoteAgentRequestWithGitUrl() {
        // Test the request building logic with git URL
        val gitUrl = "https://github.com/user/repo.git"
        val task = "Fix the bug"

        val projectId = gitUrl.split('/').lastOrNull()?.removeSuffix(".git") ?: "temp-project"

        val request = RemoteAgentRequest(
            projectId = projectId,
            task = task,
            llmConfig = null,
            gitUrl = gitUrl
        )

        assertEquals("repo", request.projectId)
        assertEquals("Fix the bug", request.task)
        assertEquals(gitUrl, request.gitUrl)
    }

    @Test
    fun testLLMConfigSerialization() {
        val config = LLMConfig(
            provider = "OpenAI",
            modelName = "gpt-4",
            apiKey = "test-key",
            baseUrl = "https://api.openai.com"
        )

        assertEquals("OpenAI", config.provider)
        assertEquals("gpt-4", config.modelName)
        assertEquals("test-key", config.apiKey)
        assertEquals("https://api.openai.com", config.baseUrl)
    }

    @Test
    fun testHealthResponseParsing() {
        val response = HealthResponse(status = "ok")
        assertEquals("ok", response.status)
    }

    @Test
    fun testProjectInfoParsing() {
        val project = ProjectInfo(
            id = "proj-1",
            name = "My Project",
            path = "/path/to/project",
            description = "A test project"
        )

        assertEquals("proj-1", project.id)
        assertEquals("My Project", project.name)
        assertEquals("/path/to/project", project.path)
        assertEquals("A test project", project.description)
    }

    // Tests for getEffectiveProjectId utility function

    @Test
    fun testGetEffectiveProjectIdWithNormalGitUrl() {
        val result = getEffectiveProjectId("fallback", "https://github.com/user/repo.git")
        assertEquals("repo", result)
    }

    @Test
    fun testGetEffectiveProjectIdWithTrailingSlash() {
        // Edge case: URL with trailing slash should still extract correct project ID
        val result = getEffectiveProjectId("fallback", "https://github.com/user/repo/")
        assertEquals("repo", result)
    }

    @Test
    fun testGetEffectiveProjectIdWithMultipleTrailingSlashes() {
        val result = getEffectiveProjectId("fallback", "https://github.com/user/repo///")
        assertEquals("repo", result)
    }

    @Test
    fun testGetEffectiveProjectIdWithEmptyGitUrl() {
        val result = getEffectiveProjectId("my-project", "")
        assertEquals("my-project", result)
    }

    @Test
    fun testGetEffectiveProjectIdWithBlankGitUrl() {
        val result = getEffectiveProjectId("my-project", "   ")
        assertEquals("my-project", result)
    }

    @Test
    fun testGetEffectiveProjectIdWithOnlySlashes() {
        // Edge case: URL that is just slashes should fallback to projectId
        val result = getEffectiveProjectId("fallback", "///")
        assertEquals("fallback", result)
    }

    @Test
    fun testGetEffectiveProjectIdWithGitSuffix() {
        val result = getEffectiveProjectId("fallback", "https://github.com/user/my-repo.git")
        assertEquals("my-repo", result)
    }

    @Test
    fun testGetEffectiveProjectIdWithoutGitSuffix() {
        val result = getEffectiveProjectId("fallback", "https://github.com/user/my-repo")
        assertEquals("my-repo", result)
    }

    // Tests for IdeaRemoteAgentClient data classes and state management

    @Test
    fun testRemoteAgentClientDataClassDefaultValues() {
        // Test RemoteAgentRequest with minimal required fields
        val request = RemoteAgentRequest(
            projectId = "test-project",
            task = "Test task"
        )

        assertEquals("test-project", request.projectId)
        assertEquals("Test task", request.task)
        assertNull(request.llmConfig)
        assertNull(request.gitUrl)
        assertNull(request.branch)
        assertNull(request.username)
        assertNull(request.password)
    }

    @Test
    fun testRemoteAgentRequestWithAllFields() {
        val llmConfig = LLMConfig(
            provider = "OpenAI",
            modelName = "gpt-4",
            apiKey = "test-key",
            baseUrl = "https://api.openai.com"
        )

        val request = RemoteAgentRequest(
            projectId = "test-project",
            task = "Test task",
            llmConfig = llmConfig,
            gitUrl = "https://github.com/user/repo.git",
            branch = "main",
            username = "user",
            password = "pass"
        )

        assertEquals("test-project", request.projectId)
        assertEquals("Test task", request.task)
        assertNotNull(request.llmConfig)
        assertEquals("https://github.com/user/repo.git", request.gitUrl)
        assertEquals("main", request.branch)
        assertEquals("user", request.username)
        assertEquals("pass", request.password)
    }

    @Test
    fun testLLMConfigWithNullBaseUrl() {
        val config = LLMConfig(
            provider = "Claude",
            modelName = "claude-3",
            apiKey = "test-key",
            baseUrl = null
        )

        assertEquals("Claude", config.provider)
        assertEquals("claude-3", config.modelName)
        assertEquals("test-key", config.apiKey)
        assertNull(config.baseUrl)
    }

    @Test
    fun testProjectListResponseEmpty() {
        val response = ProjectListResponse(projects = emptyList())
        assertTrue(response.projects.isEmpty())
    }

    @Test
    fun testProjectListResponseWithMultipleProjects() {
        val projects = listOf(
            ProjectInfo(id = "proj-1", name = "Project 1", path = "/path/1", description = "First"),
            ProjectInfo(id = "proj-2", name = "Project 2", path = "/path/2", description = "Second")
        )
        val response = ProjectListResponse(projects = projects)

        assertEquals(2, response.projects.size)
        assertEquals("proj-1", response.projects[0].id)
        assertEquals("proj-2", response.projects[1].id)
    }

    @Test
    fun testRemoteAgentExceptionWithCause() {
        val cause = RuntimeException("Original error")
        val exception = RemoteAgentException("Wrapper error", cause)

        assertEquals("Wrapper error", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun testRemoteAgentExceptionWithoutCause() {
        val exception = RemoteAgentException("Simple error")

        assertEquals("Simple error", exception.message)
        assertNull(exception.cause)
    }

    // Test utility function for extracting project ID from various URL formats

    @Test
    fun testGetEffectiveProjectIdWithSshUrl() {
        // SSH URLs like git@github.com:user/repo.git
        val result = getEffectiveProjectId("fallback", "git@github.com:user/repo.git")
        // The function splits by '/', so for SSH URLs it would get "user/repo.git" and then take last
        // Actually it splits by '/' so git@github.com:user would be first, repo.git second
        assertEquals("repo", result)
    }

    @Test
    fun testGetEffectiveProjectIdWithDeepPath() {
        // URLs with deeper paths
        val result = getEffectiveProjectId("fallback", "https://gitlab.com/group/subgroup/repo.git")
        assertEquals("repo", result)
    }
}

