package cc.unitmesh.server.service

import cc.unitmesh.agent.AgentTask
import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.server.config.LLMConfig
import cc.unitmesh.server.model.*
import cc.unitmesh.server.render.ServerSideRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class AgentService(private val defaultLLMConfig: LLMConfig) {

    /**
     * Execute agent synchronously and return final result
     */
    suspend fun executeAgent(
        projectPath: String,
        request: AgentRequest
    ): AgentResponse {
        val llmService = createLLMService()
        val renderer = DefaultCodingAgentRenderer()

        val agent = createCodingAgent(projectPath, llmService, renderer)

        return try {
            val task = AgentTask(
                requirement = request.task,
                projectPath = projectPath
            )

            val result = agent.executeTask(task)

            AgentResponse(
                success = result.success,
                message = result.message,
                output = result.message,
                iterations = result.steps.size,
                steps = result.steps.map { step ->
                    AgentStepInfo(
                        step = step.step,
                        action = step.action,
                        tool = step.tool,
                        success = step.success
                    )
                },
                edits = result.edits.map { edit ->
                    AgentEditInfo(
                        file = edit.file,
                        operation = edit.operation.name,
                        content = edit.content
                    )
                }
            )
        } catch (e: Exception) {
            AgentResponse(
                success = false,
                message = "Agent execution failed: ${e.message}",
                output = null
            )
        } finally {
            agent.shutdown()
        }
    }

    /**
     * Execute agent with SSE streaming
     */
    suspend fun executeAgentStream(
        projectPath: String,
        request: AgentRequest
    ): Flow<AgentEvent> = flow {
        val llmService = createLLMService()
        val renderer = ServerSideRenderer()

        val agent = createCodingAgent(projectPath, llmService, renderer)

        try {
            val task = AgentTask(
                requirement = request.task,
                projectPath = projectPath
            )

            // Launch agent execution in background and collect events
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                try {
                    val result = agent.executeTask(task)

                    // Send final completion event
                    renderer.sendComplete(
                        success = result.success,
                        message = result.message,
                        iterations = result.steps.size,
                        steps = result.steps.map { step ->
                            AgentStepInfo(
                                step = step.step,
                                action = step.action,
                                tool = step.tool,
                                success = step.success
                            )
                        },
                        edits = result.edits.map { edit ->
                            AgentEditInfo(
                                file = edit.file,
                                operation = edit.operation.name,
                                content = edit.content
                            )
                        }
                    )
                } catch (e: Exception) {
                    renderer.sendError("Agent execution failed: ${e.message}")
                } finally {
                    agent.shutdown()
                }
            }

            // Emit all events from the renderer
            renderer.events.collect { event ->
                emit(event)
            }
        } catch (e: Exception) {
            emit(AgentEvent.Error("Failed to start agent: ${e.message}"))
        }
    }

    private fun createLLMService(): KoogLLMService {
        val modelConfig = ModelConfig(
            provider = LLMProviderType.valueOf(defaultLLMConfig.provider.uppercase()),
            modelName = defaultLLMConfig.modelName,
            apiKey = defaultLLMConfig.apiKey,
            temperature = 0.7,
            maxTokens = 4096,
            baseUrl = defaultLLMConfig.baseUrl.ifEmpty { "" }
        )

        return KoogLLMService(modelConfig)
    }

    private fun createCodingAgent(
        projectPath: String,
        llmService: KoogLLMService,
        renderer: cc.unitmesh.agent.render.CodingAgentRenderer
    ): CodingAgent {
        // Create a simple tool config with default settings
        val toolConfig = ToolConfigFile.default()
        val mcpToolConfigService = McpToolConfigService(toolConfig)

        return CodingAgent(
            projectPath = projectPath,
            llmService = llmService,
            maxIterations = 20,
            renderer = renderer,
            fileSystem = null,
            shellExecutor = null,
            mcpServers = null,
            mcpToolConfigService = mcpToolConfigService
        )
    }
}

