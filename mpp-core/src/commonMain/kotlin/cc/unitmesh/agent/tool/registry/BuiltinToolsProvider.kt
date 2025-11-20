package cc.unitmesh.agent.tool.registry

import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.impl.AskAgentTool
import cc.unitmesh.agent.tool.impl.EditFileTool
import cc.unitmesh.agent.tool.impl.GlobTool
import cc.unitmesh.agent.tool.impl.GrepTool
import cc.unitmesh.agent.tool.impl.ReadFileTool
import cc.unitmesh.agent.tool.impl.ShellTool
import cc.unitmesh.agent.tool.impl.TaskBoundaryTool
import cc.unitmesh.agent.tool.impl.WebFetchTool
import cc.unitmesh.agent.tool.impl.WriteFileTool

/**
 * Provider for built-in tools.
 *
 * This is the single source of truth for all built-in tools.
 * To add a new tool, simply add it to the list in the provide() method.
 *
 * remember to implmentation execute in @ToolOrchestrator
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
        
        // GlobTool with AnalysisAgent support for auto-summarization of large results
        val analysisAgent = dependencies.subAgentManager?.getSubAgent<cc.unitmesh.agent.subagent.ContentHandlerContext, cc.unitmesh.agent.tool.ToolResult.AgentResult>("analysis-agent") as? cc.unitmesh.agent.subagent.AnalysisAgent
        tools.add(GlobTool(dependencies.fileSystem, analysisAgent))

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

        tools.add(WebFetchTool(dependencies.llmService))
        
        // Task management tool
        tools.add(TaskBoundaryTool())

        return tools
    }
}