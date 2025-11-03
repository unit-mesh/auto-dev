package cc.unitmesh.devins.ui.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * JVM implementation of ConfigManager
 * Uses java.io.File for file operations
 */
actual object ConfigManager {
    private val homeDir = System.getProperty("user.home")
    private val configDir = File(homeDir, ".autodev")
    private val configFile = File(configDir, "config.yaml")

    // Simple YAML serializer (we'll use a simple format that's both JSON and YAML compatible)
    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

    actual suspend fun load(): AutoDevConfigWrapper =
        withContext(Dispatchers.IO) {
            try {
                if (!configFile.exists()) {
                    return@withContext createEmpty()
                }

                val content = configFile.readText()

                // Parse YAML-like content (simplified parser)
                val configFileData = parseYamlConfig(content)

                AutoDevConfigWrapper(configFileData)
            } catch (e: Exception) {
                println("Error loading config: ${e.message}")
                createEmpty()
            }
        }

    actual suspend fun save(configFile: ConfigFile) =
        withContext(Dispatchers.IO) {
            try {
                // Ensure directory exists
                configDir.mkdirs()

                // Convert to YAML format
                val yamlContent = toYaml(configFile)
                this@ConfigManager.configFile.writeText(yamlContent)
            } catch (e: Exception) {
                println("Error saving config: ${e.message}")
                throw e
            }
        }

    actual suspend fun saveConfig(
        config: NamedModelConfig,
        setActive: Boolean
    ) {
        val wrapper = load()
        val configFile = wrapper.getConfigFile()

        val existingIndex = configFile.configs.indexOfFirst { it.name == config.name }

        val updatedConfigs =
            if (existingIndex >= 0) {
                configFile.configs.toMutableList().apply { set(existingIndex, config) }
            } else {
                configFile.configs + config
            }

        val updatedConfigFile =
            configFile.copy(
                active = if (setActive) config.name else configFile.active,
                configs = updatedConfigs
            )

        save(updatedConfigFile)
    }

    actual suspend fun deleteConfig(name: String) {
        val wrapper = load()
        val configFile = wrapper.getConfigFile()

        val updatedConfigs = configFile.configs.filter { it.name != name }
        val updatedActive =
            if (configFile.active == name && updatedConfigs.isNotEmpty()) {
                updatedConfigs.first().name
            } else {
                configFile.active
            }

        save(configFile.copy(active = updatedActive, configs = updatedConfigs))
    }

    actual suspend fun setActive(name: String) {
        val wrapper = load()
        val configFile = wrapper.getConfigFile()

        if (configFile.configs.none { it.name == name }) {
            throw IllegalArgumentException("Configuration '$name' not found")
        }

        save(configFile.copy(active = name))
    }

    actual fun getConfigPath(): String = configFile.absolutePath

    actual suspend fun exists(): Boolean =
        withContext(Dispatchers.IO) {
            configFile.exists()
        }

    private fun createEmpty(): AutoDevConfigWrapper {
        return AutoDevConfigWrapper(ConfigFile(active = "", configs = emptyList()))
    }

    /**
     * Simple YAML parser for config file
     * Supports the basic structure we need
     */
    private fun parseYamlConfig(content: String): ConfigFile {
        val lines = content.lines().filter { it.isNotBlank() }
        var active = ""
        val configs = mutableListOf<NamedModelConfig>()
        var currentConfig: MutableMap<String, String>? = null

        for (line in lines) {
            val trimmed = line.trim()

            // Skip comments
            if (trimmed.startsWith("#")) continue

            when {
                trimmed.startsWith("active:") -> {
                    active = trimmed.substringAfter("active:").trim()
                }
                trimmed.startsWith("configs:") -> {
                    // Start of configs array
                    continue
                }
                trimmed.startsWith("- name:") || trimmed.startsWith("  - name:") -> {
                    // Save previous config if exists
                    currentConfig?.let { configs.add(configMapToNamedConfig(it)) }
                    // Start new config
                    currentConfig = mutableMapOf("name" to trimmed.substringAfter("name:").trim())
                }
                trimmed.contains(":") && currentConfig != null -> {
                    // Config property
                    val key = trimmed.substringBefore(":").trim()
                    val value = trimmed.substringAfter(":").trim()
                    currentConfig[key] = value
                }
            }
        }

        // Don't forget the last config
        currentConfig?.let { configs.add(configMapToNamedConfig(it)) }

        return ConfigFile(active = active, configs = configs)
    }

    private fun configMapToNamedConfig(map: Map<String, String>): NamedModelConfig {
        return NamedModelConfig(
            name = map["name"] ?: "",
            provider = map["provider"] ?: "openai",
            apiKey = map["apiKey"] ?: "",
            model = map["model"] ?: "",
            baseUrl = map["baseUrl"] ?: "",
            temperature = map["temperature"]?.toDoubleOrNull() ?: 0.7,
            maxTokens = map["maxTokens"]?.toIntOrNull() ?: 4096
        )
    }

    /**
     * Convert ConfigFile to YAML format
     */
    private fun toYaml(configFile: ConfigFile): String =
        buildString {
            appendLine("active: ${configFile.active}")
            appendLine("configs:")

            configFile.configs.forEach { config ->
                appendLine("  - name: ${config.name}")
                appendLine("    provider: ${config.provider}")
                appendLine("    apiKey: ${config.apiKey}")
                appendLine("    model: ${config.model}")
                if (config.baseUrl.isNotEmpty()) {
                    appendLine("    baseUrl: ${config.baseUrl}")
                }
                if (config.temperature != 0.7) {
                    appendLine("    temperature: ${config.temperature}")
                }
                if (config.maxTokens != 4096) {
                    appendLine("    maxTokens: ${config.maxTokens}")
                }
            }
        }
}
