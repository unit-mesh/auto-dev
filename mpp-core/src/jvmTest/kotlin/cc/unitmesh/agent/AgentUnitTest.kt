package cc.unitmesh.agent

import cc.unitmesh.agent.communication.AgentChannel
import cc.unitmesh.agent.communication.AgentEvent
import cc.unitmesh.agent.communication.AgentSubmission
import cc.unitmesh.agent.core.DefaultAgentExecutor
import cc.unitmesh.agent.model.AgentContext
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.model.AgentResult
import cc.unitmesh.agent.model.AgentStep
import cc.unitmesh.agent.model.AgentActivity
import cc.unitmesh.agent.model.ModelConfig as AgentModelConfig
import cc.unitmesh.agent.model.PromptConfig
import cc.unitmesh.agent.model.RunConfig
import cc.unitmesh.agent.model.TerminateReason
import cc.unitmesh.agent.model.ToolConfig
import cc.unitmesh.agent.subagent.ErrorRecoveryAgent
import cc.unitmesh.agent.subagent.LogSummaryAgent
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig as LLMModelConfig
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Agent 单元测试
 */
class AgentUnitTest {

    /**
     * 创建 Mock LLM Service（用于测试）
     */
    private fun createMockLLMService(response: String = "TASK_COMPLETE: Hello!"): KoogLLMService {
        return KoogLLMService(
            LLMModelConfig(
                provider = LLMProviderType.OLLAMA,
                modelName = "mock-model",
                baseUrl = "http://localhost:11434"
            )
        )
    }

    @Test
    fun `AgentContext should be created with correct values`() {
        val context = AgentContext.create(
            agentName = "test-agent",
            sessionId = "session-123",
            inputs = mapOf("key" to "value"),
            projectPath = "/test/path"
        )

        assertTrue(context.agentId.startsWith("test-agent-"))
        assertEquals("session-123", context.sessionId)
        assertEquals("value", context.inputs["key"])
        assertEquals("/test/path", context.projectPath)
    }

    @Test
    fun `AgentDefinition should hold configuration correctly`() {
        val definition = AgentDefinition(
            name = "test-agent",
            displayName = "Test Agent",
            description = "Test description",
            promptConfig = PromptConfig(
                systemPrompt = "You are a test agent",
                queryTemplate = "Task: \${task}"
            ),
            modelConfig = AgentModelConfig(modelId = "gpt-4"),
            runConfig = RunConfig(maxTurns = 10, maxTimeMinutes = 5),
            toolConfig = ToolConfig(allowedTools = listOf("read-file", "write-file"))
        )

        assertEquals("test-agent", definition.name)
        assertEquals("Test Agent", definition.displayName)
        assertEquals(10, definition.runConfig.maxTurns)
        assertEquals(2, definition.toolConfig?.allowedTools?.size)
    }

    @Test
    fun `AgentChannel should emit and receive events`() = runBlocking {
        val channel = AgentChannel()
        val events = mutableListOf<AgentEvent>()

        // 在后台收集事件
        val job = launch {
            channel.events().take(3).toList(events)
        }

        // 发送事件
        channel.emit(AgentEvent.Progress(1, 10, "Step 1"))
        channel.emit(AgentEvent.StreamUpdate("Hello"))
        channel.emit(AgentEvent.TaskComplete("Done"))

        // 等待收集完成
        job.join()

        assertEquals(3, events.size)
        assertTrue(events[0] is AgentEvent.Progress)
        assertTrue(events[1] is AgentEvent.StreamUpdate)
        assertTrue(events[2] is AgentEvent.TaskComplete)
    }

    @Test
    fun `AgentChannel should submit and receive submissions`() = runBlocking {
        val channel = AgentChannel()
        val submissions = mutableListOf<AgentSubmission>()

        // 在后台收集提交
        val job = launch {
            channel.submissions().take(2).toList(submissions)
        }

        // 发送提交
        channel.submit(AgentSubmission.SendPrompt("Hello"))
        channel.submit(AgentSubmission.CancelTask("task-1"))

        // 等待收集完成
        job.join()

        assertEquals(2, submissions.size)
        assertTrue(submissions[0] is AgentSubmission.SendPrompt)
        assertTrue(submissions[1] is AgentSubmission.CancelTask)
    }

    @Test
    fun `ErrorRecoveryAgent should validate input correctly`() {
        val mockLLM = createMockLLMService()
        val agent = ErrorRecoveryAgent("/test/path", mockLLM)

        val input = mapOf(
            "command" to "./gradlew build",
            "errorMessage" to "Build failed"
        )

        val errorContext = agent.validateInput(input)

        assertEquals("./gradlew build", errorContext.command)
        assertEquals("Build failed", errorContext.errorMessage)
    }

    @Test
    fun `ErrorRecoveryAgent should throw exception for invalid input`() {
        val mockLLM = createMockLLMService()
        val agent = ErrorRecoveryAgent("/test/path", mockLLM)

        val input = mapOf("invalid" to "data")

        assertFailsWith<IllegalArgumentException> {
            agent.validateInput(input)
        }
    }

    @Test
    fun `LogSummaryAgent should detect if summarization is needed`() {
        val mockLLM = createMockLLMService()
        val agent = LogSummaryAgent(mockLLM, threshold = 100)

        val shortOutput = "Short output"
        val longOutput = "x".repeat(200)

        assertFalse(agent.needsSummarization(shortOutput))
        assertTrue(agent.needsSummarization(longOutput))
    }

    @Test
    fun `LogSummaryAgent should validate input correctly`() {
        val mockLLM = createMockLLMService()
        val agent = LogSummaryAgent(mockLLM)

        val input = mapOf(
            "command" to "npm test",
            "output" to "Test output",
            "exitCode" to 0,
            "executionTime" to 1000
        )

        val context = agent.validateInput(input)

        assertEquals("npm test", context.command)
        assertEquals("Test output", context.output)
        assertEquals(0, context.exitCode)
        assertEquals(1000, context.executionTime)
    }

    @Test
    fun `AgentStep should track execution step`() {
        val step = AgentStep(
            step = 1,
            action = "test-action",
            tool = "test-tool",
            params = mapOf("key" to "value"),
            result = "success",
            success = true
        )

        assertEquals(1, step.step)
        assertEquals("test-action", step.action)
        assertEquals("test-tool", step.tool)
        assertTrue(step.success)
    }

    @Test
    fun `AgentResult Success should contain output`() {
        val result = AgentResult.Success(
            output = mapOf("result" to "done"),
            terminateReason = TerminateReason.GOAL,
            steps = listOf(
                AgentStep(1, "action1", success = true)
            )
        )

        assertTrue(result.output.containsKey("result"))
        assertEquals("done", result.output["result"])
        assertEquals(TerminateReason.GOAL, result.terminateReason)
        assertEquals(1, result.steps.size)
    }

    @Test
    fun `AgentResult Failure should contain error`() {
        val result = AgentResult.Failure(
            error = "Task failed",
            terminateReason = TerminateReason.ERROR,
            steps = listOf(
                AgentStep(1, "action1", success = false)
            )
        )

        assertEquals("Task failed", result.error)
        assertEquals(TerminateReason.ERROR, result.terminateReason)
        assertEquals(1, result.steps.size)
    }

    @Test
    fun `TerminateReason should have correct values`() {
        assertEquals(5, TerminateReason.values().size)
        assertTrue(TerminateReason.values().contains(TerminateReason.GOAL))
        assertTrue(TerminateReason.values().contains(TerminateReason.MAX_TURNS))
        assertTrue(TerminateReason.values().contains(TerminateReason.TIMEOUT))
        assertTrue(TerminateReason.values().contains(TerminateReason.ERROR))
        assertTrue(TerminateReason.values().contains(TerminateReason.ABORTED))
    }

    @Test
    fun `AgentActivity should have different types`() {
        val activities = listOf(
            AgentActivity.ToolCallStart("tool", mapOf()),
            AgentActivity.ToolCallEnd("tool", "result"),
            AgentActivity.ThoughtChunk("thinking"),
            AgentActivity.Error("context", "error"),
            AgentActivity.Progress("message"),
            AgentActivity.StreamUpdate("text"),
            AgentActivity.TaskComplete("result")
        )

        assertEquals(7, activities.size)
        assertTrue(activities[0] is AgentActivity.ToolCallStart)
        assertTrue(activities[1] is AgentActivity.ToolCallEnd)
        assertTrue(activities[2] is AgentActivity.ThoughtChunk)
        assertTrue(activities[3] is AgentActivity.Error)
        assertTrue(activities[4] is AgentActivity.Progress)
        assertTrue(activities[5] is AgentActivity.StreamUpdate)
        assertTrue(activities[6] is AgentActivity.TaskComplete)
    }

    @Test
    fun `DefaultAgentExecutor should cancel agent`() = runBlocking {
        val channel = AgentChannel()
        val executor = DefaultAgentExecutor(createMockLLMService(), channel)

        val context = AgentContext.create(
            agentName = "test",
            sessionId = "session",
            inputs = mapOf(),
            projectPath = "/test"
        )

        // 取消不应抛出异常
        try {
            executor.cancel(context.agentId)
        } catch (e: Exception) {
            fail("Should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `PromptConfig should support template substitution in query`() {
        val promptConfig = PromptConfig(
            systemPrompt = "You are a test agent",
            queryTemplate = "Task: \${task}, User: \${user}"
        )

        // 验证模板包含占位符
        assertTrue(promptConfig.queryTemplate!!.contains("\${task}"))
        assertTrue(promptConfig.queryTemplate!!.contains("\${user}"))
    }

    @Test
    fun `ModelConfig should have correct default values`() {
        val config = AgentModelConfig(modelId = "gpt-4")

        assertEquals("gpt-4", config.modelId)
        assertEquals(0.7, config.temperature)
        assertEquals(4096, config.maxTokens)
        assertEquals(0.95, config.topP)
    }

    @Test
    fun `RunConfig should validate max values`() {
        val config = RunConfig(
            maxTurns = 50,
            maxTimeMinutes = 10,
            terminateOnError = false
        )

        assertEquals(50, config.maxTurns)
        assertEquals(10, config.maxTimeMinutes)
        assertFalse(config.terminateOnError)
    }

    @Test
    fun `ToolConfig should store allowed tools`() {
        val config = ToolConfig(
            allowedTools = listOf("read-file", "write-file", "shell"),
            toolTimeout = 30000
        )

        assertEquals(3, config.allowedTools.size)
        assertTrue(config.allowedTools.contains("read-file"))
        assertEquals(30000, config.toolTimeout)
    }

}

