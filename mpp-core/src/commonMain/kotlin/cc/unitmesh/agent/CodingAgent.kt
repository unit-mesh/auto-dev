package cc.unitmesh.agent

import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.McpToolConfigManager
import cc.unitmesh.agent.config.ToolItem
import cc.unitmesh.agent.tool.BaseExecutableTool
import cc.unitmesh.agent.tool.ToolExecutionContext
import cc.unitmesh.agent.tool.ToolInvocation
import cc.unitmesh.agent.core.MainAgent
import cc.unitmesh.agent.executor.CodingAgentExecutor
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.agent.mcp.McpToolsInitializer
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.model.PromptConfig
import cc.unitmesh.agent.model.RunConfig
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.policy.DefaultPolicyEngine
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
import cc.unitmesh.agent.subagent.CodebaseInvestigatorAgent
import cc.unitmesh.agent.subagent.ErrorRecoveryAgent
import cc.unitmesh.agent.subagent.LogSummaryAgent
import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CodingAgent(
    private val projectPath: String,
    private val llmService: KoogLLMService,
    override val maxIterations: Int = 100,
    private val renderer: CodingAgentRenderer = DefaultCodingAgentRenderer(),
    private val fileSystem: ToolFileSystem? = null,
    private val shellExecutor: ShellExecutor? = null,
    private val mcpServers: Map<String, McpServerConfig>? = null,
    private val mcpToolConfigService: McpToolConfigService
) : MainAgent<AgentTask, ToolResult.AgentResult>(
    AgentDefinition(
        name = "CodingAgent",
        displayName = "Autonomous Coding Agent",
        description = "Autonomous coding agent for development tasks",
        promptConfig = PromptConfig(
            systemPrompt = "You are an autonomous coding agent.",
            queryTemplate = null,
            initialMessages = emptyList()
        ),
        modelConfig = ModelConfig.default(),
        runConfig = RunConfig(
            maxTurns = 100,
            maxTimeMinutes = 30,
            terminateOnError = false
        )
    )
), CodingAgentService {

    private val promptRenderer = CodingAgentPromptRenderer()

    private val configService = mcpToolConfigService

    private val toolRegistry = run {
        println("🔧 [CodingAgent] Initializing ToolRegistry with configService: ${mcpToolConfigService != null}")
        if (mcpToolConfigService != null) {
            println("🔧 [CodingAgent] Enabled builtin tools: ${mcpToolConfigService.toolConfig.enabledBuiltinTools}")
        }
        ToolRegistry(
            fileSystem = fileSystem ?: DefaultToolFileSystem(projectPath = projectPath),
            shellExecutor = shellExecutor ?: DefaultShellExecutor(),
            configService = mcpToolConfigService  // 直接传递构造函数参数
        )
    }

    private val policyEngine = DefaultPolicyEngine()
    private val toolOrchestrator = ToolOrchestrator(toolRegistry, policyEngine, renderer, mcpConfigService = mcpToolConfigService)

    private val errorRecoveryAgent = ErrorRecoveryAgent(projectPath, llmService)
    private val logSummaryAgent = LogSummaryAgent(llmService, threshold = 2000)

    private val codebaseInvestigatorAgent = CodebaseInvestigatorAgent(projectPath, llmService)
    
    private val mcpToolsInitializer = McpToolsInitializer()

    // 执行器
    private val executor = CodingAgentExecutor(
        projectPath = projectPath,
        llmService = llmService,
        toolOrchestrator = toolOrchestrator,
        renderer = renderer,
        maxIterations = maxIterations
    )

    // 标记 MCP 工具是否已初始化
    private var mcpToolsInitialized = false

    init {
        // 注册 SubAgents（作为 Tools）- 根据配置决定是否启用
        if (configService.isBuiltinToolEnabled("error-recovery")) {
            registerTool(errorRecoveryAgent)
            toolRegistry.registerTool(errorRecoveryAgent)  // 同时注册到 ToolRegistry
        }
        if (configService.isBuiltinToolEnabled("log-summary")) {
            registerTool(logSummaryAgent)
            toolRegistry.registerTool(logSummaryAgent)  // 同时注册到 ToolRegistry
        }
        if (configService.isBuiltinToolEnabled("codebase-investigator")) {
            registerTool(codebaseInvestigatorAgent)
            toolRegistry.registerTool(codebaseInvestigatorAgent)  // 同时注册到 ToolRegistry
        }

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            initializeWorkspace(projectPath)
        }
    }

    override suspend fun execute(
        input: AgentTask,
        onProgress: (String) -> Unit
    ): ToolResult.AgentResult {
        initializeWorkspace(input.projectPath)

        val context = buildContext(input)
        val systemPrompt = buildSystemPrompt(context)

        val result = executor.execute(input, systemPrompt, onProgress)

        return ToolResult.AgentResult(
            success = result.success,
            content = result.message,
            metadata = mapOf(
                "iterations" to "0", // executor 内部管理迭代
                "steps" to result.steps.size.toString(),
                "edits" to result.edits.size.toString()
            )
        )
    }

    override suspend fun executeTask(task: AgentTask): AgentResult {
        val context = buildContext(task)
        val systemPrompt = buildSystemPrompt(context)

        return executor.execute(task, systemPrompt)
    }


    override fun buildSystemPrompt(context: CodingAgentContext, language: String): String {
        return promptRenderer.render(context, language)
    }

    override suspend fun initializeWorkspace(projectPath: String) {
        val mcpServersToInit = configService.getEnabledMcpServers().takeIf { it.isNotEmpty() }
            ?: mcpServers
        
        if (!mcpServersToInit.isNullOrEmpty()) {
            initializeMcpTools(mcpServersToInit)
        }
    }
    
    /**
     * Initialize and register MCP tools from configuration
     */
    private suspend fun initializeMcpTools(mcpServers: Map<String, McpServerConfig>) {
        println("🔧 Initializing MCP tools from ${mcpServers.size} servers...")

        // Debug: Print server configurations
        mcpServers.forEach { (name, config) ->
            println("   Server '$name': ${config.command} ${config.args.joinToString(" ")} (disabled: ${config.disabled})")
        }

        try {
            val mcpTools = mcpToolsInitializer.initialize(mcpServers)
            println("🔍 Discovered ${mcpTools.size} MCP tools")
            println("🔧 [initializeMcpTools] MCP tools initialization returned ${mcpTools.size} tools")

            if (mcpTools.isNotEmpty()) {
                // Debug: Print discovered tools
                mcpTools.forEach { tool ->
                    println("   Discovered tool: ${tool.name} (${tool::class.simpleName})")
                }

                val filteredMcpTools = configService.filterMcpTools(mcpTools)
                println("🔧 Filtered to ${filteredMcpTools.size} enabled tools")

                // Debug: Print filtered tools
                filteredMcpTools.forEach { tool ->
                    println("   Enabled tool: ${tool.name}")
                }

                filteredMcpTools.forEach { tool ->
                    registerTool(tool)
                }

                println("✅ Registered ${filteredMcpTools.size}/${mcpTools.size} MCP tools from ${mcpServers.size} servers")
            } else {
                println("ℹ️  No MCP tools discovered from ${mcpServers.size} servers")
            }
        } catch (e: Exception) {
            println("⚠️  Warning: Failed to initialize MCP tools: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Shutdown MCP connections
     */
    suspend fun shutdown() {
        mcpToolsInitializer.shutdown()
    }

    private suspend fun buildContext(task: AgentTask): CodingAgentContext {
        // 尝试使用预加载的 MCP 工具，如果没有则初始化
        if (!mcpToolsInitialized) {
            println("🔧 [buildContext] Checking for preloaded MCP tools...")

            // 首先尝试从预加载缓存中获取 MCP 工具
            val mcpServersToUse = configService.getEnabledMcpServers().takeIf { it.isNotEmpty() }
                ?: mcpServers

            if (!mcpServersToUse.isNullOrEmpty()) {
                try {
                    val enabledMcpTools = configService.toolConfig.enabledMcpTools.toSet()
                    val cachedMcpTools = McpToolConfigManager.discoverMcpTools(mcpServersToUse, enabledMcpTools)

                    if (cachedMcpTools.isNotEmpty()) {
                        println("🔧 [buildContext] Found ${cachedMcpTools.values.sumOf { it.size }} preloaded MCP tools")

                        // 将预加载的工具转换为 ExecutableTool 并注册
                        cachedMcpTools.values.flatten().forEach { toolItem ->
                            if (toolItem.enabled) {
                                // 创建一个简单的 MCP 工具适配器
                                val mcpTool = createMcpToolFromItem(toolItem)
                                registerTool(mcpTool)
                                println("   Registered MCP tool: ${toolItem.name}")
                            }
                        }

                        mcpToolsInitialized = true
                        println("✅ [buildContext] Successfully registered ${cachedMcpTools.values.sumOf { it.count { tool -> tool.enabled } }} MCP tools from cache")
                    } else {
                        println("🔧 [buildContext] No preloaded MCP tools found, falling back to direct initialization...")
                        initializeMcpTools(mcpServersToUse)
                        mcpToolsInitialized = true
                    }
                } catch (e: Exception) {
                    println("⚠️ [buildContext] Failed to use preloaded MCP tools: ${e.message}")
                    if (mcpServers != null) {
                        println("🔧 [buildContext] Falling back to direct initialization...")
                        initializeMcpTools(mcpServers)
                        mcpToolsInitialized = true
                    }
                }
            }
        }

        println("🔧 [buildContext] Getting all available tools...")
        val allTools = getAllAvailableTools()
        println("🔧 [buildContext] Got ${allTools.size} tools for context")

        return CodingAgentContext.fromTask(
            task,
            toolList = allTools
        )
    }

    /**
     * 获取所有可用的工具，包括内置工具、SubAgent 和 MCP 工具
     */
    private fun getAllAvailableTools(): List<ExecutableTool<*, *>> {
        val allTools = mutableListOf<ExecutableTool<*, *>>()

        // 1. 添加 ToolRegistry 中的内置工具（已经根据配置过滤）
        allTools.addAll(toolRegistry.getAllTools().values)

        // 2. 添加 MainAgent 中注册的工具（SubAgent 和 MCP 工具）
        // 注意：避免重复添加已经在 ToolRegistry 中的 SubAgent
        val registryToolNames = toolRegistry.getAllTools().keys
        val mainAgentTools = getAllTools().filter { it.name !in registryToolNames }
        allTools.addAll(mainAgentTools)

        println("🔍 [getAllAvailableTools] 总共获取到 ${allTools.size} 个工具")
        allTools.forEach { tool ->
            println("   - ${tool.name} (${tool::class.simpleName})")
        }

        return allTools
    }

    /**
     * 从 ToolItem 创建 MCP 工具适配器
     */
    private fun createMcpToolFromItem(toolItem: ToolItem): ExecutableTool<*, *> {
        // 创建一个简单的 MCP 工具适配器
        return object : BaseExecutableTool<Map<String, Any>, ToolResult.Success>() {
            override val name: String = toolItem.name
            override val description: String = toolItem.description

            override fun getParameterClass(): String = "Map<String, Any>"

            override fun createToolInvocation(params: Map<String, Any>): ToolInvocation<Map<String, Any>, ToolResult.Success> {
                val outerTool = this
                return object : ToolInvocation<Map<String, Any>, ToolResult.Success> {
                    override val params: Map<String, Any> = params
                    override val tool: ExecutableTool<Map<String, Any>, ToolResult.Success> = outerTool

                    override fun getDescription(): String = toolItem.description
                    override fun getToolLocations(): List<cc.unitmesh.agent.tool.ToolLocation> = emptyList()

                    override suspend fun execute(context: ToolExecutionContext): ToolResult.Success {
                        // 这里应该调用实际的 MCP 工具执行
                        // 但是为了简化，我们先返回一个占位符结果
                        return ToolResult.Success("MCP tool ${toolItem.name} executed (placeholder)")
                    }
                }
            }
        }
    }


    override fun validateInput(input: Map<String, Any>): AgentTask {
        val requirement = input["requirement"] as? String
            ?: throw IllegalArgumentException("requirement is required")
        val projectPath = input["projectPath"] as? String
            ?: throw IllegalArgumentException("projectPath is required")

        return AgentTask(requirement, projectPath)
    }

    override fun formatOutput(output: ToolResult.AgentResult): String {
        return output.content
    }
}
