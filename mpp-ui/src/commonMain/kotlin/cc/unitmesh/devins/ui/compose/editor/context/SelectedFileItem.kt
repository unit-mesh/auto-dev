package cc.unitmesh.devins.ui.compose.editor.context

/**
 * Represents a selected file in the file context.
 * Used by TopToolbar and FileChip components.
 *
 * Similar to IDEA's SelectedFileItem but platform-agnostic.
 */
data class SelectedFileItem(
    val name: String,
    val path: String,
    val relativePath: String = name,
    val isDirectory: Boolean = false,
    val isRecentFile: Boolean = false
) {
    /**
     * Generate the DevIns command for this file/folder.
     * Uses /dir: for directories and /file: for files.
     */
    fun toDevInsCommand(): String {
        return if (isDirectory) "/dir:$path" else "/file:$path"
    }

    /**
     * Truncated path for display, e.g., "...cc/unitmesh/devins/idea/editor"
     * Shows the parent directory path without the file name, truncated if too long.
     */
    val truncatedPath: String
        get() {
            val parentPath = relativePath.substringBeforeLast("/", "")
            if (parentPath.isEmpty()) return ""
            if (parentPath.length <= 40) return parentPath

            val parts = parentPath.split("/")
            if (parts.size <= 2) return "...$parentPath"

            val keepParts = parts.takeLast(4)
            return "...${keepParts.joinToString("/")}"
        }

    companion object {
        /**
         * Create a SelectedFileItem from a file path.
         * Extracts the file name from the path.
         */
        fun fromPath(path: String, isDirectory: Boolean = false, isRecent: Boolean = false): SelectedFileItem {
            val name = path.substringAfterLast('/').ifEmpty {
                path.substringAfterLast('\\')
            }.ifEmpty { path }
            return SelectedFileItem(
                name = name,
                path = path,
                relativePath = path,
                isDirectory = isDirectory,
                isRecentFile = isRecent
            )
        }
    }
}

/**
 * Truncate path for display, showing last 3-4 parts.
 */
fun truncatePath(path: String, maxLength: Int = 30): String {
    val parentPath = path.substringBeforeLast('/')
    if (parentPath.isEmpty() || parentPath == path) return ""

    if (parentPath.length <= maxLength) return parentPath

    val parts = parentPath.split('/')
    if (parts.size <= 2) return "...$parentPath"

    val keepParts = parts.takeLast(3)
    return ".../${keepParts.joinToString("/")}"
}

