package cc.unitmesh.devins.ui.config

/**
 * Configuration Manager - File-based configuration management
 *
 * Manages loading, saving, and validating configuration from ~/.autodev/config.yaml
 * Similar to the TypeScript ConfigManager but implemented as expect/actual for KMP.
 *
 * Usage:
 * ```kotlin
 * // Load configuration
 * val wrapper = ConfigManager.load()
 * val activeConfig = wrapper.getActiveConfig()
 *
 * // Save a new configuration
 * val config = NamedModelConfig(
 *     name = "default",
 *     provider = "openai",
 *     apiKey = "sk-...",
 *     model = "gpt-4"
 * )
 * ConfigManager.saveConfig(config, setActive = true)
 * ```
 */
expect object ConfigManager {
    /**
     * Load configuration from file (~/.autodev/config.yaml)
     * Returns empty config if file doesn't exist
     */
    suspend fun load(): AutoDevConfigWrapper

    /**
     * Save entire configuration file
     */
    suspend fun save(configFile: ConfigFile)

    /**
     * Add or update a single configuration
     *
     * @param config The configuration to save
     * @param setActive Whether to set this as the active configuration
     */
    suspend fun saveConfig(
        config: NamedModelConfig,
        setActive: Boolean = true
    )

    /**
     * Delete a configuration by name
     */
    suspend fun deleteConfig(name: String)

    /**
     * Switch active configuration
     */
    suspend fun setActive(name: String)

    /**
     * Get configuration file path
     */
    fun getConfigPath(): String

    /**
     * Check if configuration file exists
     */
    suspend fun exists(): Boolean
    
    /**
     * Save MCP servers configuration
     * 
     * @param mcpServers Map of server name to server configuration
     */
    suspend fun saveMcpServers(mcpServers: Map<String, cc.unitmesh.agent.mcp.McpServerConfig>)
    
    /**
     * Load tool configuration from file (~/.autodev/mcp.json)
     * Returns default config if file doesn't exist
     */
    suspend fun loadToolConfig(): cc.unitmesh.agent.config.ToolConfigFile
    
    /**
     * Save tool configuration to file (~/.autodev/mcp.json)
     */
    suspend fun saveToolConfig(toolConfig: cc.unitmesh.agent.config.ToolConfigFile)
    
    /**
     * Get tool config file path
     */
    fun getToolConfigPath(): String

    /**
     * Generate a unique configuration name by appending -1, -2, etc. if the name already exists
     * 
     * @param baseName The desired configuration name
     * @param existingNames List of existing configuration names
     * @return A unique name (either baseName or baseName-1, baseName-2, etc.)
     */
    fun generateUniqueConfigName(baseName: String, existingNames: List<String>): String
}
