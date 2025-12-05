package cc.unitmesh.devti.vcs.gitignore

import cc.unitmesh.devti.settings.coder.coderSetting
import com.intellij.openapi.project.Project

/**
 * Dual-engine wrapper that switches between custom and third-party gitignore engines
 * based on feature flag configuration. This provides a safety net for the custom engine
 * while allowing for A/B testing and gradual rollout.
 */
class GitIgnoreFlagWrapper(
    private val project: Project,
    gitIgnoreContent: String = ""
) : IgnoreEngine {
    
    private val homeSpunEngine: IgnoreEngine = IgnoreEngineFactory.createEngine(IgnoreEngineFactory.EngineType.HOMESPUN)
    private val basjesEngine: IgnoreEngine = IgnoreEngineFactory.createEngine(IgnoreEngineFactory.EngineType.BASJES)
    
    init {
        if (gitIgnoreContent.isNotEmpty()) {
            loadFromContent(gitIgnoreContent)
        }
    }
    
    /**
     * Determines which engine to use based on the feature flag setting.
     *
     * @return the active engine instance
     */
    private fun getActiveEngine(): IgnoreEngine {
        return if (project.coderSetting.state.enableHomeSpunGitIgnore) {
            homeSpunEngine
        } else {
            basjesEngine
        }
    }
    
    /**
     * Gets the currently active engine type for monitoring/debugging.
     *
     * @return the active engine type
     */
    fun getActiveEngineType(): IgnoreEngineFactory.EngineType {
        return if (project.coderSetting.state.enableHomeSpunGitIgnore) {
            IgnoreEngineFactory.EngineType.HOMESPUN
        } else {
            IgnoreEngineFactory.EngineType.BASJES
        }
    }
    
    override fun isIgnored(filePath: String): Boolean {
        return try {
            getActiveEngine().isIgnored(filePath)
        } catch (e: Exception) {
            // If the active engine fails, fall back to the other engine
            val fallbackEngine = if (project.coderSetting.state.enableHomeSpunGitIgnore) {
                basjesEngine
            } else {
                homeSpunEngine
            }
            
            // Log the error (in a real implementation, use proper logging)
            System.err.println("Warning: Active gitignore engine failed, falling back. Error: ${e.message}")
            
            try {
                fallbackEngine.isIgnored(filePath)
            } catch (fallbackException: Exception) {
                // If both engines fail, default to not ignored
                System.err.println("Error: Both gitignore engines failed. Defaulting to not ignored. Error: ${fallbackException.message}")
                false
            }
        }
    }
    
    override fun addRule(pattern: String) {
        // Add to both engines to keep them in sync
        try {
            homeSpunEngine.addRule(pattern)
        } catch (e: Exception) {
            System.err.println("Warning: Failed to add rule to homespun engine: ${e.message}")
        }
        
        try {
            basjesEngine.addRule(pattern)
        } catch (e: Exception) {
            System.err.println("Warning: Failed to add rule to basjes engine: ${e.message}")
        }
    }
    
    override fun removeRule(pattern: String) {
        // Remove from both engines to keep them in sync
        try {
            homeSpunEngine.removeRule(pattern)
        } catch (e: Exception) {
            System.err.println("Warning: Failed to remove rule from homespun engine: ${e.message}")
        }
        
        try {
            basjesEngine.removeRule(pattern)
        } catch (e: Exception) {
            System.err.println("Warning: Failed to remove rule from basjes engine: ${e.message}")
        }
    }
    
    override fun getRules(): List<String> {
        return getActiveEngine().getRules()
    }
    
    override fun clearRules() {
        homeSpunEngine.clearRules()
        basjesEngine.clearRules()
    }
    
    override fun loadFromContent(gitIgnoreContent: String) {
        // Load into both engines to keep them in sync
        try {
            homeSpunEngine.loadFromContent(gitIgnoreContent)
        } catch (e: Exception) {
            System.err.println("Warning: Failed to load content into homespun engine: ${e.message}")
        }
        
        try {
            basjesEngine.loadFromContent(gitIgnoreContent)
        } catch (e: Exception) {
            System.err.println("Warning: Failed to load content into basjes engine: ${e.message}")
        }
    }
    
    /**
     * Gets statistics from both engines for monitoring and debugging.
     *
     * @return a map containing statistics from both engines
     */
    fun getStatistics(): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()
        
        stats["activeEngine"] = getActiveEngineType().name
        
        try {
            if (homeSpunEngine is HomeSpunIgnoreEngine) {
                stats["homeSpun"] = homeSpunEngine.getStatistics()
            }
        } catch (e: Exception) {
            stats["homeSpunError"] = e.message ?: "Unknown error"
        }
        
        try {
            if (basjesEngine is BasjesIgnoreEngine) {
                stats["basjes"] = basjesEngine.getStatistics()
            }
        } catch (e: Exception) {
            stats["basjesError"] = e.message ?: "Unknown error"
        }
        
        return stats
    }
}
