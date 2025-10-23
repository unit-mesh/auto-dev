package cc.unitmesh.devti.vcs.context

import com.intellij.openapi.vcs.changes.Change

/**
 * Represents a VCS change with associated priority and metadata.
 */
data class PrioritizedChange(
    val change: Change,
    val priority: FilePriority,
    val filePath: String,
    val fileSize: Long,
    val fileExtension: String
) : Comparable<PrioritizedChange> {
    
    /**
     * Compare by priority level (descending), then by file size (ascending)
     */
    override fun compareTo(other: PrioritizedChange): Int {
        val priorityComparison = other.priority.level.compareTo(this.priority.level)
        return if (priorityComparison != 0) {
            priorityComparison
        } else {
            this.fileSize.compareTo(other.fileSize)
        }
    }
}

