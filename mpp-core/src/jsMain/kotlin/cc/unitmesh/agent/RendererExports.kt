package cc.unitmesh.agent

import cc.unitmesh.agent.render.CodingAgentRenderer
import kotlin.js.JsExport

/**
 * JS-friendly renderer interface
 * Allows TypeScript to provide custom rendering implementations
 */
@JsExport
interface JsCodingAgentRenderer {
    fun renderIterationHeader(current: Int, max: Int)
    fun renderLLMResponseStart()
    fun renderLLMResponseChunk(chunk: String)
    fun renderLLMResponseEnd()
    fun renderToolCall(toolName: String, paramsStr: String)
    fun renderToolResult(toolName: String, success: Boolean, output: String?, fullOutput: String?)
    fun renderTaskComplete()
    fun renderFinalResult(success: Boolean, message: String, iterations: Int)
    fun renderError(message: String)
    fun renderRepeatWarning(toolName: String, count: Int)
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

