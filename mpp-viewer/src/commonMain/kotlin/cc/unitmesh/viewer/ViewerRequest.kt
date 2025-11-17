package cc.unitmesh.viewer

import kotlinx.serialization.Serializable

/**
 * Request to display content in the viewer
 *
 * @param type The type of content to display
 * @param content The content to display (text, base64 encoded data, etc.)
 * @param language The programming language for syntax highlighting (for CODE type)
 * @param fileName The name of the file being displayed
 * @param filePath The full path of the file being displayed
 * @param readOnly Whether the content should be read-only
 * @param lineNumber Optional line number to scroll to
 */
@Serializable
data class ViewerRequest(
    val type: ViewerType,
    val content: String,
    val language: String? = null,
    val fileName: String? = null,
    val filePath: String? = null,
    val readOnly: Boolean = true,
    val lineNumber: Int? = null
)

