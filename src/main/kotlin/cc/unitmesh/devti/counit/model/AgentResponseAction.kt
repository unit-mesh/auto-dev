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
    Flow,

    /**
     * Display result in WebView
     */
    WebView
}