package cc.unitmesh.devins.ui.config

import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.agent.AgentType
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.NamedModelConfig
import kotlinx.serialization.Serializable

/**
 * Root configuration file structure
 *
 * Maps to ~/.autodev/config.yaml format:
 * ```yaml
 * active: default
 * configs:
 *   - name: default
 *     provider: openai
 *     apiKey: sk-...
 *     model: gpt-4
 *   - name: work
 *     provider: anthropic
 *     apiKey: sk-ant-...
 *     model: claude-3-opus
 * mcpServers:
 *   filesystem:
 *     command: npx
 *     args: ["-y", "@modelcontextprotocol/server-filesystem", "/path/to/allowed/files"]
 * remoteServer:
 *   url: "http://localhost:8080"
 *   enabled: false
 *   useServerConfig: false
 * ```
 */
@Serializable
public data class ConfigFile(
    val active: String = "",
    val configs: List<NamedModelConfig> = emptyList(),
    val mcpServers: Map<String, McpServerConfig>? = emptyMap(),
    val language: String? = "en", // Language preference: "en" or "zh"
    val remoteServer: RemoteServerConfig? = null,
    val agentType: String? = "Local", // "Local" or "Remote" - which agent mode to use
    val lastWorkspace: WorkspaceInfo? = null, // Last opened workspace information
    val issueTracker: IssueTrackerConfig? = null // Issue tracker configuration
)

/**
 * Remote server configuration
 */
@Serializable
data class RemoteServerConfig(
    val url: String = "http://localhost:8080",
    val enabled: Boolean = false,
    val useServerConfig: Boolean = false // Whether to use server's LLM config instead of local
)

/**
 * Last opened workspace information
 */
@Serializable
data class WorkspaceInfo(
    val name: String,
    val path: String
)

/**
 * Issue tracker configuration
 * Supports GitHub, GitLab, Jira, etc.
 */
@Serializable
data class IssueTrackerConfig(
    val type: String = "github", // "github", "gitlab", "jira", etc.
    val token: String = "",
    val repoOwner: String = "", // For GitHub/GitLab
    val repoName: String = "",  // For GitHub/GitLab
    val serverUrl: String = "",  // For GitLab/Jira (e.g., "https://gitlab.com", "https://jira.company.com")
    val enabled: Boolean = false
) {
    fun isConfigured(): Boolean {
        return when (type.lowercase()) {
            "github" -> token.isNotBlank() && repoOwner.isNotBlank() && repoName.isNotBlank()
            "gitlab" -> token.isNotBlank() && repoOwner.isNotBlank() && repoName.isNotBlank() && serverUrl.isNotBlank()
            "jira" -> token.isNotBlank() && serverUrl.isNotBlank()
            else -> false
        }
    }
}

class AutoDevConfigWrapper(val configFile: ConfigFile) {
    fun getActiveConfig(): NamedModelConfig? {
        if (configFile.active.isEmpty() || configFile.configs.isEmpty()) {
            return null
        }

        return configFile.configs.find { it.name == configFile.active }
            ?: configFile.configs.firstOrNull()
    }

    fun getAllConfigs(): List<NamedModelConfig> = configFile.configs

    fun getActiveName(): String = configFile.active

    fun isValid(): Boolean {
        val active = getActiveConfig() ?: return false

        if (active.provider.equals("ollama", ignoreCase = true)) {
            return active.model.isNotEmpty()
        }

        return active.provider.isNotEmpty() &&
            active.apiKey.isNotEmpty() &&
            active.model.isNotEmpty()
    }

    fun getActiveModelConfig(): ModelConfig? {
        return getActiveConfig()?.toModelConfig()
    }

    fun getMcpServers(): Map<String, McpServerConfig> {
        return configFile.mcpServers ?: emptyMap()
    }

    fun getEnabledMcpServers(): Map<String, McpServerConfig> {
        return configFile.mcpServers?.filter { !it.value.disabled && it.value.validate() } ?: emptyMap()
    }

    fun getRemoteServer(): RemoteServerConfig {
        return configFile.remoteServer ?: RemoteServerConfig()
    }

    fun isRemoteMode(): Boolean {
        return configFile.remoteServer?.enabled == true
    }

    fun getAgentType(): AgentType {
        return AgentType.fromString(configFile.agentType ?: "Local")
    }

    fun getLastWorkspace(): WorkspaceInfo? {
        return configFile.lastWorkspace
    }
    
    fun getIssueTracker(): IssueTrackerConfig {
        return configFile.issueTracker ?: IssueTrackerConfig()
    }
}
