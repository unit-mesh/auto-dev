package cc.unitmesh.devins.ui.config

import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.llm.NamedModelConfig
import cc.unitmesh.yaml.YamlUtils
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
    private val toolConfigFile = File(configDir, "mcp.json")

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
        val configFile = wrapper.configFile

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

    /**
     * Generate a unique configuration name by appending -1, -2, etc. if the name already exists
     *
     * @param baseName The desired configuration name
     * @param existingNames List of existing configuration names
     * @return A unique name (either baseName or baseName-1, baseName-2, etc.)
     */
    actual fun generateUniqueConfigName(baseName: String, existingNames: List<String>): String {
        if (baseName !in existingNames) {
            return baseName
        }

        var counter = 1
        var uniqueName = "$baseName-$counter"
        while (uniqueName in existingNames) {
            counter++
            uniqueName = "$baseName-$counter"
        }
        return uniqueName
    }

    actual suspend fun deleteConfig(name: String) {
        val wrapper = load()
        val configFile = wrapper.configFile

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
        val configFile = wrapper.configFile

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

    actual suspend fun saveMcpServers(mcpServers: Map<String, McpServerConfig>) {
        val wrapper = load()
        val configFile = wrapper.configFile

        val updatedConfigFile = configFile.copy(mcpServers = mcpServers)
        save(updatedConfigFile)
    }

    actual suspend fun loadToolConfig(): ToolConfigFile =
        withContext(Dispatchers.IO) {
            try {
                if (!toolConfigFile.exists()) {
                    return@withContext ToolConfigFile.default()
                }

                val content = toolConfigFile.readText()
                json.decodeFromString<ToolConfigFile>(content)
            } catch (e: Exception) {
                println("Error loading tool config: ${e.message}")
                ToolConfigFile.default()
            }
        }

    actual suspend fun saveToolConfig(toolConfig: ToolConfigFile) =
        withContext(Dispatchers.IO) {
            try {
                // Ensure directory exists
                configDir.mkdirs()

                // Write JSON
                val jsonContent = json.encodeToString(ToolConfigFile.serializer(), toolConfig)
                toolConfigFile.writeText(jsonContent)
            } catch (e: Exception) {
                println("Error saving tool config: ${e.message}")
                throw e
            }
        }

    actual fun getToolConfigPath(): String = toolConfigFile.absolutePath

    private fun createEmpty(): AutoDevConfigWrapper {
        return AutoDevConfigWrapper(ConfigFile(active = "", configs = emptyList()))
    }

    /**
     * Parse YAML config file using YamlUtils
     */
    private fun parseYamlConfig(content: String): ConfigFile {
        // Try to parse as JSON first (for MCP config compatibility)
        try {
            if (content.trim().startsWith("{")) {
                return json.decodeFromString<ConfigFile>(content)
            }
        } catch (e: Exception) {
            // Fall through to YAML parsing
        }

        // Use YamlUtils for proper YAML parsing
        return YamlUtils.loadAs<ConfigFile>(content, kotlinx.serialization.serializer())
    }

    private fun toYaml(configFile: ConfigFile): String {
        return YamlUtils.dump(configFile, kotlinx.serialization.serializer())
    }
}
