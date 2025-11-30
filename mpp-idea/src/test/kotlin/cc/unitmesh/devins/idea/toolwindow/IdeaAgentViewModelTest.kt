package cc.unitmesh.devins.idea.toolwindow

// Temporarily disabled - requires IntelliJ Platform Test Framework
// To run these tests, use: ./gradlew :mpp-idea:test --tests "*IdeaAgentViewModelTest" with proper IntelliJ Platform setup
// See AGENTS.md for more details on running IntelliJ Platform tests

/*
import cc.unitmesh.agent.AgentType
import cc.unitmesh.devins.idea.renderer.JewelRenderer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Unit tests for IdeaAgentViewModel.
 *
 * Tests the ViewModel's functionality including:
 * - Agent type switching
 * - Configuration loading
 * - Task execution
 * - Builtin command handling
 * - Tool loading status
 *
 * Note: These tests extend BasePlatformTestCase which requires IntelliJ Platform.
 * They should be run through the IntelliJ Platform Test Framework.
 */
class IdeaAgentViewModelTest : BasePlatformTestCase() {

    private lateinit var viewModel: IdeaAgentViewModel
    private lateinit var testScope: CoroutineScope

    override fun setUp() {
        super.setUp()
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        viewModel = IdeaAgentViewModel(project, testScope)
    }

    override fun tearDown() {
        testScope.cancel()
        viewModel.dispose()
        super.tearDown()
    }

    fun testInitialAgentType() = runBlocking {
        // Default agent type should be CODING
        val agentType = viewModel.currentAgentType.first()
        assertEquals(AgentType.CODING, agentType)
    }

    fun testAgentTypeChange() = runBlocking {
        // Change agent type
        viewModel.onAgentTypeChange(AgentType.CODE_REVIEW)
        val agentType = viewModel.currentAgentType.first()
        assertEquals(AgentType.CODE_REVIEW, agentType)
    }

    fun testRendererInitialization() {
        // Renderer should be initialized
        assertNotNull(viewModel.renderer)
        assertTrue(viewModel.renderer is JewelRenderer)
    }

    fun testInitialExecutingState() = runBlocking {
        // Should not be executing initially
        val isExecuting = viewModel.isExecuting.first()
        assertFalse(isExecuting)
    }

    fun testClearHistory() = runBlocking {
        // Add a message
        viewModel.renderer.addUserMessage("Test message")
        val timeline = viewModel.renderer.timeline.first()
        assertTrue(timeline.isNotEmpty())

        // Clear history
        viewModel.clearHistory()
        val clearedTimeline = viewModel.renderer.timeline.first()
        assertTrue(clearedTimeline.isEmpty())
    }

    fun testClearCommand() = runBlocking {
        // Add a message
        viewModel.renderer.addUserMessage("Test message")

        // Execute /clear command
        viewModel.executeTask("/clear", null)

        // Wait a bit for the command to execute
        Thread.sleep(100)

        // Timeline should contain command result
        val timeline = viewModel.renderer.timeline.first()
        assertTrue(timeline.isNotEmpty())
    }

    fun testHelpCommand() = runBlocking {
        // Execute /help command
        viewModel.executeTask("/help", null)

        // Wait a bit for the command to execute
        Thread.sleep(100)

        // Timeline should contain help text
        val timeline = viewModel.renderer.timeline.first()
        assertTrue(timeline.isNotEmpty())

        // Should contain TaskCompleteItem with help text
        val lastItem = timeline.last()
        assertTrue(lastItem is JewelRenderer.TimelineItem.TaskCompleteItem)
    }

    fun testStatusCommand() = runBlocking {
        // Execute /status command
        viewModel.executeTask("/status", null)

        // Wait a bit for the command to execute
        Thread.sleep(100)

        // Timeline should contain status
        val timeline = viewModel.renderer.timeline.first()
        assertTrue(timeline.isNotEmpty())
    }

    fun testToolLoadingStatus() {
        // Get tool loading status
        val status = viewModel.getToolLoadingStatus()

        // Verify structure
        assertNotNull(status)
        assertTrue(status.subAgentsTotal >= 0)
        assertTrue(status.mcpServersTotal >= 0)
    }

    fun testConfigDialogState() = runBlocking {
        // Initially not showing
        val initialState = viewModel.showConfigDialog.first()
        assertFalse(initialState)

        // Show dialog
        viewModel.setShowConfigDialog(true)
        val showingState = viewModel.showConfigDialog.first()
        assertTrue(showingState)

        // Hide dialog
        viewModel.setShowConfigDialog(false)
        val hiddenState = viewModel.showConfigDialog.first()
        assertFalse(hiddenState)
    }

    fun testIsConfigured() {
        // Initially may or may not be configured (depends on config file)
        val isConfigured = viewModel.isConfigured()
        // Just verify it doesn't throw
        assertNotNull(isConfigured)
    }

    fun testCancelTask() = runBlocking {
        // Cancel should not throw when not executing
        viewModel.cancelTask()

        val isExecuting = viewModel.isExecuting.first()
        assertFalse(isExecuting)
    }
}
*/

