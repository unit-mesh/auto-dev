package cc.unitmesh.server.service

import cc.unitmesh.agent.AgentTask
import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.NamedModelConfig
import cc.unitmesh.server.config.LLMConfig as ServerLLMConfig
import cc.unitmesh.server.config.ServerConfigLoader
import cc.unitmesh.server.model.*
import cc.unitmesh.server.render.ServerSideRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class AgentService(private val fallbackLLMConfig: ServerLLMConfig) {

    // Load server-side configuration from ~/.autodev/config.yaml
    private val serverConfig: NamedModelConfig? by lazy {
        try {
            ServerConfigLoader.loadActiveConfig()
        } catch (e: Exception) {
            println("⚠️  Failed to load server config from ~/.autodev/config.yaml: ${e.message}")
            println("   Will use fallback config from environment variables")
            null
        }
    }

    /**
     * Execute agent synchronously and return final result
     */
    suspend fun executeAgent(
        projectPath: String,
        request: AgentRequest
    ): AgentResponse {
        val llmService = createLLMService(request.llmConfig)
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
        val llmService = createLLMService(request.llmConfig)
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

    /**
     * Create LLM service with priority:
     * 1. Use client-provided llmConfig if available
     * 2. Otherwise use server's ~/.autodev/config.yaml configuration
     * 3. Otherwise use fallback config from environment variables
     */
    private fun createLLMService(clientConfig: LLMConfig? = null): KoogLLMService {
        val (provider, modelName, apiKey, baseUrl) = when {
            // Priority 1: Client-provided config
            clientConfig != null -> {
                println("🔧 Using client-provided LLM config: ${clientConfig.provider}/${clientConfig.modelName}")
                Quadruple(
                    clientConfig.provider,
                    clientConfig.modelName,
                    clientConfig.apiKey,
                    clientConfig.baseUrl
                )
            }
            // Priority 2: Server's ~/.autodev/config.yaml
            serverConfig != null -> {
                println("🔧 Using server config from ~/.autodev/config.yaml: ${serverConfig?.provider}/${serverConfig?.model}")
                Quadruple(
                    serverConfig?.provider ?: "openai",
                    serverConfig?.model ?: "gpt-4",
                    serverConfig?.apiKey ?: "",
                    serverConfig?.baseUrl ?: ""
                )
            }
            // Priority 3: Fallback to environment variables
            else -> {
                println("🔧 Using fallback config from environment: ${fallbackLLMConfig.provider}/${fallbackLLMConfig.modelName}")
                Quadruple(
                    fallbackLLMConfig.provider,
                    fallbackLLMConfig.modelName,
                    fallbackLLMConfig.apiKey,
                    fallbackLLMConfig.baseUrl
                )
            }
        }

        val modelConfig = ModelConfig(
            provider = LLMProviderType.valueOf(provider.uppercase()),
            modelName = modelName,
            apiKey = apiKey,
            temperature = 0.7,
            maxTokens = 4096,
            baseUrl = baseUrl.ifEmpty { "" }
        )

        return KoogLLMService(modelConfig)
    }

    // Helper data class for multiple return values
    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
            mcpToolConfigService = mcpToolConfigService,
            enableLLMStreaming = false  // 暂时禁用 LLM 流式，使用非流式模式确保输出
        )
    }
}

