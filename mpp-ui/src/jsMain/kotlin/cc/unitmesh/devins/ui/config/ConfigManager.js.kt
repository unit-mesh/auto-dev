package cc.unitmesh.devins.ui.config

import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.llm.NamedModelConfig
import cc.unitmesh.yaml.YamlUtils
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
    private val homeDir: String =
        if (isNodeJs) {
            osModule.homedir() as String
        } else {
            "/tmp" // Fallback for browser environment
        }

    private val configDir: String =
        if (isNodeJs) {
            pathModule.join(homeDir, ".autodev") as String
        } else {
            "/tmp/.autodev" // Fallback for browser environment
        }

    private val configFilePath: String =
        if (isNodeJs) {
            pathModule.join(configDir, "config.yaml") as String
        } else {
            "/tmp/.autodev/config.yaml" // Fallback for browser environment
        }

    private val toolConfigFilePath: String =
        if (isNodeJs) {
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

    /**
     * Convert ConfigFile to YAML format using YamlUtils
     */
    private fun toYaml(configFile: ConfigFile): String {
        return YamlUtils.dump(configFile, kotlinx.serialization.serializer())
    }
}
