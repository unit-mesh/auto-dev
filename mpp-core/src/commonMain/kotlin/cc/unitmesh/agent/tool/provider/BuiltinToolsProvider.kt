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

        tools.add(ReadFileTool(dependencies.fileSystem))
        tools.add(WriteFileTool(dependencies.fileSystem))
        tools.add(EditFileTool(dependencies.fileSystem))

        // Search tools
        tools.add(GrepTool(dependencies.fileSystem))
        tools.add(GlobTool(dependencies.fileSystem))

        if (dependencies.shellExecutor.isAvailable()) {
            tools.add(ShellTool(dependencies.shellExecutor))
        } else {
            println("   ⚠️  Shell execution not available, skipping ShellTool")
        }

        if (dependencies.subAgentManager != null) {
            tools.add(AskAgentTool(dependencies.subAgentManager))
        } else {
            println("   ⚠️  SubAgentManager not available, skipping AskAgentTool")
        }

        if (dependencies.llmService != null) {
            tools.add(WebFetchTool(dependencies.llmService))
        } else {
            println("   ⚠️  LLMService not available, skipping WebFetchTool")
        }

        return tools
    }
}

