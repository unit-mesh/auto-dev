package cc.unitmesh.devins.ui.compose.agent

/**
 * Platform-specific file viewer interface
 * Shows file content in a syntax-highlighted editor
 */
expect class FileViewer() {
    /**
     * Show file content in a viewer window
     * @param filePath The absolute or relative path to the file
     * @param readOnly Whether the viewer should be read-only
     */
    fun showFile(filePath: String, readOnly: Boolean = true)
    
    /**
     * Close the viewer window if it's open
     */
    fun close()
}

