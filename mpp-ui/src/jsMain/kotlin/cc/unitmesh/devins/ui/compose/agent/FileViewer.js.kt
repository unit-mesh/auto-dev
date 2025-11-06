package cc.unitmesh.devins.ui.compose.agent

import kotlinx.browser.window

/**
 * JS implementation of FileViewer
 * For JS/Node.js, we log the file path or could open in an external editor
 */
actual class FileViewer {
    actual fun showFile(filePath: String, readOnly: Boolean) {
        try {
            // For JS/Node.js, we print to console
            console.log("FileViewer: Opening file $filePath")
            // TODO: Could implement using Monaco Editor or similar web-based editor
        } catch (e: Exception) {
            console.error("Error opening file: ${e.message}")
        }
    }

    actual fun close() {
        // No-op for JS
    }
}

