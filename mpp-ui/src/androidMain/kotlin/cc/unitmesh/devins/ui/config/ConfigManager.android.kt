package cc.unitmesh.devins.ui.config

import android.content.Context
import cc.unitmesh.agent.mcp.McpServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Android implementation of ConfigManager
 * Uses Android Context for proper file storage location
 * 
 * Best Practice: Use app-specific internal storage
 * - Files are private to the app
 * - Automatically cleaned up when app is uninstalled
 * - No special permissions required
 */
actual object ConfigManager {
    private var appContext: Context? = null
    
    /**
     * Initialize ConfigManager with Android Context
     * Should be called once in Application.onCreate()
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
    
    private fun getConfigDir(): File {
        val context = appContext 
            ?: throw IllegalStateException("ConfigManager not initialized. Call ConfigManager.initialize(context) first.")
        
        // Use app-specific internal storage directory
        // Path: /data/data/your.package.name/files/.autodev/
        return File(context.filesDir, ".autodev")
    }
    
    private fun getConfigFile(): File {
        return File(getConfigDir(), "config.yaml")
    }
    
    // JSON parser for potential JSON config support
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    actual suspend fun load(): AutoDevConfigWrapper =
        withContext(Dispatchers.IO) {
            try {
                val file = getConfigFile()
                if (!file.exists()) {
                    return@withContext createEmpty()
                }

                val content = file.readText()
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
                val dir = getConfigDir()
                dir.mkdirs()
                
                val file = getConfigFile()
                val yamlContent = toYaml(configFile)
                file.writeText(yamlContent)
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

    actual fun getConfigPath(): String = getConfigFile().absolutePath

    actual suspend fun exists(): Boolean =
        withContext(Dispatchers.IO) {
            getConfigFile().exists()
        }
    
    actual suspend fun saveMcpServers(mcpServers: Map<String, McpServerConfig>) {
        val wrapper = load()
        val configFile = wrapper.getConfigFile()
        
        val updatedConfigFile = configFile.copy(mcpServers = mcpServers)
        save(updatedConfigFile)
    }

    private fun createEmpty(): AutoDevConfigWrapper {
        return AutoDevConfigWrapper(ConfigFile(active = "", configs = emptyList()))
    }

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
                    currentConfig?.let { configs.add(configMapToNamedConfig(it)) }
                    currentConfig = mutableMapOf("name" to trimmed.substringAfter("name:").trim())
                }
                parsingMode == "configs" && trimmed.contains(":") && currentConfig != null -> {
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
                    val key = trimmed.substringBefore(":").trim()
                    val value = trimmed.substringAfter(":").trim()
                    
                    when (key) {
                        "args", "autoApprove" -> {
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
            maxTokens = map["maxTokens"]?.toIntOrNull() ?: 4096
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
