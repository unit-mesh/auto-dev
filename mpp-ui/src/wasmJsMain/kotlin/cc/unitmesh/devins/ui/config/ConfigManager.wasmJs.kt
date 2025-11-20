package cc.unitmesh.devins.ui.config

import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.devins.ui.platform.BrowserStorage
import cc.unitmesh.devins.ui.platform.console
import cc.unitmesh.llm.NamedModelConfig
import cc.unitmesh.yaml.YamlUtils
import kotlinx.serialization.json.Json

/**
 * WASM implementation of ConfigManager
 * Uses browser localStorage for configuration storage
 */
actual object ConfigManager {
    private const val CONFIG_KEY = "autodev-config"
    private const val TOOL_CONFIG_KEY = "autodev-tool-config"

    private val configDir: String = "browser://localStorage"
    private val configFilePath: String = "$configDir/$CONFIG_KEY"
    private val toolConfigFilePath: String = "$configDir/$TOOL_CONFIG_KEY"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    actual suspend fun load(): AutoDevConfigWrapper {
        return try {
            val content = BrowserStorage.getItem(CONFIG_KEY)
            if (content == null) {
                console.log("WASM: No config found in localStorage, returning empty config")
                return createEmpty()
            }

            val configFileData = parseYamlConfig(content)
            AutoDevConfigWrapper(configFileData)
        } catch (e: Exception) {
            console.error("WASM: Error loading config: ${e.message}")
            createEmpty()
        }
    }

    actual suspend fun save(configFile: ConfigFile) {
        try {
            val yamlContent = toYaml(configFile)
            BrowserStorage.setItem(CONFIG_KEY, yamlContent)
            console.log("WASM: Config saved to localStorage")
        } catch (e: Exception) {
            console.error("WASM: Error saving config: ${e.message}")
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

        val updatedConfigs = if (existingIndex >= 0) {
            configFile.configs.toMutableList().apply { set(existingIndex, config) }
        } else {
            configFile.configs + config
        }

        val updatedConfigFile = configFile.copy(
            active = if (setActive) config.name else configFile.active,
            configs = updatedConfigs
        )

        save(updatedConfigFile)
    }

    actual suspend fun deleteConfig(name: String) {
        val wrapper = load()
        val configFile = wrapper.configFile

        val updatedConfigs = configFile.configs.filter { it.name != name }
        val updatedActive = if (configFile.active == name && updatedConfigs.isNotEmpty()) {
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

    actual fun getConfigPath(): String = configFilePath

    actual suspend fun exists(): Boolean {
        return BrowserStorage.hasItem(CONFIG_KEY)
    }

    actual suspend fun saveMcpServers(mcpServers: Map<String, McpServerConfig>) {
        val wrapper = load()
        val configFile = wrapper.configFile

        val updatedConfigFile = configFile.copy(mcpServers = mcpServers)
        save(updatedConfigFile)
    }

    actual suspend fun saveRemoteServer(remoteServer: RemoteServerConfig) {
        val wrapper = load()
        val configFile = wrapper.configFile

        val updatedConfigFile = configFile.copy(remoteServer = remoteServer)
        save(updatedConfigFile)
    }

    actual suspend fun saveLastWorkspace(name: String, path: String) {
        val wrapper = load()
        val configFile = wrapper.configFile

        val updatedConfigFile = configFile.copy(
            lastWorkspace = WorkspaceInfo(name = name, path = path)
        )
        save(updatedConfigFile)
    }

    actual suspend fun getLastWorkspace(): WorkspaceInfo? {
        val wrapper = load()
        return wrapper.getLastWorkspace()
    }
    
    actual suspend fun saveIssueTracker(issueTracker: IssueTrackerConfig) {
        val wrapper = load()
        val configFile = wrapper.configFile
        
        val updatedConfigFile = configFile.copy(issueTracker = issueTracker)
        save(updatedConfigFile)
    }
    
    actual suspend fun getIssueTracker(): IssueTrackerConfig {
        val wrapper = load()
        return wrapper.getIssueTracker()
    }

    actual suspend fun loadToolConfig(): ToolConfigFile {
        return try {
            val content = BrowserStorage.getItem(TOOL_CONFIG_KEY)
            if (content == null) {
                console.log("WASM: No tool config found in localStorage, returning default")
                return ToolConfigFile.default()
            }

            json.decodeFromString<ToolConfigFile>(content)
        } catch (e: Exception) {
            console.error("WASM: Error loading tool config: ${e.message}")
            ToolConfigFile.default()
        }
    }

    actual suspend fun saveToolConfig(toolConfig: ToolConfigFile) {
        try {
            val jsonContent = json.encodeToString(ToolConfigFile.serializer(), toolConfig)
            BrowserStorage.setItem(TOOL_CONFIG_KEY, jsonContent)
            console.log("WASM: Tool config saved to localStorage")
        } catch (e: Exception) {
            console.error("WASM: Error saving tool config: ${e.message}")
            throw e
        }
    }

    actual fun getToolConfigPath(): String = toolConfigFilePath

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

    private fun createEmpty(): AutoDevConfigWrapper {
        return AutoDevConfigWrapper(ConfigFile(active = "", configs = emptyList()))
    }

    /**
     * Parse YAML config file using YamlUtils
     */
    private fun parseYamlConfig(content: String): ConfigFile {
        // Try to parse as JSON first (for compatibility)
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
