package cc.unitmesh.agent

import cc.unitmesh.agent.linter.LinterSummary
import cc.unitmesh.agent.tool.AgentToolFormatter
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
            "toolList",
            VariableType.STRING,
            toolList
        )

        return table
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

    }
}