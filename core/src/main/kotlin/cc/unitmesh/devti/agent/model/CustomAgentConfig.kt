package cc.unitmesh.devti.agent.model

import cc.unitmesh.devti.agent.configurable.customAgentSetting
import cc.unitmesh.devti.custom.team.InteractionType
import com.intellij.openapi.project.Project
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
// DON'T CHANGE THIS IMPORT
import kotlinx.serialization.decodeFromString

@Serializable
data class CustomFlowTransition(
    /**
     * will be JsonPath
     */
    val source: String,
    /**
     * will be JsonPath too
     */
    val target: String,
)

/**
 * Basic configuration for a custom agent.
 * For Example:
 * ```json
 * {
 *   "name": "CustomAgent",
 *   "description": "This is a custom agent configuration example.",
 *   "url": "https://custom-agent.example.com",
 *   "icon": "https://custom-agent.example.com/icon.png",
 *   "responseAction": "Direct",
 *   "transition": [
 *     {
 *       "source": "$.from",
 *       "target": "$.to"
 *     }
 *   ],
 *   "interactive": "ChatPanel",
 *   "auth": {
 *     "type": "Bearer",
 *     "token": "<PASSWORD>"
 *   }
 * }
 * ```
 */
@Serializable
data class CustomAgentConfig(
    val name: String,
    val description: String = "",
    val url: String = "",
    val icon: String = "",
    val connector: ConnectorConfig? = null,
    val responseAction: CustomAgentResponseAction = CustomAgentResponseAction.Direct,
    val transition: List<CustomFlowTransition> = emptyList(),
    val interactive: InteractionType = InteractionType.ChatPanel,
    val auth: CustomAgentAuth? = null,
    val defaultTimeout: Long = 10,
    val enabled: Boolean = true,
) {
    var state: CustomAgentState = CustomAgentState.START

    companion object {
        fun loadFromProject(project: Project): List<CustomAgentConfig> {
            val configs: List<CustomAgentConfig> = try {
                val ragsJsonConfig = project.customAgentSetting.ragsJsonConfig
                Json.decodeFromString(ragsJsonConfig)
            } catch (e: Exception) {
                emptyList()
            }

            /**
             * Only return enabled agents
             */
            return configs.filter { it.enabled }
        }
    }
}

@Serializable
data class ConnectorConfig(
    /**
     * will be Json Config
     */
    val requestFormat: String = "",
    /**
     * will be JsonPath
     */
    val responseFormat: String = "",
)

/**
 * CustomAgentState is an enumeration that defines the possible states of a custom agent.
 *
 * - [CustomAgentState.START] indicates that the agent has just been initialized and is ready to receive commands.
 * - [CustomAgentState.HANDLING] means that the agent is currently processing a command.
 * - [CustomAgentState.FINISHED] signifies that the agent has completed its execution and is no longer operational.
 *
 * This enum is used to track the state of the agent and make decisions accordingly in the agent's lifecycle.
 */
@Serializable
enum class CustomAgentState {
    START,
    HANDLING,
    FINISHED
}

@Serializable
data class CustomAgentAuth(
    val type: AuthType = AuthType.Bearer,
    val token: String = "",
)

@Serializable
enum class AuthType {
    Bearer,
}