package cc.unitmesh.agent.render

/**
 * Default console renderer
 */
class DefaultCodingAgentRenderer : CodingAgentRenderer {
    private val reasoningBuffer = StringBuilder()
    private var isInDevinBlock = false

    override fun renderIterationHeader(current: Int, max: Int) {
        println("\n[$current/$max] Analyzing and executing...")
    }

    override fun renderLLMResponseStart() {
        reasoningBuffer.clear()
        isInDevinBlock = false
        print("💭 ")
    }

    override fun renderLLMResponseChunk(chunk: String) {
        // Parse chunk to detect devin blocks
        reasoningBuffer.append(chunk)
        val text = reasoningBuffer.toString()

        // Check if we're entering or leaving a devin block
        if (text.contains("<devin>")) {
            isInDevinBlock = true
        }
        if (text.contains("</devin>")) {
            isInDevinBlock = false
        }

        // Only print if not in devin block
        if (!isInDevinBlock && !chunk.contains("<devin>") && !chunk.contains("</devin>")) {
            print(chunk)
        }
    }

    override fun renderLLMResponseEnd() {
        println("\n")
    }

    override fun renderToolCall(toolName: String, paramsStr: String) {
        println("🔧 /$toolName $paramsStr")
    }

    override fun renderToolResult(toolName: String, success: Boolean, output: String?, fullOutput: String?) {
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
}