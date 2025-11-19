package cc.unitmesh.agent.tool.registry

import cc.unitmesh.agent.tool.ExecutableTool

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