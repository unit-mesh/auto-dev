package cc.unitmesh.devins.ui.compose.agent

import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for CodingAgentViewModel built-in command handling
 */
class CodingAgentViewModelTest {
    private fun createMockLLMService(): KoogLLMService {
        val config =
            ModelConfig(
                provider = LLMProviderType.DEEPSEEK,
                modelName = "deepseek-chat",
                apiKey = "test-key",
                temperature = 0.7,
                maxTokens = 128000,
                baseUrl = "https://api.deepseek.com"
            )
        return KoogLLMService.create(config)
    }

    @Test
    fun `should handle init command without starting agent execution`() =
        runTest {
            val llmService = createMockLLMService()
            val viewModel =
                CodingAgentViewModel(
                    llmService = llmService,
                    projectPath = "/test/project",
                    maxIterations = 10
                )

            // Execute /init command
            viewModel.executeTask("/init")

            // Should not be executing (built-in commands don't set isExecuting)
            assertFalse(viewModel.isExecuting, "Built-in commands should not set isExecuting to true")
        }

    @Test
    fun `should handle clear command`() =
        runTest {
            val llmService = createMockLLMService()
            val viewModel =
                CodingAgentViewModel(
                    llmService = llmService,
                    projectPath = "/test/project",
                    maxIterations = 10
                )

            // Execute /clear command
            viewModel.executeTask("/clear")

            // Should not be executing
            assertFalse(viewModel.isExecuting, "Clear command should not set isExecuting to true")
        }

    @Test
    fun `should handle help command`() =
        runTest {
            val llmService = createMockLLMService()
            val viewModel =
                CodingAgentViewModel(
                    llmService = llmService,
                    projectPath = "/test/project",
                    maxIterations = 10
                )

            // Execute /help command
            viewModel.executeTask("/help")

            // Should not be executing
            assertFalse(viewModel.isExecuting, "Help command should not set isExecuting to true")
        }

    @Test
    fun `should handle regular tasks by starting agent execution`() =
        runTest {
            val llmService = createMockLLMService()
            val viewModel =
                CodingAgentViewModel(
                    llmService = llmService,
                    projectPath = "/test/project",
                    maxIterations = 10
                )

            // Execute regular task
            viewModel.executeTask("Create a simple hello world function")

            // Should be executing for regular tasks
            assertTrue(viewModel.isExecuting, "Regular tasks should set isExecuting to true")

            // Cancel the task to clean up
            viewModel.cancelTask()
            assertFalse(viewModel.isExecuting, "Task should be cancelled")
        }

    @Test
    fun `should handle unknown slash commands by delegating to agent`() =
        runTest {
            val llmService = createMockLLMService()
            val viewModel =
                CodingAgentViewModel(
                    llmService = llmService,
                    projectPath = "/test/project",
                    maxIterations = 10
                )

            // Execute unknown slash command
            viewModel.executeTask("/unknown-command")

            // Should be executing (unknown commands are delegated to agent)
            assertTrue(viewModel.isExecuting, "Unknown slash commands should be delegated to agent")

            // Cancel the task to clean up
            viewModel.cancelTask()
            assertFalse(viewModel.isExecuting, "Task should be cancelled")
        }
}
