package cc.unitmesh.agent

import cc.unitmesh.agent.communication.AgentChannel
import cc.unitmesh.agent.communication.AgentEvent
import cc.unitmesh.agent.communication.AgentSubmission
import cc.unitmesh.agent.core.DefaultAgentExecutor
import cc.unitmesh.agent.model.*
import cc.unitmesh.agent.subagent.CodebaseInvestigatorAgent
import cc.unitmesh.agent.subagent.ErrorRecoveryAgent
import cc.unitmesh.agent.subagent.LogSummaryAgent
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.devins.filesystem.EmptyFileSystem
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for the optimized AI Agent architecture
 * 
 * Tests the integration of:
 * - CodingAgent with DefaultAgentExecutor
 * - SubAgent communication and execution
 * - AgentChannel Queue Pair pattern
 * - CodebaseInvestigatorAgent functionality
 */
class OptimizedAgentArchitectureTest {

    @Test
    fun `CodebaseInvestigatorAgent should validate input correctly`() {
        val mockLLMService = createMockLLMService()
        val agent = CodebaseInvestigatorAgent("/test/project", mockLLMService)
        
        // Test valid input
        val validInput = mapOf(
            "query" to "Find authentication code",
            "scope" to "classes"
        )
        
        val context = agent.validateInput(validInput)
        assertEquals("Find authentication code", context.query)
        assertEquals("classes", context.scope)
        assertEquals("/test/project", context.projectPath)
    }
    
    @Test
    fun `CodebaseInvestigatorAgent should handle missing query parameter`() {
        val mockLLMService = createMockLLMService()
        val agent = CodebaseInvestigatorAgent("/test/project", mockLLMService)
        
        val invalidInput = mapOf<String, Any>()
        
        try {
            agent.validateInput(invalidInput)
            throw AssertionError("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("query") == true)
        }
    }
    
    @Test
    fun `CodebaseInvestigatorAgent should execute investigation successfully`() = runBlocking {
        val mockLLMService = createMockLLMService()
        val agent = CodebaseInvestigatorAgent("/test/project", mockLLMService)
        
        val input = mapOf(
            "query" to "Find user management classes",
            "scope" to "classes"
        )
        
        val progressMessages = mutableListOf<String>()
        val result = agent.run(input) { progress ->
            progressMessages.add(progress)
        }
        
        assertTrue(result.isNotEmpty())
        assertTrue(progressMessages.isNotEmpty())
        assertTrue(progressMessages.any { it.contains("investigation") })
    }
    
    @Test
    fun `AgentChannel should handle Queue Pair communication`() = runBlocking {
        val channel = AgentChannel()
        val events = mutableListOf<AgentEvent>()
        val submissions = mutableListOf<AgentSubmission>()
        
        // Start collectors
        val eventJob = launch {
            channel.events().take(2).toList(events)
        }
        
        val submissionJob = launch {
            channel.submissions().take(2).toList(submissions)
        }
        
        // Send events and submissions
        channel.emit(AgentEvent.Progress(1, 5, "Starting"))
        channel.emit(AgentEvent.StreamUpdate("Processing..."))
        
        channel.submit(AgentSubmission.SendPrompt("Test prompt"))
        channel.submit(AgentSubmission.CancelTask("task-1"))
        
        // Wait for collection
        eventJob.join()
        submissionJob.join()
        
        assertEquals(2, events.size)
        assertEquals(2, submissions.size)
        assertTrue(events[0] is AgentEvent.Progress)
        assertTrue(events[1] is AgentEvent.StreamUpdate)
        assertTrue(submissions[0] is AgentSubmission.SendPrompt)
        assertTrue(submissions[1] is AgentSubmission.CancelTask)
    }
    
    @Test
    fun `DefaultAgentExecutor should handle agent execution`() = runBlocking {
        val mockLLMService = createMockLLMService()
        val channel = AgentChannel()
        val executor = DefaultAgentExecutor(mockLLMService, channel)
        
        val definition = AgentDefinition(
            name = "TestAgent",
            displayName = "Test Agent",
            description = "Test agent for unit testing",
            promptConfig = PromptConfig(
                systemPrompt = "You are a test agent",
                queryTemplate = "Task: \${task}",
                initialMessages = emptyList()
            ),
            modelConfig = ModelConfig(
                modelId = "test-model",
                temperature = 0.7,
                maxTokens = 1000,
                topP = 1.0
            ),
            runConfig = RunConfig(
                maxTurns = 3,
                maxTimeMinutes = 5,
                terminateOnError = false
            )
        )
        
        val context = AgentContext(
            agentId = "test-agent",
            sessionId = "test-session",
            inputs = mapOf("task" to "Test task"),
            projectPath = "/test/project",
            metadata = emptyMap()
        )
        
        val activities = mutableListOf<AgentActivity>()
        val result = executor.execute(definition, context) { activity ->
            activities.add(activity)
        }
        
        assertNotNull(result)
        assertTrue(activities.isNotEmpty())
    }
    
    private fun createMockLLMService(): KoogLLMService {
        return object : KoogLLMService {
            override suspend fun streamPrompt(
                userPrompt: String,
                fileSystem: cc.unitmesh.devins.filesystem.FileSystem,
                historyMessages: List<cc.unitmesh.llm.LLMMessage>,
                compileDevIns: Boolean
            ) = flowOf("Mock response for: ${userPrompt.take(50)}")
        }
    }
}
