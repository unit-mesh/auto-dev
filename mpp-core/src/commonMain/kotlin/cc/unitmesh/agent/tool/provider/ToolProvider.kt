package cc.unitmesh.agent.tool.provider

import cc.unitmesh.agent.core.SubAgentManager
import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.impl.HttpFetcher
import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.llm.KoogLLMService

/**
 * Dependencies required for tool creation
 */
data class ToolDependencies(
    val fileSystem: ToolFileSystem,
    val shellExecutor: ShellExecutor,
    val subAgentManager: SubAgentManager? = null,
    val llmService: KoogLLMService? = null,
    val httpFetcher: HttpFetcher? = null
)

/**
 * Provider interface for discovering and providing tools.
 * 
 * This enables plugin-style tool registration where tool providers
 * can be easily added or removed without modifying core code.
 */
interface ToolProvider {
    /**
     * Provide a list of tools based on available dependencies
     * 
     * @param dependencies All available dependencies for tool creation
     * @return List of executable tools
     */
    fun provide(dependencies: ToolDependencies): List<ExecutableTool<*, *>>
    
    /**
     * Legacy method for backward compatibility
     */
    fun provide(
        fileSystem: ToolFileSystem,
        shellExecutor: ShellExecutor,
        subAgentManager: SubAgentManager?
    ): List<ExecutableTool<*, *>> {
        return provide(ToolDependencies(fileSystem, shellExecutor, subAgentManager))
    }
    
    /**
     * Priority of this provider (higher priority providers are loaded first)
     * Default is 0, system providers should use 100+
     */
    fun priority(): Int = 0
}

/**
 * Registry for managing tool providers
 */
object ToolProviderRegistry {
    private val providers = mutableListOf<ToolProvider>()
    
    /**
     * Register a tool provider
     */
    fun register(provider: ToolProvider) {
        providers.add(provider)
        // Sort by priority (descending)
        providers.sortByDescending { it.priority() }
    }
    
    /**
     * Get all registered providers
     */
    fun getProviders(): List<ToolProvider> = providers.toList()
    
    /**
     * Clear all providers (mainly for testing)
     */
    fun clear() {
        providers.clear()
    }
    
    /**
     * Discover all tools from registered providers
     */
    fun discoverTools(dependencies: ToolDependencies): List<ExecutableTool<*, *>> {
        return providers.flatMap { it.provide(dependencies) }
    }
    
    /**
     * Legacy method for backward compatibility
     */
    fun discoverTools(
        fileSystem: ToolFileSystem,
        shellExecutor: ShellExecutor,
        subAgentManager: SubAgentManager?
    ): List<ExecutableTool<*, *>> {
        return discoverTools(ToolDependencies(fileSystem, shellExecutor, subAgentManager))
    }
}

