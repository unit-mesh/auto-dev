package cc.unitmesh.devti.counit.model

import cc.unitmesh.devti.custom.team.InteractionType
import kotlinx.serialization.Serializable

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
) {
    var state: CustomAgentState = CustomAgentState.START
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