package cc.unitmesh.agent.mcp

import cc.unitmesh.agent.tool.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Adapter that wraps an MCP tool as an ExecutableTool
 * 
 * This adapter bridges the gap between MCP tools and the Agent tool system,
 * allowing MCP servers to be used as standard tools in the CodingAgent.
 */
class McpToolAdapter(
    private val toolInfo: McpToolInfo,
    private val serverName: String,
    private val clientManager: McpClientManager
) : BaseExecutableTool<McpToolAdapter.Params, ToolResult.Success>() {
    
    override val name: String = "${serverName}_${toolInfo.name}"
    override val description: String = toolInfo.description
    
    override val metadata: ToolMetadata by lazy {
        ToolMetadata(
            displayName = toolInfo.name,
            tuiEmoji = "🔌", // MCP tool emoji
            composeIcon = "extension", // MCP tool icon
            category = ToolCategory.Utility, // MCP tools are utilities
            schema = object : cc.unitmesh.agent.tool.schema.DeclarativeToolSchema(
                description = toolInfo.description,
                properties = emptyMap()
            ) {
                override fun getExampleUsage(toolName: String): String = "/$toolName"
            }
        )
    }
    
    @Serializable
    data class Params(
        val arguments: String = "{}" // JSON string of arguments
    )
    
    override fun getParameterClass(): String = "McpToolAdapter.Params"
    
    override fun createToolInvocation(params: Params): ToolInvocation<Params, ToolResult.Success> {
        return McpToolInvocation(params, this)
    }
    
    private inner class McpToolInvocation(
        params: Params,
        tool: ExecutableTool<Params, ToolResult.Success>
    ) : BaseToolInvocation<Params, ToolResult.Success>(params, tool) {
        
        override fun getDescription(): String {
            return "Execute MCP tool: ${toolInfo.name} on server: $serverName"
        }
        
        override suspend fun execute(context: ToolExecutionContext): ToolResult.Success {
            try {
                val result = clientManager.executeTool(
                    serverName = serverName,
                    toolName = toolInfo.name,
                    arguments = params.arguments
                )
                
                return ToolResult.Success(
                    content = result,
                    metadata = mapOf(
                        "mcp_server" to serverName,
                        "mcp_tool" to toolInfo.name
                    )
                )
            } catch (e: Exception) {
                return ToolResult.Success(
                    content = "Error executing MCP tool: ${e.message}",
                    metadata = mapOf(
                        "mcp_server" to serverName,
                        "mcp_tool" to toolInfo.name,
                        "error" to "true"
                    )
                )
            }
        }
    }
}

/**
 * Factory for creating MCP tool adapters
 */
object McpToolAdapterFactory {
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Create tool adapters for all discovered MCP tools
     * 
     * @param discoveredTools Map of server name to list of discovered tools
     * @param clientManager The MCP client manager to use for execution
     * @return List of ExecutableTool instances
     */
    fun createAdapters(
        discoveredTools: Map<String, List<McpToolInfo>>,
        clientManager: McpClientManager
    ): List<ExecutableTool<*, *>> {
        val adapters = mutableListOf<ExecutableTool<*, *>>()
        
        discoveredTools.forEach { (serverName, tools) ->
            tools.filter { it.enabled }.forEach { toolInfo ->
                adapters.add(McpToolAdapter(toolInfo, serverName, clientManager))
            }
        }
        
        return adapters
    }
    
    /**
     * Create AgentTool representations for discovered MCP tools
     * Used for displaying in UI and prompts
     */
    fun createAgentTools(
        discoveredTools: Map<String, List<McpToolInfo>>
    ): List<AgentTool> {
        val agentTools = mutableListOf<AgentTool>()
        
        discoveredTools.forEach { (serverName, tools) ->
            tools.filter { it.enabled }.forEach { toolInfo ->
                agentTools.add(
                    AgentTool(
                        name = "${serverName}_${toolInfo.name}",
                        description = toolInfo.description,
                        isMcp = true,
                        mcpGroup = serverName
                    )
                )
            }
        }
        
        return agentTools
    }
}
