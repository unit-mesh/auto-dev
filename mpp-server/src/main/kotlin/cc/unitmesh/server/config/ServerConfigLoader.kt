package cc.unitmesh.server.config

import cc.unitmesh.llm.NamedModelConfig
import cc.unitmesh.yaml.YamlUtils
import java.io.File

/**
 * Server-side configuration loader
 * Loads LLM configuration from ~/.autodev/config.yaml
 */
object ServerConfigLoader {
    private val homeDir = System.getProperty("user.home")
    private val configDir = File(homeDir, ".autodev")
    private val configFile = File(configDir, "config.yaml")

    /**
     * Load active configuration from ~/.autodev/config.yaml
     * Returns null if file doesn't exist or no active config is set
     */
    fun loadActiveConfig(): NamedModelConfig? {
        if (!configFile.exists()) {
            println("📝 Config file not found: ${configFile.absolutePath}")
            return null
        }

        return try {
            val content = configFile.readText()
            val yamlData = YamlUtils.load(content)

            if (yamlData == null) {
                println("⚠️  Failed to parse config.yaml")
                return null
            }

            // Get active config name
            val activeName = yamlData["active"] as? String
            if (activeName == null) {
                println("⚠️  No active configuration set in config.yaml")
                return null
            }

            // Get configs list
            @Suppress("UNCHECKED_CAST")
            val configs = yamlData["configs"] as? List<Map<String, Any>>
            if (configs == null) {
                println("⚠️  No configurations found in config.yaml")
                return null
            }

            // Find active config
            val activeConfigMap = configs.firstOrNull { (it["name"] as? String) == activeName }
            if (activeConfigMap == null) {
                println("⚠️  Active configuration '$activeName' not found in configs")
                return null
            }

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
            println("❌ Failed to parse config file: ${e.message}")
            e.printStackTrace()
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

