package cc.unitmesh.agent.render

import cc.unitmesh.agent.logging.getLogger

/**
 * Default console renderer - simple text output
 * Suitable for basic console applications and testing
 */
class DefaultCodingAgentRenderer : BaseRenderer() {

    private val logger = getLogger("DefaultCodingAgentRenderer")

    override fun renderIterationHeader(current: Int, max: Int) {
        logger.info { "\n[$current/$max] Analyzing and executing..." }
        println("\n[$current/$max] Analyzing and executing...")
    }

    override fun renderLLMResponseStart() {
        super.renderLLMResponseStart()
//        print("💭 ")
    }

    override fun renderLLMResponseChunk(chunk: String) {
        reasoningBuffer.append(chunk)

        // Wait for more content if we detect an incomplete devin block
        if (hasIncompleteDevinBlock(reasoningBuffer.toString())) {
            return
        }

        // Filter devin blocks and output clean content
        val processedContent = filterDevinBlocks(reasoningBuffer.toString())
        val cleanContent = cleanNewlines(processedContent)

        // Simple output for default renderer
        print(cleanContent)
    }

    override fun renderLLMResponseEnd() {
        super.renderLLMResponseEnd()
        logger.debug { "LLM response ended" }
        println("\n")
    }

    override fun renderToolCall(toolName: String, paramsStr: String) {
        logger.info { "Tool call: $toolName $paramsStr" }
        println("🔧 /$toolName $paramsStr")
    }

    override fun renderToolResult(toolName: String, success: Boolean, output: String?, fullOutput: String?, metadata: Map<String, String>) {
        val icon = if (success) "✓" else "✗"
        print("   $icon $toolName")

        // Show key result info if available
        if (success && output != null) {
            // For read-file, show full content (no truncation) so LLM can see complete file
            // For other tools, show preview (300 chars)
            val shouldTruncate = toolName != "read-file"
            val maxLength = if (shouldTruncate) 300 else Int.MAX_VALUE

            val preview = if (output.length > maxLength) output.take(maxLength) else output
            if (preview.isNotEmpty() && !preview.startsWith("Successfully")) {
                print(" → ${preview.replace("\n", " ")}")
                if (shouldTruncate && output.length > maxLength) print("...")
            }
        }
        logger.debug { "Tool result: $toolName success=$success" }
        println()
    }

    override fun renderTaskComplete() {
        println("✓ Task marked as complete\n")
    }

    override fun renderFinalResult(success: Boolean, message: String, iterations: Int) {
        val icon = if (success) "✅" else "⚠️ "
        println("\n$icon $message")
    }

    override fun renderError(message: String) {
        println("❌ $message")
    }

    override fun renderRepeatWarning(toolName: String, count: Int) {
        println("⚠️  Warning: Tool '$toolName' has been called $count times in a row")
    }

    override fun renderRecoveryAdvice(recoveryAdvice: String) {
        println("\n🔧 ERROR RECOVERY ADVICE:")
        println("─".repeat(50))
        // Split by lines and add proper indentation
        recoveryAdvice.lines().forEach { line ->
            if (line.trim().isNotEmpty()) {
                println("   $line")
            } else {
                println()
            }
        }
        println("─".repeat(50))
    }

    override fun renderUserConfirmationRequest(toolName: String, params: Map<String, Any>) {
        println("🔐 Tool '$toolName' requires user confirmation")
        println("   Parameters: ${params.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
        println("   (Auto-approved for now)")
    }
}