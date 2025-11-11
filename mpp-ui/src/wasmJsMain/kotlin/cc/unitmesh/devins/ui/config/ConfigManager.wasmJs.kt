package cc.unitmesh.devins.ui.config

import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.llm.NamedModelConfig
import cc.unitmesh.yaml.YamlUtils

/**
 * WASM implementation of ConfigManager
 * Uses browser localStorage for configuration storage
 */
actual object ConfigManager {
    private val configDir: String = "browser://localStorage"
    private val configFilePath: String = "$configDir/autodev-config.yaml"
    private val toolConfigFilePath: String = "$configDir/mcp.json"

    actual suspend fun load(): AutoDevConfigWrapper {
        println("WASM: ConfigManager.load() - returning empty config")
        return createEmpty()
    }

    actual suspend fun save(configFile: ConfigFile) {
        println("WASM: ConfigManager.save() - not implemented")
    }

    actual suspend fun saveConfig(
        config: NamedModelConfig,
        setActive: Boolean
    ) {
        println("WASM: ConfigManager.saveConfig() - not implemented")
    }

    actual suspend fun deleteConfig(name: String) {
        println("WASM: ConfigManager.deleteConfig() - not implemented")
    }

    actual suspend fun setActive(name: String) {
        println("WASM: ConfigManager.setActive() - not implemented")
    }

    actual fun getConfigPath(): String = configFilePath

    actual suspend fun exists(): Boolean {
        return false
    }

    actual suspend fun saveMcpServers(mcpServers: Map<String, McpServerConfig>) {
        println("WASM: ConfigManager.saveMcpServers() - not implemented")
    }

    actual suspend fun saveRemoteServer(remoteServer: RemoteServerConfig) {
        println("WASM: ConfigManager.saveRemoteServer() - not implemented")
    }

    actual suspend fun loadToolConfig(): ToolConfigFile {
        println("WASM: ConfigManager.loadToolConfig() - returning default")
        return ToolConfigFile.default()
    }

    actual suspend fun saveToolConfig(toolConfig: ToolConfigFile) {
        println("WASM: ConfigManager.saveToolConfig() - not implemented")
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
}
