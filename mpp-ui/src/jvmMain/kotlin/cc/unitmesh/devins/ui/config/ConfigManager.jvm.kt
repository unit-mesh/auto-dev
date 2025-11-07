package cc.unitmesh.devins.ui.config

import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.llm.NamedModelConfig
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
     * Simple YAML parser for config file
     * Supports the basic structure we need
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

        val lines = content.lines().filter { it.isNotBlank() }
        var active = ""
        val configs = mutableListOf<NamedModelConfig>()
        val mcpServers = mutableMapOf<String, McpServerConfig>()
        var currentConfig: MutableMap<String, String>? = null
        var currentMcpServer: String? = null
        var currentMcpConfig: MutableMap<String, Any>? = null
        var parsingMode = "root"

        for (line in lines) {
            val trimmed = line.trim()

            // Skip comments
            if (trimmed.startsWith("#")) continue

            when {
                trimmed.startsWith("active:") -> {
                    active = trimmed.substringAfter("active:").trim()
                    parsingMode = "root"
                }
                trimmed.startsWith("configs:") -> {
                    parsingMode = "configs"
                    continue
                }
                trimmed.startsWith("mcpServers:") -> {
                    parsingMode = "mcpServers"
                    continue
                }
                parsingMode == "configs" && (trimmed.startsWith("- name:") || trimmed.startsWith("  - name:")) -> {
                    // Save previous config if exists
                    currentConfig?.let { configs.add(configMapToNamedConfig(it)) }
                    // Start new config
                    currentConfig = mutableMapOf("name" to trimmed.substringAfter("name:").trim())
                }
                parsingMode == "configs" && trimmed.contains(":") && currentConfig != null -> {
                    // Config property
                    val key = trimmed.substringBefore(":").trim()
                    val value = trimmed.substringAfter(":").trim()
                    currentConfig[key] = value
                }
                parsingMode == "mcpServers" && !trimmed.startsWith("-") && trimmed.contains(":") && !trimmed.contains("  ") -> {
                    // Save previous MCP server if exists
                    currentMcpServer?.let { serverName ->
                        currentMcpConfig?.let { mcpServers[serverName] = mcpConfigMapToServerConfig(it) }
                    }
                    // Start new MCP server
                    currentMcpServer = trimmed.substringBefore(":").trim()
                    currentMcpConfig = mutableMapOf()
                }
                parsingMode == "mcpServers" && trimmed.contains(":") && currentMcpConfig != null -> {
                    // MCP server property
                    val key = trimmed.substringBefore(":").trim()
                    val value = trimmed.substringAfter(":").trim()

                    when (key) {
                        "args", "autoApprove" -> {
                            // Arrays - parse simple bracket notation
                            val arrayStr = value.removePrefix("[").removeSuffix("]")
                            val items = if (arrayStr.isNotEmpty()) {
                                arrayStr.split(",").map { it.trim().removeSurrounding("\"") }
                            } else {
                                emptyList()
                            }
                            currentMcpConfig[key] = items
                        }
                        "disabled" -> currentMcpConfig[key] = value.toBoolean()
                        else -> currentMcpConfig[key] = value.removeSurrounding("\"")
                    }
                }
            }
        }

        // Don't forget the last config
        currentConfig?.let { configs.add(configMapToNamedConfig(it)) }
        currentMcpServer?.let { serverName ->
            currentMcpConfig?.let { mcpServers[serverName] = mcpConfigMapToServerConfig(it) }
        }

        return ConfigFile(active = active, configs = configs, mcpServers = mcpServers)
    }

    private fun configMapToNamedConfig(map: Map<String, String>): NamedModelConfig {
        return NamedModelConfig(
            name = map["name"] ?: "",
            provider = map["provider"] ?: "openai",
            apiKey = map["apiKey"] ?: "",
            model = map["model"] ?: "",
            baseUrl = map["baseUrl"] ?: "",
            temperature = map["temperature"]?.toDoubleOrNull() ?: 0.0,
            maxTokens = map["maxTokens"]?.toIntOrNull() ?: 128000
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mcpConfigMapToServerConfig(map: Map<String, Any>): McpServerConfig {
        return McpServerConfig(
            command = map["command"] as? String,
            url = map["url"] as? String,
            args = (map["args"] as? List<String>) ?: emptyList(),
            disabled = (map["disabled"] as? Boolean) ?: false,
            autoApprove = (map["autoApprove"] as? List<String>)
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
                if (config.temperature != 0.0) {
                    appendLine("    temperature: ${config.temperature}")
                }
                if (config.maxTokens != 128000) {
                    appendLine("    maxTokens: ${config.maxTokens}")
                }
            }

            // Add MCP servers configuration
            if (configFile.mcpServers.isNotEmpty()) {
                appendLine("mcpServers:")
                configFile.mcpServers.forEach { (name, config) ->
                    appendLine("  $name:")
                    config.command?.let { appendLine("    command: \"$it\"") }
                    config.url?.let { appendLine("    url: \"$it\"") }
                    if (config.args.isNotEmpty()) {
                        val argsStr = config.args.joinToString(", ") { "\"$it\"" }
                        appendLine("    args: [$argsStr]")
                    }
                    if (config.disabled) {
                        appendLine("    disabled: true")
                    }
                    config.autoApprove?.let { autoApprove ->
                        if (autoApprove.isNotEmpty()) {
                            val autoApproveStr = autoApprove.joinToString(", ") { "\"$it\"" }
                            appendLine("    autoApprove: [$autoApproveStr]")
                        } else {
                            appendLine("    autoApprove: []")
                        }
                    }
                }
            }
        }
}

