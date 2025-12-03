package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.plan.AgentPlan
import cc.unitmesh.agent.plan.MarkdownPlanParser
import cc.unitmesh.agent.plan.PlanStateService
import cc.unitmesh.agent.plan.PlanSummaryData
import cc.unitmesh.agent.plan.TaskStatus as PlanTaskStatus
import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.agent.tool.schema.ToolCategory
import kotlinx.serialization.Serializable

enum class PlanAction { CREATE, UPDATE, COMPLETE_STEP, FAIL_STEP, VIEW }

/**
 * Reference to a specific step within a task
 */
@Serializable
data class StepRef(
    val taskIndex: Int,
    val stepIndex: Int
)

@Serializable
data class PlanManagementParams(
    val action: String,
    val planMarkdown: String = "",
    val taskIndex: Int = 0,
    val stepIndex: Int = 0,
    /**
     * For batch updates: list of steps to complete/fail in one call.
     * When provided, taskIndex and stepIndex are ignored.
     * Example: [{"taskIndex": 1, "stepIndex": 1}, {"taskIndex": 1, "stepIndex": 2}]
     */
    val steps: List<StepRef> = emptyList()
)

object PlanManagementSchema : DeclarativeToolSchema(
    description = "Manage task plans for complex multi-step work.",
    properties = mapOf(
        "action" to string(description = "Action: CREATE (new plan), COMPLETE_STEP (mark steps done), FAIL_STEP (mark steps failed), VIEW (show plan). Use COMPLETE_STEP to update progress - do NOT resend the full plan.", required = true,
            enum = listOf("CREATE", "COMPLETE_STEP", "FAIL_STEP", "VIEW")),
        "planMarkdown" to string(description = "Plan content in markdown format (only for CREATE)", required = false),
        "taskIndex" to string(description = "1-based task index (for single step update)", required = false),
        "stepIndex" to string(description = "1-based step index (for single step update)", required = false),
        "steps" to string(description = "Array of steps to update: [{\"taskIndex\": 1, \"stepIndex\": 1}, ...]. Use this for batch updates.", required = false)
    )
) {
    override fun getExampleUsage(toolName: String): String =
        """/$toolName action="COMPLETE_STEP" steps=[{"taskIndex": 1, "stepIndex": 1}, {"taskIndex": 1, "stepIndex": 2}]"""
}

class PlanManagementInvocation(
    params: PlanManagementParams,
    tool: PlanManagementTool,
    private val planStateService: PlanStateService
) : BaseToolInvocation<PlanManagementParams, ToolResult>(params, tool) {

    override fun getDescription(): String = "Plan Management: ${params.action}"
    override fun getToolLocations(): List<ToolLocation> = emptyList()

    override suspend fun execute(context: ToolExecutionContext): ToolResult {
        val action = try {
            PlanAction.valueOf(params.action.uppercase())
        } catch (e: IllegalArgumentException) {
            return ToolResult.Error("Invalid action: ${params.action}", ToolErrorType.PARAMETER_OUT_OF_RANGE.code)
        }
        return when (action) {
            PlanAction.CREATE -> createPlan()
            PlanAction.UPDATE -> updatePlan()
            PlanAction.COMPLETE_STEP -> updateStepStatus(PlanTaskStatus.COMPLETED)
            PlanAction.FAIL_STEP -> updateStepStatus(PlanTaskStatus.FAILED)
            PlanAction.VIEW -> viewPlan()
        }
    }

    private fun createPlan(): ToolResult {
        if (params.planMarkdown.isBlank()) {
            return ToolResult.Error("planMarkdown is required for CREATE", ToolErrorType.MISSING_REQUIRED_PARAMETER.code)
        }
        val plan = planStateService.createPlanFromMarkdown(params.planMarkdown)
        val summary = PlanSummaryData.from(plan)

        // Return concise summary with task titles
        val taskList = plan.tasks.mapIndexed { idx, task ->
            "${idx + 1}. ${task.title} (${task.steps.size} steps)"
        }.joinToString("\n")

        return ToolResult.Success(
            "Plan created with ${plan.taskCount} tasks, ${summary.totalSteps} steps.\n\n$taskList",
            mapOf(
                "plan_id" to plan.id,
                "task_count" to plan.taskCount.toString(),
                "total_steps" to summary.totalSteps.toString()
            )
        )
    }

    private fun updatePlan(): ToolResult {
        if (params.planMarkdown.isBlank()) {
            return ToolResult.Error("planMarkdown is required for UPDATE", ToolErrorType.MISSING_REQUIRED_PARAMETER.code)
        }
        val tasks = MarkdownPlanParser.parse(params.planMarkdown)
        if (planStateService.currentPlan.value == null) {
            val plan = planStateService.createPlanFromMarkdown(params.planMarkdown)
            return ToolResult.Success("Plan created with ${plan.taskCount} tasks.\n\n${plan.toMarkdown()}",
                mapOf("plan_id" to plan.id, "task_count" to plan.taskCount.toString()))
        }
        planStateService.updatePlan(tasks)
        val updatedPlan = planStateService.currentPlan.value!!
        return ToolResult.Success("Plan updated with ${updatedPlan.taskCount} tasks.\n\n${updatedPlan.toMarkdown()}",
            mapOf("plan_id" to updatedPlan.id, "task_count" to updatedPlan.taskCount.toString()))
    }

    private fun updateStepStatus(status: PlanTaskStatus): ToolResult {
        val currentPlan = planStateService.currentPlan.value
            ?: return ToolResult.Error("No active plan", ToolErrorType.FILE_NOT_FOUND.code)

        // Determine which steps to update: batch (steps array) or single (taskIndex/stepIndex)
        val stepsToUpdate = if (params.steps.isNotEmpty()) {
            params.steps
        } else if (params.taskIndex > 0 && params.stepIndex > 0) {
            listOf(StepRef(params.taskIndex, params.stepIndex))
        } else {
            return ToolResult.Error("Either 'steps' array or both 'taskIndex' and 'stepIndex' must be provided",
                ToolErrorType.MISSING_REQUIRED_PARAMETER.code)
        }

        val updatedSteps = mutableListOf<String>()
        val statusText = if (status == PlanTaskStatus.COMPLETED) "completed" else "failed"

        for (stepRef in stepsToUpdate) {
            val taskIdx = stepRef.taskIndex - 1
            val stepIdx = stepRef.stepIndex - 1

            if (taskIdx < 0 || taskIdx >= currentPlan.tasks.size) {
                return ToolResult.Error("Task index ${stepRef.taskIndex} out of range", ToolErrorType.PARAMETER_OUT_OF_RANGE.code)
            }
            val task = currentPlan.tasks[taskIdx]
            if (stepIdx < 0 || stepIdx >= task.steps.size) {
                return ToolResult.Error("Step index ${stepRef.stepIndex} out of range for task ${stepRef.taskIndex}", ToolErrorType.PARAMETER_OUT_OF_RANGE.code)
            }
            val step = task.steps[stepIdx]
            planStateService.updateStepStatus(task.id, step.id, status)
            updatedSteps.add("[${stepRef.taskIndex}.${stepRef.stepIndex}] ${step.description}")
        }

        val updatedPlan = planStateService.currentPlan.value!!
        val summary = PlanSummaryData.from(updatedPlan)

        // Return concise summary instead of full markdown
        val resultMessage = if (stepsToUpdate.size == 1) {
            "Step $statusText: ${updatedSteps.first()}"
        } else {
            "${stepsToUpdate.size} steps $statusText:\n${updatedSteps.joinToString("\n") { "  - $it" }}"
        }

        return ToolResult.Success(
            "$resultMessage\n\nProgress: ${summary.completedSteps}/${summary.totalSteps} (${summary.progressPercent}%)",
            mapOf(
                "steps_updated" to stepsToUpdate.size.toString(),
                "progress" to "${summary.progressPercent}%",
                "completed" to summary.completedSteps.toString(),
                "total" to summary.totalSteps.toString(),
                "status" to status.name
            )
        )
    }

    private fun viewPlan(): ToolResult {
        val currentPlan = planStateService.currentPlan.value
            ?: return ToolResult.Success("No active plan.", mapOf("has_plan" to "false"))
        return ToolResult.Success(currentPlan.toMarkdown(), mapOf(
            "plan_id" to currentPlan.id, "task_count" to currentPlan.taskCount.toString(),
            "progress" to "${currentPlan.progressPercent}%", "status" to currentPlan.status.name))
    }
}

/**
 * Plan Management Tool - for complex multi-step tasks
 *
 * ## Purpose
 * Create and track structured plans with tasks and steps. This helps organize complex work
 * and communicate progress to users through a visual plan UI.
 *
 * ## When to Use
 * - Tasks requiring multiple files to be created or modified
 * - Tasks with dependencies between steps
 * - Complex refactoring or feature implementation
 * - Any work that benefits from structured tracking (3+ steps)
 *
 * ## When NOT to Use
 * - Simple one-step tasks (answering questions, quick refactors)
 * - Single-file edits
 * - Trivial operations
 *
 * ## Plan Format (Markdown)
 * ```
 * 1. Task Title
 *    - [ ] Step 1 description
 *    - [ ] Step 2 description
 *
 * 2. Another Task
 *    - [ ] Step description
 * ```
 *
 * ## Example Flow
 * ```
 * /plan action="CREATE" planMarkdown="1. Setup\n   - [ ] Create entity\n   - [ ] Create repository\n\n2. Implementation\n   - [ ] Create service\n   - [ ] Create controller"
 * // ... create entity ...
 * /plan action="COMPLETE_STEP" taskIndex=1 stepIndex=1
 * // ... create repository ...
 * /plan action="COMPLETE_STEP" taskIndex=1 stepIndex=2
 * // ... continue ...
 * ```
 */
class PlanManagementTool(
    private val planStateService: PlanStateService = PlanStateService()
) : BaseExecutableTool<PlanManagementParams, ToolResult>() {

    override val name: String = "plan"
    override val description: String = """
        Manage task plans for complex multi-step work. Create structured plans with tasks and steps,
        then track progress by marking steps as completed or failed.

        Actions:
        - CREATE: Create a new plan from markdown (planMarkdown required)
        - COMPLETE_STEP: Mark a step as completed (taskIndex and stepIndex required, 1-based)
        - FAIL_STEP: Mark a step as failed
        - VIEW: View current plan status

        IMPORTANT: Use COMPLETE_STEP to mark progress. Do NOT resend the full plan markdown to update progress.
        Use for complex tasks (3+ steps). Skip for simple one-step tasks.
    """.trimIndent()

    override val metadata: ToolMetadata = ToolMetadata(
        displayName = "Plan Management", tuiEmoji = "📋", composeIcon = "plan",
        category = ToolCategory.Utility, schema = PlanManagementSchema
    )

    override fun getParameterClass(): String = PlanManagementParams::class.simpleName ?: "PlanManagementParams"

    override fun createToolInvocation(params: PlanManagementParams): ToolInvocation<PlanManagementParams, ToolResult> {
        if (params.action.isBlank()) throw ToolException("Action cannot be empty", ToolErrorType.MISSING_REQUIRED_PARAMETER)
        try { PlanAction.valueOf(params.action.uppercase()) }
        catch (e: IllegalArgumentException) { throw ToolException("Invalid action: ${params.action}", ToolErrorType.PARAMETER_OUT_OF_RANGE) }
        return PlanManagementInvocation(params, this, planStateService)
    }

    fun getPlanStateService(): PlanStateService = planStateService
}

