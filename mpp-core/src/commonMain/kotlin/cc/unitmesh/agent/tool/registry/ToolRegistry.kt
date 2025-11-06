package cc.unitmesh.agent.tool.registry

import cc.unitmesh.agent.core.SubAgentManager
import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.impl.*
import cc.unitmesh.agent.tool.provider.BuiltinToolsProvider
import cc.unitmesh.agent.tool.provider.ToolDependencies
import cc.unitmesh.agent.tool.provider.ToolProviderRegistry
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.llm.KoogLLMService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Registry for managing and discovering tools
 */
class ToolRegistry(
    private val fileSystem: ToolFileSystem = DefaultToolFileSystem(),
    private val shellExecutor: ShellExecutor = DefaultShellExecutor(),
    private val configService: cc.unitmesh.agent.config.McpToolConfigService? = null,
    private val subAgentManager: SubAgentManager? = null,
    private val llmService: KoogLLMService? = null
) {
    private val logger = getLogger("ToolRegistry")
    private val tools = mutableMapOf<String, ExecutableTool<*, *>>()
    private val json = Json { ignoreUnknownKeys = true }
    
    init {
        registerBuiltinTools()
    }
    
    /**
     * Register a tool in the registry
     */
    fun <TParams : Any, TResult : ToolResult> registerTool(
        tool: ExecutableTool<TParams, TResult>
    ) {
        tools[tool.name] = tool
    }
    
    /**
     * Unregister a tool from the registry
     */
    fun unregisterTool(toolName: String) {
        tools.remove(toolName)
    }
    
    /**
     * Get a tool by name
     */
    fun getTool(toolName: String): ExecutableTool<*, *>? {
        return tools[toolName]
    }
    
    /**
     * Get all registered tools
     */
    fun getAllTools(): Map<String, ExecutableTool<*, *>> {
        return tools.toMap()
    }
    
    /**
     * Get all tool names
     */
    fun getToolNames(): Set<String> {
        return tools.keys.toSet()
    }
    
    /**
     * Check if a tool is registered
     */
    fun hasToolNamed(toolName: String): Boolean {
        return toolName in tools
    }

    fun generateToolExample(tool: ExecutableTool<*, *>): String {
        val toToolType = tool.name.toToolType() ?: return "/${tool.name} <parameters>"
        return when (toToolType) {
            ToolType.ReadFile -> """
                /read-file path="src/main.kt"
                /read-file path="README.md" startLine=1 endLine=10
            """.trimIndent()

            ToolType.WriteFile -> """
                /write-file path="output.txt" content="Hello, World!"
                /write-file path="config.json" content="{\"key\": \"value\"}" createDirectories=true
            """.trimIndent()

            ToolType.Grep -> """
                /grep pattern="function.*main" path="src" include="*.kt"
                /grep pattern="TODO|FIXME" recursive=true caseSensitive=false
            """.trimIndent()

            ToolType.Glob -> """
                /glob pattern="*.kt" path="src"
                /glob pattern="**/*.{ts,js}" includeFileInfo=true
            """.trimIndent()

            ToolType.Shell -> """
                /shell command="ls -la"
                /shell command="npm test" workingDirectory="frontend" timeoutMs=60000
            """.trimIndent()

            else -> {
                "/${tool.name} <parameters>"
            }
        }
    }

    /**
     * Create a tool invocation from parameters
     */
    @Suppress("UNCHECKED_CAST")
    fun <TParams : Any> createInvocation(
        toolName: String,
        params: TParams
    ): ToolInvocation<TParams, ToolResult>? {
        val tool = tools[toolName] as? ExecutableTool<TParams, ToolResult>
        return tool?.createInvocation(params)
    }
    
    /**
     * Create a tool invocation from JSON parameters
     */
    fun <TParams : Any> createInvocationFromJson(
        toolName: String,
        paramsJson: JsonElement,
        paramsClass: String
    ): ToolInvocation<TParams, ToolResult>? {
        return try {
            // For now, we'll need to handle JSON parsing differently in KMP
            // This is a simplified version - in practice you'd need proper serialization
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Execute a tool with parameters
     */
    suspend fun <TParams : Any> executeTool(
        toolName: String,
        params: TParams,
        context: ToolExecutionContext = ToolExecutionContext()
    ): ToolResult {
        val invocation = createInvocation(toolName, params)
            ?: return ToolResult.Error("Tool not found: $toolName", ToolErrorType.UNKNOWN.code)
        
        return try {
            invocation.execute(context)
        } catch (e: ToolException) {
            e.toToolResult()
        } catch (e: Exception) {
            ToolResult.Error(
                message = e.message ?: "Unknown error occurred",
                errorType = ToolErrorType.INTERNAL_ERROR.code
            )
        }
    }

    fun getAgentTools(): List<AgentTool> {
        return tools.values.map { tool ->
            AgentTool(
                name = tool.name,
                description = tool.description,
                example = generateToolExample(tool),
                isDevIns = true
            )
        }
    }
    
    /**
     * Get tool information for a specific tool
     */
    fun getToolInfo(toolName: String): AgentTool? {
        val tool = tools[toolName] ?: return null
        return AgentTool(
            name = tool.name,
            description = tool.description,
            example = generateToolExample(tool),
            isDevIns = true
        )
    }
    
    /**
     * Register all built-in tools (filtered by configuration if available)
     * 
     * Tools are now discovered through ToolProvider mechanism, making it easy
     * to add new tools by just creating a new provider.
     */
    private fun registerBuiltinTools() {
        // Ensure the builtin provider is registered
        if (ToolProviderRegistry.getProviders().isEmpty()) {
            ToolProviderRegistry.register(BuiltinToolsProvider())
        }
        
        // Create ToolDependencies with all available dependencies
        val dependencies = ToolDependencies(
            fileSystem = fileSystem,
            shellExecutor = shellExecutor,
            subAgentManager = subAgentManager,
            llmService = llmService
        )
        
        // Discover all tools from registered providers
        val allBuiltinTools = ToolProviderRegistry.discoverTools(dependencies)

        logger.debug { "🔧 [ToolRegistry] All available built-in tools: ${allBuiltinTools.map { it.name }}" }
        logger.debug { "🔧 [ToolRegistry] ConfigService available: ${configService != null}" }

        // Filter tools based on configuration if available
        val toolsToRegister = if (configService != null) {
            val filtered = configService.filterBuiltinTools(allBuiltinTools)
            logger.debug { "🔧 [ToolRegistry] Filtered tools: ${filtered.map { it.name }}" }
            filtered
        } else {
            logger.debug { "🔧 [ToolRegistry] No config service, registering all tools" }
            allBuiltinTools
        }

        toolsToRegister.forEach { tool ->
            registerTool(tool)
        }

        logger.info { "🔧 Registered ${toolsToRegister.size}/${allBuiltinTools.size} built-in tools" }
        toolsToRegister.forEach { tool ->
            logger.debug { "   Built-in tool: ${tool.name}" }
        }
    }
}

data class RegistryStats(
    val totalTools: Int,
    val fileSystemTools: Int,
    val executionTools: Int,
    val availableTools: List<String>
)

object GlobalToolRegistry {
    private var instance: ToolRegistry? = null
    
    fun getInstance(
        fileSystem: ToolFileSystem? = null,
        shellExecutor: ShellExecutor? = null
    ): ToolRegistry {
        if (instance == null) {
            instance = ToolRegistry(
                fileSystem ?: DefaultToolFileSystem(),
                shellExecutor ?: DefaultShellExecutor()
            )
        }
        return instance!!
    }
    
    fun setInstance(registry: ToolRegistry) {
        instance = registry
    }
    
    fun reset() {
        instance = null
    }
}
