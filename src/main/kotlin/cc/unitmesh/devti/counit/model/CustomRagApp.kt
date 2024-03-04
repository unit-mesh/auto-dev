package cc.unitmesh.devti.counit.model

/**
 * Enumeration of possible response actions.
 *
 * @property Direct Direct display result
 * @property TextChunk Text splitting result
 * @property Flow Will be handled by the client
 */
enum class ResponseAction {
    /**
     * Direct display result
     */
    Direct,

    /**
     * Text splitting result
     */
    TextChunk,

    /**
     * will be handled by the client
     */
    Flow
}

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

data class CustomRagApp(
    val name: String,
    val description: String = "",
    val url: String = "",
    val icon: String = "",
    val responseAction: ResponseAction = ResponseAction.Direct,
    val customFlowTransition: List<CustomFlowTransition> = emptyList(),
)