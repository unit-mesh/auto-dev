package cc.unitmesh.devins.ui.config

import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.mcp.McpServerConfig
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json

// Check if we're in Node.js environment
private val isNodeJs: Boolean = js("typeof process !== 'undefined' && process.versions && process.versions.node") as Boolean

// External Node.js modules (conditionally loaded)
private val fsModule: dynamic = if (isNodeJs) js("require('fs')") else null
private val fsPromises: dynamic = if (isNodeJs) js("require('fs/promises')") else null
private val pathModule: dynamic = if (isNodeJs) js("require('path')") else null
private val osModule: dynamic = if (isNodeJs) js("require('os')") else null

/**
 * JS implementation of ConfigManager
 * Uses Node.js fs module for file operations
 * This implementation is called by TypeScript code
 */
actual object ConfigManager {
    private val homeDir: String = if (isNodeJs) {
        osModule.homedir() as String
    } else {
        "/tmp" // Fallback for browser environment
    }

    private val configDir: String = if (isNodeJs) {
        pathModule.join(homeDir, ".autodev") as String
    } else {
        "/tmp/.autodev" // Fallback for browser environment
    }

    private val configFilePath: String = if (isNodeJs) {
        pathModule.join(configDir, "config.yaml") as String
    } else {
        "/tmp/.autodev/config.yaml" // Fallback for browser environment
    }

    private val toolConfigFilePath: String = if (isNodeJs) {
        pathModule.join(configDir, "mcp.json") as String
    } else {
        "/tmp/.autodev/mcp.json" // Fallback for browser environment
    }

    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

    actual suspend fun load(): AutoDevConfigWrapper {
        return try {
            if (!isNodeJs) {
                console.warn("Config loading not supported in browser environment")
                return createEmpty()
            }

            // Check if file exists
            val exists =
                try {
                    fsPromises.access(configFilePath).await()
                    true
                } catch (e: dynamic) {
                    false
                }

            if (!exists) {
                return createEmpty()
            }

            // Read file
            val content = fsPromises.readFile(configFilePath, "utf-8").await() as String

            // Parse YAML content
            val configFileData = parseYamlConfig(content)

            AutoDevConfigWrapper(configFileData)
        } catch (e: Throwable) {
            console.error("Error loading config:", e)
            createEmpty()
        }
    }

    actual suspend fun save(configFile: ConfigFile) {
        try {
            if (!isNodeJs) {
                console.warn("Config saving not supported in browser environment")
                return
            }

            // Ensure directory exists
            try {
                fsPromises.mkdir(configDir, js("{ recursive: true }")).await()
            } catch (e: dynamic) {
                // Directory might already exist
            }

            // Convert to YAML
            val yamlContent = toYaml(configFile)

            // Write file
            fsPromises.writeFile(configFilePath, yamlContent, "utf-8").await()
        } catch (e: Throwable) {
            console.error("Error saving config:", e)
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

    actual fun getConfigPath(): String = configFilePath

    actual suspend fun exists(): Boolean {
        return try {
            fsPromises.access(configFilePath).await()
            true
        } catch (e: dynamic) {
            false
        }
    }

    actual suspend fun saveMcpServers(mcpServers: Map<String, McpServerConfig>) {
        val wrapper = load()
        val configFile = wrapper.getConfigFile()

        val updatedConfigFile = configFile.copy(mcpServers = mcpServers)
        save(updatedConfigFile)
    }

    actual suspend fun loadToolConfig(): ToolConfigFile {
        return try {
            if (!isNodeJs) {
                console.warn("Tool config loading not supported in browser environment")
                return ToolConfigFile.default()
            }

            // Check if file exists
            val exists =
                try {
                    fsPromises.access(toolConfigFilePath).await()
                    true
                } catch (e: dynamic) {
                    false
                }

            if (!exists) {
                return ToolConfigFile.default()
            }

            // Read file
            val content = fsPromises.readFile(toolConfigFilePath, "utf-8").await() as String

            // Parse JSON
            json.decodeFromString<ToolConfigFile>(content)
        } catch (e: Throwable) {
            console.error("Error loading tool config:", e)
            ToolConfigFile.default()
        }
    }

    actual suspend fun saveToolConfig(toolConfig: ToolConfigFile) {
        try {
            if (!isNodeJs) {
                console.warn("Tool config saving not supported in browser environment")
                return
            }

            // Ensure directory exists
            try {
                fsPromises.mkdir(configDir, js("{ recursive: true }")).await()
            } catch (e: dynamic) {
                // Directory might already exist
            }

            // Write JSON
            val jsonContent = json.encodeToString(ToolConfigFile.serializer(), toolConfig)
            fsPromises.writeFile(toolConfigFilePath, jsonContent, "utf-8").await()
        } catch (e: Throwable) {
            console.error("Error saving tool config:", e)
            throw e
        }
    }

    actual fun getToolConfigPath(): String = toolConfigFilePath

    private fun createEmpty(): AutoDevConfigWrapper {
        return AutoDevConfigWrapper(ConfigFile(active = "", configs = emptyList()))
    }

    /**
     * Parse YAML config file
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
            temperature = map["temperature"]?.toDoubleOrNull() ?: 0.7,
            maxTokens = map["maxTokens"]?.toIntOrNull() ?: 8192
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
                if (config.temperature != 0.7) {
                    appendLine("    temperature: ${config.temperature}")
                }
                if (config.maxTokens != 8192) {
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
