package cc.unitmesh.agent.executor

import cc.unitmesh.agent.CodeReviewContext
import cc.unitmesh.agent.CodeReviewResult
import cc.unitmesh.agent.ReviewFinding
import cc.unitmesh.agent.ReviewTask
import cc.unitmesh.agent.Severity
import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.llm.KoogLLMService

/**
 * Executor for CodeReviewAgent
 * Handles the execution flow for code review tasks
 */
class CodeReviewAgentExecutor(
    private val projectPath: String,
    private val llmService: KoogLLMService,
    private val toolRegistry: ToolRegistry,
    private val fileSystem: ToolFileSystem,
    private val renderer: CodingAgentRenderer,
    private val maxIterations: Int = 50,
    private val enableLLMStreaming: Boolean = true
) {
    private val logger = getLogger("CodeReviewAgentExecutor")

    suspend fun execute(
        task: ReviewTask,
        systemPrompt: String,
        onProgress: (String) -> Unit = {}
    ): CodeReviewResult {
        logger.info { "Starting code review: ${task.reviewType} for ${task.filePaths.size} files" }

        onProgress("🔍 Starting code review...")
        // Note: renderTaskStart is not available in base renderer

        try {
            // Build user message
            val userMessage = buildUserMessage(task)

            onProgress("📖 Reading files for review...")

            // Log the prompts for debugging
            logger.info { "System prompt length: ${systemPrompt.length} characters" }
            logger.debug { "System prompt preview: ${systemPrompt.take(500)}..." }
            logger.info { "User message length: ${userMessage.length} characters" }
            logger.debug { "User message preview: ${userMessage.take(500)}..." }

            // Call LLM for analysis
            renderer.renderLLMResponseStart()

            val response = if (enableLLMStreaming) {
                callLLMStreaming(systemPrompt, userMessage, onProgress)
            } else {
                callLLM(systemPrompt, userMessage, onProgress)
            }

            renderer.renderLLMResponseEnd()

            onProgress("✅ Review complete")

            // Parse findings from response
            val findings = parseFindings(response)

            renderer.renderTaskComplete()

            return CodeReviewResult(
                success = true,
                message = response,
                findings = findings
            )
        } catch (e: Exception) {
            logger.error(e) { "Code review failed: ${e.message}" }
            renderer.renderError("Review failed: ${e.message}")

            return CodeReviewResult(
                success = false,
                message = "Review failed: ${e.message}",
                findings = emptyList()
            )
        }
    }

    private suspend fun buildUserMessage(task: ReviewTask): String {
        logger.info { "Building user message for ${task.filePaths.size} files" }
        logger.info { "Project path: $projectPath" }

        return buildString {
            appendLine("Please review the following code:")
            appendLine()

            if (task.filePaths.isNotEmpty()) {
                appendLine("Files to review (${task.filePaths.size} files):")
                appendLine()

                task.filePaths.forEach { filePath ->
                    appendLine("## File: $filePath")
                    appendLine()

                    try {
                        // Read file content
                        // Normalize path: if it starts with /, it's absolute; otherwise join with projectPath
                        val fullPath = if (filePath.startsWith("/")) {
                            filePath
                        } else {
                            "$projectPath/$filePath".replace("//", "/")
                        }

                        logger.info { "Reading file: $fullPath" }
                        val content = fileSystem.readFile(fullPath)

                        if (content != null) {
                            val lineCount = content.lines().size
                            logger.info { "Successfully read file $filePath: $lineCount lines" }

                            // Add file content with line numbers
                            appendLine("```")
                            content.lines().forEachIndexed { index, line ->
                                appendLine("${index + 1}: $line")
                            }
                            appendLine("```")
                            appendLine()
                        } else {
                            logger.warn { "File content is null for: $fullPath" }
                            appendLine("(File is empty or could not be read)")
                            appendLine()
                        }
                    } catch (e: Exception) {
                        logger.warn { "Failed to read file $filePath: ${e.message}" }
                        appendLine("(Unable to read file content: ${e.message})")
                        appendLine()
                    }
                }
            }

            appendLine("Review type: ${task.reviewType}")

            if (task.additionalContext.isNotBlank()) {
                appendLine()
                appendLine("Additional context:")
                appendLine(task.additionalContext)
            }

            appendLine()
            appendLine("Please provide a thorough code review following the guidelines in the system prompt.")
        }.also { message ->
            logger.info { "User message length: ${message.length} characters" }
            logger.debug { "User message preview: ${message.take(500)}..." }
        }
    }

    private suspend fun callLLM(
        systemPrompt: String,
        userMessage: String,
        onProgress: (String) -> Unit
    ): String {
        onProgress("🤖 Analyzing code...")

        // Build history messages with system prompt
        val historyMessages = listOf(
            cc.unitmesh.devins.llm.Message(
                role = cc.unitmesh.devins.llm.MessageRole.SYSTEM,
                content = systemPrompt
            )
        )

        val response = StringBuilder()
        llmService.streamPrompt(
            userPrompt = userMessage,
            historyMessages = historyMessages,
            compileDevIns = false  // Don't compile DevIns for code review
        ).collect { chunk ->
            response.append(chunk)
        }

        return response.toString()
    }

    private suspend fun callLLMStreaming(
        systemPrompt: String,
        userMessage: String,
        onProgress: (String) -> Unit
    ): String {
        onProgress("🤖 Analyzing code...")
        val historyMessages = listOf(
            cc.unitmesh.devins.llm.Message(
                role = cc.unitmesh.devins.llm.MessageRole.SYSTEM,
                content = systemPrompt
            )
        )

        val fullResponse = StringBuilder()

        llmService.streamPrompt(
            userPrompt = userMessage,
            historyMessages = historyMessages,
            compileDevIns = false  // Don't compile DevIns for code review
        ).collect { chunk ->
            fullResponse.append(chunk)
            renderer.renderLLMResponseChunk(chunk)
        }

        return fullResponse.toString()
    }

    private fun parseFindings(response: String): List<ReviewFinding> {
        // Simple parsing logic - can be enhanced
        val findings = mutableListOf<ReviewFinding>()

        // Look for common patterns indicating severity
        val lines = response.lines()
        var currentSeverity = Severity.INFO

        for (line in lines) {
            when {
                line.contains("CRITICAL", ignoreCase = true) -> currentSeverity = Severity.CRITICAL
                line.contains("HIGH", ignoreCase = true) -> currentSeverity = Severity.HIGH
                line.contains("MEDIUM", ignoreCase = true) -> currentSeverity = Severity.MEDIUM
                line.contains("LOW", ignoreCase = true) -> currentSeverity = Severity.LOW
                line.startsWith("-") || line.startsWith("*") -> {
                    // Extract finding from bullet point
                    val description = line.trimStart('-', '*', ' ')
                    if (description.length > 10) {
                        findings.add(
                            ReviewFinding(
                                severity = currentSeverity,
                                category = "General",
                                description = description
                            )
                        )
                    }
                }
            }
        }

        return findings
    }
}
