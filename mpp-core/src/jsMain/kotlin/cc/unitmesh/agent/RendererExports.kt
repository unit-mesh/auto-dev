package cc.unitmesh.agent

import cc.unitmesh.agent.plan.PlanSummaryData
import cc.unitmesh.agent.render.CodingAgentRenderer
import kotlin.js.JsExport

/**
 * JS-friendly plan summary data
 */
@JsExport
data class JsPlanSummaryData(
    val planId: String,
    val title: String,
    val totalSteps: Int,
    val completedSteps: Int,
    val failedSteps: Int,
    val progressPercent: Int,
    val status: String,
    val currentStepDescription: String?
) {
    companion object {
        fun from(summary: PlanSummaryData): JsPlanSummaryData {
            return JsPlanSummaryData(
                planId = summary.planId,
                title = summary.title,
                totalSteps = summary.totalSteps,
                completedSteps = summary.completedSteps,
                failedSteps = summary.failedSteps,
                progressPercent = summary.progressPercent,
                status = summary.status.name,
                currentStepDescription = summary.currentStepDescription
            )
        }
    }
}

/**
 * JS-friendly renderer interface
 * Allows TypeScript to provide custom rendering implementations
 * This interface mirrors the Kotlin CodingAgentRenderer interface
 */
@JsExport
interface JsCodingAgentRenderer {
    // Lifecycle methods
    fun renderIterationHeader(current: Int, max: Int)
    fun renderLLMResponseStart()
    fun renderLLMResponseChunk(chunk: String)
    fun renderLLMResponseEnd()

    // Tool execution methods
    fun renderToolCall(toolName: String, paramsStr: String)
    fun renderToolResult(toolName: String, success: Boolean, output: String?, fullOutput: String?)

    // Status and completion methods
    fun renderTaskComplete()
    fun renderFinalResult(success: Boolean, message: String, iterations: Int)
    fun renderError(message: String)
    fun renderRepeatWarning(toolName: String, count: Int)

    // Error recovery methods
    fun renderRecoveryAdvice(recoveryAdvice: String)

    // Plan summary bar (optional - default no-op in BaseRenderer)
    fun renderPlanSummary(summary: JsPlanSummaryData) {}
}

/**
 * Renderer factory for creating different types of renderers
 */
@JsExport
object RendererFactory {
    /**
     * Create a renderer adapter from JS implementation
     */
    fun createRenderer(jsRenderer: JsCodingAgentRenderer): CodingAgentRenderer {
        return JsRendererAdapter(jsRenderer)
    }
}

/**
 * Adapter to convert JS renderer to Kotlin renderer
 */
class JsRendererAdapter(private val jsRenderer: JsCodingAgentRenderer) : CodingAgentRenderer {
    override fun renderIterationHeader(current: Int, max: Int) {
        jsRenderer.renderIterationHeader(current, max)
    }

    override fun renderLLMResponseStart() {
        jsRenderer.renderLLMResponseStart()
    }

    override fun renderLLMResponseChunk(chunk: String) {
        jsRenderer.renderLLMResponseChunk(chunk)
    }

    override fun renderLLMResponseEnd() {
        jsRenderer.renderLLMResponseEnd()
    }

    override fun renderToolCall(toolName: String, paramsStr: String) {
        jsRenderer.renderToolCall(toolName, paramsStr)
    }

    override fun renderToolResult(toolName: String, success: Boolean, output: String?, fullOutput: String?, metadata: Map<String, String>) {
        jsRenderer.renderToolResult(toolName, success, output, fullOutput)
    }

    override fun renderTaskComplete() {
        jsRenderer.renderTaskComplete()
    }

    override fun renderFinalResult(success: Boolean, message: String, iterations: Int) {
        jsRenderer.renderFinalResult(success, message, iterations)
    }

    override fun renderError(message: String) {
        jsRenderer.renderError(message)
    }

    override fun renderRepeatWarning(toolName: String, count: Int) {
        jsRenderer.renderRepeatWarning(toolName, count)
    }

    override fun renderRecoveryAdvice(recoveryAdvice: String) {
        jsRenderer.renderRecoveryAdvice(recoveryAdvice)
    }

    override fun renderUserConfirmationRequest(toolName: String, params: Map<String, Any>) {
        // For now, just use error rendering since JS renderer doesn't have this method yet
        jsRenderer.renderError("Tool '$toolName' requires user confirmation: $params (Auto-approved)")
    }

    override fun renderPlanSummary(summary: PlanSummaryData) {
        jsRenderer.renderPlanSummary(JsPlanSummaryData.from(summary))
    }
}

