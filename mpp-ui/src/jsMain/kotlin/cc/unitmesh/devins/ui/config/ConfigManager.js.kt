package cc.unitmesh.devins.ui.config

import kotlinx.coroutines.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.js.Promise

// External Node.js modules (must be top-level)
@JsModule("fs")
@JsNonModule
external val fsModule: dynamic

@JsModule("fs/promises")
@JsNonModule
external val fsPromises: dynamic

@JsModule("path")
@JsNonModule
external val pathModule: dynamic

@JsModule("os")
@JsNonModule
external val osModule: dynamic

/**
 * JS implementation of ConfigManager
 * Uses Node.js fs module for file operations
 * This implementation is called by TypeScript code
 */
actual object ConfigManager {
    private val homeDir: String = osModule.homedir() as String
    private val configDir: String = pathModule.join(homeDir, ".autodev") as String
    private val configFilePath: String = pathModule.join(configDir, "config.yaml") as String
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    actual suspend fun load(): AutoDevConfigWrapper {
        return try {
            // Check if file exists
            val exists = try {
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
    
    actual suspend fun saveConfig(config: NamedModelConfig, setActive: Boolean) {
        val wrapper = load()
        val configFile = wrapper.getConfigFile()
        
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
        val configFile = wrapper.getConfigFile()
        
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
    
    private fun createEmpty(): AutoDevConfigWrapper {
        return AutoDevConfigWrapper(ConfigFile(active = "", configs = emptyList()))
    }
    
    /**
     * Parse YAML config file
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
    private fun toYaml(configFile: ConfigFile): String = buildString {
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
