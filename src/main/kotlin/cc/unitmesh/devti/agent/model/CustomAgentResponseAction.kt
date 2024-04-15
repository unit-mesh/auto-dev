package cc.unitmesh.devti.agent.model

/**
 * CustomAgentResponseAction is an enumeration of possible response actions that can be taken by the
 * CustomAgent when processing user input.
 *
 * @property Direct Direct display result - The CustomAgent will directly display the result to the user.
 * @property TextChunk Text splitting result - The CustomAgent will split the result into text chunks for easier consumption.
 * @property Flow Will be handled by the client - The response will be handled by the client application.
 * @property Stream Stream response - The CustomAgent will stream the response to the user.
 * @property WebView Display result in WebView - The CustomAgent will display the result in a WebView.
 * @property DevIns Handle by DevIns language compile and run in code block.
 */
enum class CustomAgentResponseAction {
    /**
     * Direct display result
     */
    Direct,

    /**
     * Stream response
     */
    Stream,

    /**
     * Text splitting result
     */
    TextChunk,

    /**
     * will be handled by the client
     */
    Flow,

    /**
     * Display result in WebView
     */
    WebView,

    /**
     * Handle by DevIns language compile and run in code block.
     * @since: AutoDev@1.8.2
     */
    DevIns
}