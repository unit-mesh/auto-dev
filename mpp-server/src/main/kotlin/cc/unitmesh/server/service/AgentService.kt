package cc.unitmesh.server.service

import cc.unitmesh.agent.AgentEditInfo
import cc.unitmesh.agent.AgentEvent
import cc.unitmesh.agent.AgentStepInfo
import cc.unitmesh.agent.AgentTask
import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.NamedModelConfig
import cc.unitmesh.server.config.ServerConfigLoader
import cc.unitmesh.server.model.*
import cc.unitmesh.server.render.ServerSideRenderer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import cc.unitmesh.server.config.LLMConfig as ServerLLMConfig

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
     * Execute agent with SSE streaming (optionally with git clone first)
     */
    suspend fun executeAgentStream(
        projectPath: String,
        request: AgentRequest
    ): Flow<AgentEvent> = flow {
        // If gitUrl is provided, clone the repository first
        val actualProjectPath = if (!request.gitUrl.isNullOrBlank()) {
            val gitCloneService = GitCloneService()
            
            // Collect and emit all clone logs
            gitCloneService.cloneRepositoryWithLogs(
                gitUrl = request.gitUrl,
                branch = request.branch,
                username = request.username,
                password = request.password,
                projectId = request.projectId
            ).collect { event ->
                emit(event)
            }
            
            // Get the cloned path
            val clonedPath = gitCloneService.lastClonedPath
            if (clonedPath == null) {
                emit(AgentEvent.Error("Clone failed - no project path available"))
                return@flow
            }
            
            clonedPath
        } else {
            projectPath
        }
        
        // Now execute the agent
        val llmService = createLLMService(request.llmConfig)
        val renderer = ServerSideRenderer()

        val agent = createCodingAgent(actualProjectPath, llmService, renderer)

        try {
            val task = AgentTask(
                requirement = request.task,
                projectPath = actualProjectPath
            )

            coroutineScope {
                launch {
                    try {
                        val result = agent.executeTask(task)
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
                        e.printStackTrace()
                        renderer.sendError("Agent execution failed: ${e.message}")
                    } finally {
                        agent.shutdown()
                    }
                }
                renderer.events.collect { event ->
                    emit(event)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
            clientConfig != null -> {
                Quadruple(
                    clientConfig.provider,
                    clientConfig.modelName,
                    clientConfig.apiKey,
                    clientConfig.baseUrl
                )
            }
            serverConfig != null -> {
                Quadruple(
                    serverConfig?.provider ?: "deepseek",
                    serverConfig?.model ?: "deepseek-chat",
                    serverConfig?.apiKey ?: "",
                    serverConfig?.baseUrl ?: ""
                )
            }
            else -> {
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
            temperature = 0.9,
            maxTokens = 128000,
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
            enableLLMStreaming = true  // 启用 LLM 流式输出以支持 SSE
        )
    }
}

