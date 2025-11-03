package cc.unitmesh.agent

import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.render.RendererType
import kotlin.js.JsExport

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

    /**
     * Get renderer type information for JS consumers
     */
    fun getRendererTypes(): Array<String> {
        return RendererType.values().map { it.name }.toTypedArray()
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

    override fun renderToolResult(toolName: String, success: Boolean, output: String?, fullOutput: String?) {
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
}

