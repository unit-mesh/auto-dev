package cc.unitmesh.xuiper.config

import cc.unitmesh.llm.NamedModelConfig
import cc.unitmesh.yaml.YamlUtils
import java.io.File

/**
 * Configuration loader for xuiper-ui evaluation.
 * Loads LLM configuration from ~/.autodev/config.yaml
 */
object ConfigLoader {
    private val homeDir = System.getProperty("user.home")
    private val configDir = File(homeDir, ".autodev")
    private val configFile = File(configDir, "config.yaml")

    /**
     * Load active configuration from ~/.autodev/config.yaml
     * Returns null if file doesn't exist or no active config is set
     */
    fun loadActiveConfig(): NamedModelConfig? {
        if (!configFile.exists()) {
            return null
        }

        return try {
            val content = configFile.readText()
            val yamlData = YamlUtils.load(content) ?: return null

            // Get active config name
            val activeName = yamlData["active"] as? String ?: return null

            // Get configs list
            @Suppress("UNCHECKED_CAST")
            val configs = yamlData["configs"] as? List<Map<String, Any>> ?: return null

            // Find active config
            val activeConfigMap = configs.firstOrNull { (it["name"] as? String) == activeName }
                ?: return null

            // Parse to NamedModelConfig
            NamedModelConfig(
                name = activeConfigMap["name"] as? String ?: activeName,
                provider = activeConfigMap["provider"] as? String ?: "openai",
                apiKey = activeConfigMap["apiKey"] as? String ?: "",
                model = activeConfigMap["model"] as? String ?: "gpt-4",
                baseUrl = activeConfigMap["baseUrl"] as? String ?: "",
                temperature = (activeConfigMap["temperature"] as? Number)?.toDouble() ?: 0.7,
                maxTokens = (activeConfigMap["maxTokens"] as? Number)?.toInt() ?: 8192
            )
        } catch (e: Exception) {
            println("Warning: Failed to parse config file: ${e.message}")
            null
        }
    }

    /**
     * Check if config file exists
     */
    fun exists(): Boolean = configFile.exists()

    /**
     * Get config file path
     */
    fun getConfigPath(): String = configFile.absolutePath
}

