package cc.unitmesh.agent.tool.tracking

import kotlinx.serialization.Serializable

/**
 * Represents a single file change operation
 */
@Serializable
data class FileChange(
    /**
     * The file path that was changed
     */
    val filePath: String,
    
    /**
     * Type of change operation
     */
    val changeType: ChangeType,
    
    /**
     * Original content before change (null for new files)
     */
    val originalContent: String?,
    
    /**
     * New content after change (null for deleted files)
     */
    val newContent: String?,
    
    /**
     * Timestamp when the change was recorded
     */
    val timestamp: Long = getCurrentTimestamp(),
    
    /**
     * Additional metadata about the change
     */
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Get a display name for the file (just the filename)
     */
    fun getFileName(): String = filePath.substringAfterLast('/')
    
    /**
     * Get the size difference in characters
     */
    fun getSizeDiff(): Int {
        val oldSize = originalContent?.length ?: 0
        val newSize = newContent?.length ?: 0
        return newSize - oldSize
    }
    
    /**
     * Get the line difference
     */
    fun getLineDiff(): Int {
        val oldLines = originalContent?.lines()?.size ?: 0
        val newLines = newContent?.lines()?.size ?: 0
        return newLines - oldLines
    }
}

/**
 * Types of file change operations
 */
@Serializable
enum class ChangeType {
    CREATE,
    EDIT,
    DELETE,
    OVERWRITE
}

/**
 * Cross-platform way to get current timestamp
 */
expect fun getCurrentTimestamp(): Long

