package cc.unitmesh.agent.mcp

import cc.unitmesh.agent.tool.AgentTool

/**
 * Adapter to convert MCP tools to AgentTool interface
 * 
 * This allows MCP tools to be used seamlessly with the existing agent system.
 */
object McpToolAdapter {
    /**
     * Convert McpToolInfo to AgentTool
     */
    fun toAgentTool(toolInfo: McpToolInfo): AgentTool {
        return AgentTool(
            name = toolInfo.name,
            description = toolInfo.description,
            example = "",
            isMcp = true,
            mcpGroup = toolInfo.serverName,
            completion = toolInfo.name
        )
    }
    
    /**
     * Convert a list of McpToolInfo to AgentTools
     */
    fun toAgentTools(tools: List<McpToolInfo>): List<AgentTool> {
        return tools.map { toAgentTool(it) }
    }
    
    /**
     * Convert a map of server name to tools to a flat list of AgentTools
     */
    fun toAgentTools(toolsMap: Map<String, List<McpToolInfo>>): List<AgentTool> {
        return toolsMap.values.flatten().map { toAgentTool(it) }
    }
    
    /**
     * Filter enabled tools from a map
     */
    fun getEnabledTools(toolsMap: Map<String, List<McpToolInfo>>): List<AgentTool> {
        return toolsMap.values.flatten()
            .filter { it.enabled }
            .map { toAgentTool(it) }
    }
}

