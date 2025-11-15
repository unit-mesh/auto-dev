package cc.unitmesh.agent

import cc.unitmesh.agent.linter.LinterSummary
import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.devins.compiler.variable.VariableTable
import cc.unitmesh.devins.compiler.variable.VariableType

/**
 * Context for code review
 */
data class CodeReviewContext(
    val projectPath: String,
    val filePaths: List<String>,
    val reviewType: ReviewType,
    val additionalContext: String,
    val toolList: String,
    val linterSummary: LinterSummary? = null
) : AgentContext {

    /**
     * Convert context to variable table for template compilation
     */
    override fun toVariableTable(): VariableTable {
        val table = VariableTable()

        table.addVariable(
            "projectPath",
            VariableType.STRING,
            projectPath
        )
        table.addVariable(
            "reviewType",
            VariableType.STRING,
            reviewType.name
        )
        table.addVariable(
            "filePaths",
            VariableType.STRING,
            filePaths.joinToString(", ")
        )
        table.addVariable(
            "toolList",
            VariableType.STRING,
            toolList
        )
        table.addVariable(
            "additionalContext",
            VariableType.STRING,
            additionalContext
        )
        table.addVariable(
            "linterInfo",
            VariableType.STRING,
            formatLinterInfo()
        )

        return table
    }

    /**
     * Format linter information for display in prompts
     */
    private fun formatLinterInfo(): String {
        if (linterSummary == null) {
            return "No linter information available."
        }

        return buildString {
            appendLine("### Available Linters")
            appendLine()

            if (linterSummary.availableLinters.isNotEmpty()) {
                appendLine("**Installed and Ready (${linterSummary.availableLinters.size}):**")
                linterSummary.availableLinters.forEach { linter ->
                    appendLine("- **${linter.name}** ${linter.version?.let { "($it)" } ?: ""}")
                    if (linter.supportedFiles.isNotEmpty()) {
                        appendLine("  - Files: ${linter.supportedFiles.joinToString(", ")}")
                    }
                }
                appendLine()
            }

            if (linterSummary.unavailableLinters.isNotEmpty()) {
                appendLine("**Not Installed (${linterSummary.unavailableLinters.size}):**")
                linterSummary.unavailableLinters.forEach { linter ->
                    appendLine("- **${linter.name}**")
                    linter.installationInstructions?.let {
                        appendLine("  - Install: $it")
                    }
                }
                appendLine()
            }

            if (linterSummary.fileMapping.isNotEmpty()) {
                appendLine("### File-Linter Mapping")
                linterSummary.fileMapping.forEach { (file, linters) ->
                    appendLine("- `$file` → ${linters.joinToString(", ")}")
                }
            }
        }
    }

    companion object {
        /**
         * Create CodeReviewContext from ReviewTask
         *
         * @param task The review task
         * @param toolList List of available tools
         * @param linterSummary Optional linter summary
         * @return CodeReviewContext with formatted tool list
         */
        fun fromTask(
            task: ReviewTask,
            toolList: List<ExecutableTool<*, *>>,
            linterSummary: LinterSummary? = null
        ): CodeReviewContext {
            return CodeReviewContext(
                projectPath = task.projectPath,
                filePaths = task.filePaths,
                reviewType = task.reviewType,
                additionalContext = task.additionalContext,
                toolList = AgentToolFormatter.formatToolListForAI(toolList),
                linterSummary = linterSummary
            )
        }

        /**
         * Create CodeReviewContext with simple tool list formatting
         *
         * @param task The review task
         * @param toolList List of available tools
         * @param linterSummary Optional linter summary
         * @return CodeReviewContext with simple tool list
         */
        fun fromTaskSimple(
            task: ReviewTask,
            toolList: List<ExecutableTool<*, *>>,
            linterSummary: LinterSummary? = null
        ): CodeReviewContext {
            return CodeReviewContext(
                projectPath = task.projectPath,
                filePaths = task.filePaths,
                reviewType = task.reviewType,
                additionalContext = task.additionalContext,
                toolList = AgentToolFormatter.formatToolListSimple(toolList),
                linterSummary = linterSummary
            )
        }
    }
}