package cc.unitmesh.devins.ui.config

import android.content.Context
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.llm.NamedModelConfig
import cc.unitmesh.yaml.YamlUtils
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
        val context =
            appContext
                ?: throw IllegalStateException("ConfigManager not initialized. Call ConfigManager.initialize(context) first.")

        // Use app-specific internal storage directory
        // Path: /data/data/your.package.name/files/.autodev/
        return File(context.filesDir, ".autodev")
    }

    private fun getConfigFile(): File {
        return File(getConfigDir(), "config.yaml")
    }

    private fun getToolConfigFile(): File {
        return File(getConfigDir(), "mcp.json")
    }

    // JSON parser for potential JSON config support
    private val json =
        Json {
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

    actual fun getConfigPath(): String = getConfigFile().absolutePath

    actual suspend fun exists(): Boolean =
        withContext(Dispatchers.IO) {
            getConfigFile().exists()
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
    
    actual suspend fun getIssueTracker(): IssueTrackerConfig? {
        val wrapper = load()
        return wrapper.getIssueTracker()
    }

    actual suspend fun loadToolConfig(): ToolConfigFile =
        withContext(Dispatchers.IO) {
            try {
                val file = getToolConfigFile()
                if (!file.exists()) {
                    return@withContext ToolConfigFile.default()
                }

                val content = file.readText()
                json.decodeFromString<ToolConfigFile>(content)
            } catch (e: Exception) {
                println("Error loading tool config: ${e.message}")
                ToolConfigFile.default()
            }
        }

    actual suspend fun saveToolConfig(toolConfig: ToolConfigFile) =
        withContext(Dispatchers.IO) {
            try {
                val dir = getConfigDir()
                dir.mkdirs()

                val file = getToolConfigFile()
                val jsonContent = json.encodeToString(ToolConfigFile.serializer(), toolConfig)
                file.writeText(jsonContent)
            } catch (e: Exception) {
                println("Error saving tool config: ${e.message}")
                throw e
            }
        }

    actual fun getToolConfigPath(): String = getToolConfigFile().absolutePath

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
        // Try to parse as JSON first (for MCP config compatibility)
        try {
            if (content.trim().startsWith("{")) {
                return json.decodeFromString<ConfigFile>(content)
            }
        } catch (e: Exception) {
            // Fall through to YAML parsing
        }

        // Use YamlUtils for proper YAML parsing
        return YamlUtils.loadAs(content, kotlinx.serialization.serializer())
    }

    /**
     * Convert ConfigFile to YAML format using YamlUtils
     */
    private fun toYaml(configFile: ConfigFile): String {
        return YamlUtils.dump(configFile, kotlinx.serialization.serializer())
    }
}
