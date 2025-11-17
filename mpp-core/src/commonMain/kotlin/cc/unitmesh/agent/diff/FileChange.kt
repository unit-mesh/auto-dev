package cc.unitmesh.agent.diff

import cc.unitmesh.agent.Platform
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
    val timestamp: Long = Platform.getCurrentTimestamp(),

    /**
     * Additional metadata about the change
     */
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Cached diff statistics (calculated lazily)
     */
    @Transient
    private var _diffStats: DiffUtils.DiffStats? = null
    
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
     * Get the line difference (total change: added - deleted)
     * This is the NET change in line count
     */
    fun getLineDiff(): Int {
        val oldLines = originalContent?.lines()?.size ?: 0
        val newLines = newContent?.lines()?.size ?: 0
        return newLines - oldLines
    }
    
    /**
     * Get accurate diff statistics using LCS algorithm
     * Returns: added lines, deleted lines, context lines
     */
    fun getDiffStats(): DiffUtils.DiffStats {
        if (_diffStats == null) {
            _diffStats = DiffUtils.calculateDiffStats(originalContent, newContent)
        }
        return _diffStats!!
    }
    
    /**
     * Get the number of added lines (based on diff algorithm)
     */
    fun getAddedLines(): Int = getDiffStats().addedLines
    
    /**
     * Get the number of deleted lines (based on diff algorithm)
     */
    fun getDeletedLines(): Int = getDiffStats().deletedLines
    
    /**
     * Get the total number of changed lines (added + deleted)
     */
    fun getTotalChangedLines(): Int = getDiffStats().totalChanges
}

/**
 * Types of file change operations
 */
@Serializable
enum class ChangeType {
    CREATE,
    EDIT,
    DELETE,
    RENAME
}
