package cc.unitmesh.agent.tool.provider

import cc.unitmesh.agent.core.SubAgentManager
import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.impl.*
import cc.unitmesh.agent.tool.shell.ShellExecutor

/**
 * Provider for built-in tools.
 * 
 * This is the single source of truth for all built-in tools.
 * To add a new tool, simply add it to the list in the provide() method.
 */
class BuiltinToolsProvider : ToolProvider {
    
    override fun priority(): Int = 100 // System provider, high priority
    
    override fun provide(
        fileSystem: ToolFileSystem,
        shellExecutor: ShellExecutor,
        subAgentManager: SubAgentManager?
    ): List<ExecutableTool<*, *>> {
        val tools = mutableListOf<ExecutableTool<*, *>>()
        
        // File system tools
        tools.add(ReadFileTool(fileSystem))
        tools.add(WriteFileTool(fileSystem))
        tools.add(EditFileTool(fileSystem))
        
        // Search tools
        tools.add(GrepTool(fileSystem))
        tools.add(GlobTool(fileSystem))
        
        // Execution tools (only if shell executor is available)
        if (shellExecutor.isAvailable()) {
            tools.add(ShellTool(shellExecutor))
        }
        
        // Communication tools (only if SubAgentManager is available)
        if (subAgentManager != null) {
            tools.add(AskAgentTool(subAgentManager))
        }
        
        return tools
    }
}

