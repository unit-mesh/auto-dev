package cc.unitmesh.devins.ui.config

import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.llm.NamedModelConfig
import cc.unitmesh.yaml.YamlUtils
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*

/**
 * iOS implementation of ConfigManager
 * Uses NSFileManager for file operations
 */
actual object ConfigManager {
    @OptIn(ExperimentalForeignApi::class)
    private val fileManager = NSFileManager.defaultManager

    @OptIn(ExperimentalForeignApi::class)
    private val configDir: String
        get() {
            val paths = NSSearchPathForDirectoriesInDomains(
                NSApplicationSupportDirectory,
                NSUserDomainMask,
                true
            )
            val appSupportDir = paths.firstOrNull() as? String ?: NSHomeDirectory()
            return "$appSupportDir/autodev"
        }

    private val configFilePath: String
        get() = "$configDir/config.yaml"

    private val toolConfigFilePath: String
        get() = "$configDir/mcp.json"

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun load(): AutoDevConfigWrapper =
        withContext(Dispatchers.Default) {
            try {
                if (!fileManager.fileExistsAtPath(configFilePath)) {
                    return@withContext createEmpty()
                }

                val content = NSString.stringWithContentsOfFile(
                    configFilePath,
                    encoding = NSUTF8StringEncoding,
                    error = null
                ) as? String ?: return@withContext createEmpty()

                // Parse YAML content
                val configFileData = parseYamlConfig(content)
                AutoDevConfigWrapper(configFileData)
            } catch (e: Exception) {
                println("❌ Exception loading config: ${e.message}")
                e.printStackTrace()
                createEmpty()
            }
        }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun save(configFile: ConfigFile) =
        withContext(Dispatchers.Default) {
            try {
                // Create config directory if it doesn't exist
                if (!fileManager.fileExistsAtPath(configDir)) {
                    fileManager.createDirectoryAtPath(
                        configDir,
                        withIntermediateDirectories = true,
                        attributes = null,
                        error = null
                    )
                }

                // Convert to YAML format
                val yamlContent = buildYamlConfig(configFile)

                // Write to file
                val nsString = yamlContent as NSString
                nsString.writeToFile(
                    configFilePath,
                    atomically = true,
                    encoding = NSUTF8StringEncoding,
                    error = null
                )

                println("✅ Configuration saved to $configFilePath")
            } catch (e: Exception) {
                println("❌ Error saving config: ${e.message}")
                throw e
            }
        }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun saveConfig(
        config: NamedModelConfig,
        setActive: Boolean
    ) = withContext(Dispatchers.Default) {
        val wrapper = load()
        val configs = wrapper.getAllConfigs().toMutableList()

        // Update or add config
        val existingIndex = configs.indexOfFirst { it.name == config.name }
        if (existingIndex >= 0) {
            configs[existingIndex] = config
        } else {
            configs.add(config)
        }

        val active = if (setActive) config.name else wrapper.getActiveName()

        save(
            ConfigFile(
                active = active,
                configs = configs
            )
        )
    }

    actual suspend fun setActive(name: String) {
        val wrapper = load()
        save(
            ConfigFile(
                active = name,
                configs = wrapper.getAllConfigs()
            )
        )
    }

    actual suspend fun deleteConfig(name: String) {
        val wrapper = load()
        val configs = wrapper.getAllConfigs().filter { it.name != name }
        val active =
            if (wrapper.getActiveName() == name) {
                configs.firstOrNull()?.name ?: ""
            } else {
                wrapper.getActiveName()
            }

        save(
            ConfigFile(
                active = active,
                configs = configs
            )
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun loadToolConfig(): ToolConfigFile =
        withContext(Dispatchers.Default) {
            try {
                if (!fileManager.fileExistsAtPath(toolConfigFilePath)) {
                    return@withContext ToolConfigFile()
                }

                val content = NSString.stringWithContentsOfFile(
                    toolConfigFilePath,
                    encoding = NSUTF8StringEncoding,
                    error = null
                ) as? String ?: return@withContext ToolConfigFile()

                kotlinx.serialization.json.Json.decodeFromString<ToolConfigFile>(content)
            } catch (e: Exception) {
                println("❌ Error loading tool config: ${e.message}")
                ToolConfigFile.default()
            }
        }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun saveToolConfig(toolConfig: ToolConfigFile) =
        withContext(Dispatchers.Default) {
            try {
                // Create config directory if it doesn't exist
                if (!fileManager.fileExistsAtPath(configDir)) {
                    fileManager.createDirectoryAtPath(
                        configDir,
                        withIntermediateDirectories = true,
                        attributes = null,
                        error = null
                    )
                }

                val json = kotlinx.serialization.json.Json { prettyPrint = true; ignoreUnknownKeys = true }
                val jsonContent = json.encodeToString(ToolConfigFile.serializer(), toolConfig)

                // Write to file
                val nsString = jsonContent as NSString
                nsString.writeToFile(
                    toolConfigFilePath,
                    atomically = true,
                    encoding = NSUTF8StringEncoding,
                    error = null
                )

                println("✅ Tool configuration saved to $toolConfigFilePath")
            } catch (e: Exception) {
                println("❌ Error saving tool config: ${e.message}")
                throw e
            }
        }

    actual fun getConfigPath(): String = configFilePath

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun exists(): Boolean {
        return fileManager.fileExistsAtPath(configFilePath)
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
        return AutoDevConfigWrapper(
            ConfigFile(
                active = "",
                configs = emptyList()
            )
        )
    }

    private fun parseYamlConfig(content: String): ConfigFile {
        // Try to parse as JSON first (for MCP config compatibility)
        try {
            if (content.trim().startsWith("{")) {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                return json.decodeFromString<ConfigFile>(content)
            }
        } catch (e: Exception) {
            // Fall through to YAML parsing
        }

        // Use YamlUtils for proper YAML parsing
        return YamlUtils.loadAs(content, kotlinx.serialization.serializer())
    }

    private fun buildYamlConfig(configFile: ConfigFile): String {
        return YamlUtils.dump(configFile, kotlinx.serialization.serializer())
    }
}

