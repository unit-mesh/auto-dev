package cc.unitmesh.devti.vcs.context

import com.intellij.openapi.vcs.changes.Change

/**
 * Strategy for generating diff output based on priority and token budget.
 */
interface DiffStrategy {
    /**
     * Generate diff output for a change
     * @param change The change to generate diff for
     * @param fullDiff The full diff content (if available)
     * @return The formatted diff output
     */
    fun generateDiff(change: PrioritizedChange, fullDiff: String?): String
}

/**
 * Full diff strategy - includes complete diff content
 */
class FullDiffStrategy : DiffStrategy {
    override fun generateDiff(change: PrioritizedChange, fullDiff: String?): String {
        return fullDiff ?: generateSummary(change)
    }

    private fun generateSummary(change: PrioritizedChange): String {
        return when (change.change.type) {
            Change.Type.NEW -> "new file ${change.filePath}"
            Change.Type.DELETED -> "delete file ${change.filePath}"
            Change.Type.MODIFICATION -> "modify file ${change.filePath}"
            Change.Type.MOVED -> {
                val beforePath = change.change.beforeRevision?.file?.path
                "rename file from $beforePath to ${change.filePath}"
            }
            else -> "change file ${change.filePath}"
        }
    }
}

/**
 * Summary diff strategy - only includes file change summary
 */
class SummaryDiffStrategy : DiffStrategy {
    override fun generateDiff(change: PrioritizedChange, fullDiff: String?): String {
        val changeType = when (change.change.type) {
            Change.Type.NEW -> "new"
            Change.Type.DELETED -> "deleted"
            Change.Type.MODIFICATION -> "modified"
            Change.Type.MOVED -> "renamed"
            else -> "changed"
        }

        val sizeInfo = if (change.fileSize > 0) {
            " (${formatFileSize(change.fileSize)})"
        } else {
            ""
        }

        return when (change.change.type) {
            Change.Type.MOVED -> {
                val beforePath = change.change.beforeRevision?.file?.path
                "$changeType: $beforePath -> ${change.filePath}$sizeInfo"
            }
            else -> {
                "$changeType: ${change.filePath}$sizeInfo"
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}

/**
 * Metadata-only strategy - only includes file path and change type
 */
class MetadataOnlyStrategy : DiffStrategy {
    override fun generateDiff(change: PrioritizedChange, fullDiff: String?): String {
        return when (change.change.type) {
            Change.Type.NEW -> "new file ${change.filePath}"
            Change.Type.DELETED -> "delete file ${change.filePath}"
            Change.Type.MODIFICATION -> "modify file ${change.filePath}"
            Change.Type.MOVED -> {
                val beforePath = change.change.beforeRevision?.file?.path
                "rename file from $beforePath to ${change.filePath}"
            }
            else -> "change file ${change.filePath}"
        }
    }
}

