package cc.unitmesh.agent.codereview

import cc.unitmesh.agent.linter.LintIssue
import kotlinx.serialization.Serializable

/**
 * Represents a modified code range (function, class, etc.) in a file
 */
@Serializable
data class ModifiedCodeRange(
    val filePath: String,
    val elementName: String,
    val elementType: String, // "CLASS", "METHOD", "FUNCTION", etc.
    val startLine: Int,
    val endLine: Int,
    val modifiedLines: List<Int> // Lines that were actually modified within this range
) {
    companion object {
        fun filterIssuesByModifiedRanges(
            issues: List<LintIssue>,
            filePath: String,
            modifiedCodeRanges: Map<String, List<ModifiedCodeRange>>
        ): List<LintIssue> {
            if (modifiedCodeRanges.isEmpty()) {
                return issues
            }

            val ranges = modifiedCodeRanges[filePath] ?: emptyList()
            if (ranges.isEmpty()) {
                return emptyList()
            }

            return issues.filter { issue ->
                ranges.any { range ->
                    issue.line >= range.startLine && issue.line <= range.endLine
                }
            }
        }

    }
}