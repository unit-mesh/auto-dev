package cc.unitmesh.agent.tool.provider

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

object ToolProviderRegistry {
    private val providers = mutableListOf<ToolProvider>()
    
    fun register(provider: ToolProvider) {
        providers.add(provider)
        providers.sortByDescending { it.priority() }
    }

    fun getProviders(): List<ToolProvider> = providers.toList()

    fun clear() {
        providers.clear()
    }

    fun discoverTools(dependencies: ToolDependencies): List<ExecutableTool<*, *>> {
        return providers.flatMap { it.provide(dependencies) }
    }
}

