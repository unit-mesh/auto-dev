package cc.unitmesh.agent.tool.provider

import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.impl.*

/**
 * Provider for built-in tools.
 * 
 * This is the single source of truth for all built-in tools.
 * To add a new tool, simply add it to the list in the provide() method.
 */
class BuiltinToolsProvider : ToolProvider {
    
    override fun priority(): Int = 100 // System provider, high priority
    
    override fun provide(dependencies: ToolDependencies): List<ExecutableTool<*, *>> {
        val tools = mutableListOf<ExecutableTool<*, *>>()
        
        // File system tools
        tools.add(ReadFileTool(dependencies.fileSystem))
        tools.add(WriteFileTool(dependencies.fileSystem))
        tools.add(EditFileTool(dependencies.fileSystem))
        
        // Search tools
        tools.add(GrepTool(dependencies.fileSystem))
        tools.add(GlobTool(dependencies.fileSystem))
        
        // Execution tools (only if shell executor is available)
        if (dependencies.shellExecutor.isAvailable()) {
            tools.add(ShellTool(dependencies.shellExecutor))
        }
        
        // Communication tools (only if SubAgentManager is available)
        if (dependencies.subAgentManager != null) {
            tools.add(AskAgentTool(dependencies.subAgentManager))
        }
        
        // Web tools (only if LLM service and HTTP fetcher are available)
        if (dependencies.llmService != null && dependencies.httpFetcher != null) {
            tools.add(WebFetchTool(dependencies.llmService, dependencies.httpFetcher))
        }
        
        return tools
    }
}

