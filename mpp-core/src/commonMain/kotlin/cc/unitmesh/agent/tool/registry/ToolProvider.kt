package cc.unitmesh.agent.tool.registry

import cc.unitmesh.agent.core.SubAgentManager
import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.llm.KoogLLMService

data class ToolDependencies(
    val fileSystem: ToolFileSystem,
    val shellExecutor: ShellExecutor,
    val subAgentManager: SubAgentManager? = null,
    val llmService: KoogLLMService? = null
)

interface ToolProvider {
    fun provide(dependencies: ToolDependencies): List<ExecutableTool<*, *>>
    
    fun priority(): Int = 0
}

