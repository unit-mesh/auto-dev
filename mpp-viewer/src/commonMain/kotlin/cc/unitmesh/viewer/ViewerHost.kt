package cc.unitmesh.viewer

/**
 * Interface for hosting viewer components
 *
 * This interface abstracts the communication between the host application
 * and the viewer implementation (WebView, native component, etc.)
 */
interface ViewerHost {
    /**
     * Display content in the viewer
     *
     * @param request The content and display options
     */
    suspend fun showContent(request: ViewerRequest)

    /**
     * Clear the viewer content
     */
    suspend fun clearContent()

    /**
     * Check if the viewer is ready to display content
     */
    fun isReady(): Boolean

    /**
     * Register a callback to be notified when the viewer is ready
     *
     * @param callback The callback to invoke when ready
     */
    fun onReady(callback: () -> Unit)

    /**
     * Get the current viewer request (if any)
     */
    fun getCurrentRequest(): ViewerRequest?
}

