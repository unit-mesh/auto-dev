package cc.unitmesh.devins.ui.compose.agent.codereview.analysis

import cc.unitmesh.devins.ui.compose.agent.codereview.DiffFileInfo
import cc.unitmesh.devins.ui.compose.agent.codereview.ModifiedCodeRange
import cc.unitmesh.devins.ui.compose.sketch.DiffLineType

/**
 * Builds diff context strings from diff file information.
 * This is a pure non-AI utility for creating human-readable diff summaries.
 */
class DiffContextBuilder {
    /**
     * Build diff context showing what was changed.
     *
     * @param diffFiles List of changed files
     * @param modifiedCodeRanges Optional map of modified code ranges for detailed context
     * @return Formatted diff context string
     */
    fun buildDiffContext(
        diffFiles: List<DiffFileInfo>,
        modifiedCodeRanges: Map<String, List<ModifiedCodeRange>> = emptyMap()
    ): String {
        if (diffFiles.isEmpty()) return ""

        return buildString {
            appendLine("## Changed Files Summary")
            appendLine()

            diffFiles.forEach { file ->
                appendLine("### ${file.path}")
                appendLine("Change Type: ${file.changeType}")
                appendLine(
                    "Modified Lines: ${
                        file.hunks.sumOf {
                            it.lines.count { line ->
                                line.type == DiffLineType.ADDED
                            }
                        }
                    }"
                )
                appendLine()
            }

            // Include modified code ranges if available
            if (modifiedCodeRanges.isNotEmpty()) {
                appendLine()
                appendLine("## Modified Code Elements")
                appendLine()

                modifiedCodeRanges.forEach { (filePath, ranges) ->
                    if (ranges.isNotEmpty()) {
                        appendLine("### $filePath")
                        ranges.forEach { range ->
                            appendLine("- ${range.elementType}: ${range.elementName} (lines ${range.startLine}-${range.endLine})")
                        }
                        appendLine()
                    }
                }
            }
        }
    }

    /**
     * Build a compact summary of changes.
     *
     * @param diffFiles List of changed files
     * @return Compact summary string
     */
    fun buildCompactSummary(diffFiles: List<DiffFileInfo>): String {
        val addedCount = diffFiles.count { it.changeType == cc.unitmesh.agent.tool.tracking.ChangeType.CREATE }
        val modifiedCount = diffFiles.count { it.changeType == cc.unitmesh.agent.tool.tracking.ChangeType.EDIT }
        val deletedCount = diffFiles.count { it.changeType == cc.unitmesh.agent.tool.tracking.ChangeType.DELETE }
        val renamedCount = diffFiles.count { it.changeType == cc.unitmesh.agent.tool.tracking.ChangeType.RENAME }

        return buildString {
            append("${diffFiles.size} file(s) changed")
            if (addedCount > 0) append(", $addedCount added")
            if (modifiedCount > 0) append(", $modifiedCount modified")
            if (deletedCount > 0) append(", $deletedCount deleted")
            if (renamedCount > 0) append(", $renamedCount renamed")
        }
    }
}
