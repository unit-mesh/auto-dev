package cc.unitmesh.agent.subagent

import cc.unitmesh.agent.core.SubAgent
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.model.ModelConfig
import cc.unitmesh.agent.model.PromptConfig
import cc.unitmesh.agent.model.RunConfig
import cc.unitmesh.llm.KoogLLMService
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 日志摘要 SubAgent
 * 
 * 总结长命令输出
 * 从 TypeScript 版本移植
 */
class LogSummaryAgent(
    private val llmService: KoogLLMService,
    private val threshold: Int = 2000
) : SubAgent<LogSummaryContext, LogSummaryResult>(
    definition = createDefinition()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 检查输出是否需要摘要
     */
    fun needsSummarization(output: String): Boolean = output.length > threshold

    override fun validateInput(input: Map<String, Any>): LogSummaryContext {
        return LogSummaryContext(
            command = input["command"] as? String ?: throw IllegalArgumentException("command is required"),
            output = input["output"] as? String ?: throw IllegalArgumentException("output is required"),
            exitCode = (input["exitCode"] as? Number)?.toInt() ?: 0,
            executionTime = (input["executionTime"] as? Number)?.toInt() ?: 0
        )
    }

    override suspend fun execute(
        input: LogSummaryContext,
        onProgress: (String) -> Unit
    ): LogSummaryResult {
        onProgress("Starting log analysis...")

        // Quick heuristic analysis first
        val heuristics = quickAnalysis(input)
        onProgress("Performing AI analysis...")

        // Build prompt for AI analysis
        val prompt = buildAnalysisPrompt(input, heuristics)

        return try {
            val aiResponse = llmService.sendPrompt(
                "${getSystemPrompt()}\n\n$prompt"
            )
            onProgress("Parsing results...")
            parseResponse(aiResponse, heuristics, input)
        } catch (e: Exception) {
            // Fallback to heuristic analysis if AI fails
            onProgress("AI analysis failed, using heuristics")
            heuristicFallback(input, heuristics)
        }
    }

    override fun formatOutput(output: LogSummaryResult): String {
        return buildString {
            appendLine("📊 Summary: ${output.summary}")

            if (output.keyPoints.isNotEmpty()) {
                appendLine()
                appendLine("🔍 Key Points:")
                output.keyPoints.forEach { point ->
                    appendLine("  • $point")
                }
            }

            if (output.errors.isNotEmpty()) {
                appendLine()
                appendLine("❌ Errors:")
                output.errors.forEach { error ->
                    appendLine("  • ${error.trim()}")
                }
            }

            if (output.warnings.isNotEmpty()) {
                appendLine()
                appendLine("⚠️  Warnings:")
                output.warnings.forEach { warning ->
                    appendLine("  • ${warning.trim()}")
                }
            }

            if (output.statistics != null) {
                appendLine()
                appendLine("📈 Statistics: ${output.statistics.totalLines} lines, ${output.statistics.errorCount} errors, ${output.statistics.warningCount} warnings")
            }

            if (output.nextSteps != null && output.nextSteps.isNotEmpty()) {
                appendLine()
                appendLine("💡 Next Steps:")
                output.nextSteps.forEach { step ->
                    appendLine("  • $step")
                }
            }
        }
    }

    /**
     * 快速启发式分析
     */
    private fun quickAnalysis(context: LogSummaryContext): Heuristics {
        val lines = context.output.split("\n")
        val lowerOutput = context.output.lowercase()

        var errorCount = 0
        var warningCount = 0
        var successIndicators = 0
        var failureIndicators = 0

        for (line in lines) {
            val lower = line.lowercase()

            // Count errors
            if (lower.contains("error") || lower.contains("failed") || lower.contains("exception")) {
                errorCount++
            }

            // Count warnings
            if (lower.contains("warn") || lower.contains("deprecated")) {
                warningCount++
            }

            // Success indicators
            if (lower.contains("success") || lower.contains("passed") || lower.contains("✓")) {
                successIndicators++
            }

            // Failure indicators
            if (lower.contains("fail") || lower.contains("✗")) {
                failureIndicators++
            }
        }

        return Heuristics(
            totalLines = lines.size,
            errorCount = errorCount,
            warningCount = warningCount,
            hasTestResults = lowerOutput.contains("test") && (lowerOutput.contains("passed") || lowerOutput.contains("failed")),
            hasBuildInfo = lowerOutput.contains("build") && (lowerOutput.contains("success") || lowerOutput.contains("failed")),
            successIndicators = successIndicators,
            failureIndicators = failureIndicators
        )
    }

    /**
     * 系统提示词
     */
    private fun getSystemPrompt(): String {
        return """
You are a Log Summary Agent specialized in analyzing command-line output.

Your task is to analyze build logs, test results, and command outputs, then provide:
1. A concise summary (1-2 sentences)
2. Key points (3-5 bullet points)
3. Any errors found
4. Any warnings found
5. Suggested next steps (if applicable)

Response format (JSON):
{
  "summary": "Brief overview",
  "keyPoints": ["point1", "point2", ...],
  "errors": ["error1", "error2", ...],
  "warnings": ["warning1", "warning2", ...],
  "nextSteps": ["step1", "step2", ...]
}

Keep it concise and actionable. Focus on what matters most.
""".trimIndent()
    }

    /**
     * 构建分析提示词
     */
    private fun buildAnalysisPrompt(context: LogSummaryContext, heuristics: Heuristics): String {
        // Truncate very long outputs for AI analysis
        val maxCharsForAI = 8000
        val outputForAI = if (context.output.length > maxCharsForAI) {
            val headSize = (maxCharsForAI * 0.6).toInt()
            val tailSize = (maxCharsForAI * 0.4).toInt()
            context.output.substring(0, headSize) +
                    "\n\n... [truncated ${context.output.length - maxCharsForAI} chars] ...\n\n" +
                    context.output.substring(context.output.length - tailSize)
        } else {
            context.output
        }

        return """
Analyze this command output:

**Command**: `${context.command}`
**Exit Code**: ${context.exitCode}
**Execution Time**: ${context.executionTime}ms
**Output Length**: ${context.output.length} chars, ${heuristics.totalLines} lines

**Heuristic Analysis**:
- Errors detected: ${heuristics.errorCount}
- Warnings detected: ${heuristics.warningCount}
- Success indicators: ${heuristics.successIndicators}
- Failure indicators: ${heuristics.failureIndicators}
- Contains test results: ${heuristics.hasTestResults}
- Contains build info: ${heuristics.hasBuildInfo}

**Output**:
```
$outputForAI
```

Provide a JSON summary as specified in your system prompt.
""".trimIndent()
    }

    /**
     * 解析 AI 响应
     */
    private fun parseResponse(
        aiResponse: String,
        heuristics: Heuristics,
        context: LogSummaryContext
    ): LogSummaryResult {
        return try {
            val jsonMatch = Regex("\\{[\\s\\S]*?\\}").find(aiResponse)
            if (jsonMatch != null) {
                val parsed = json.decodeFromString<LogSummaryResultJson>(jsonMatch.value)
                LogSummaryResult(
                    success = context.exitCode == 0,
                    summary = parsed.summary ?: "Command executed",
                    keyPoints = parsed.keyPoints ?: emptyList(),
                    errors = parsed.errors ?: emptyList(),
                    warnings = parsed.warnings ?: emptyList(),
                    statistics = Statistics(
                        totalLines = heuristics.totalLines,
                        errorCount = heuristics.errorCount,
                        warningCount = heuristics.warningCount
                    ),
                    nextSteps = parsed.nextSteps
                )
            } else {
                heuristicFallback(context, heuristics)
            }
        } catch (e: Exception) {
            heuristicFallback(context, heuristics)
        }
    }

    /**
     * 启发式回退
     */
    private fun heuristicFallback(
        context: LogSummaryContext,
        heuristics: Heuristics
    ): LogSummaryResult {
        val success = context.exitCode == 0
        val lines = context.output.split("\n")

        // Extract error lines
        val errors = lines
            .filter { line ->
                val lower = line.lowercase()
                lower.contains("error") || lower.contains("exception") || lower.contains("failed")
            }
            .take(5)

        // Extract warning lines
        val warnings = lines
            .filter { line ->
                val lower = line.lowercase()
                lower.contains("warn") || lower.contains("deprecated")
            }
            .take(3)

        // Generate summary
        val summary = when {
            success && heuristics.hasBuildInfo -> "Build completed successfully in ${context.executionTime}ms"
            success && heuristics.hasTestResults -> "Tests completed in ${context.executionTime}ms"
            success -> "Command completed successfully in ${context.executionTime}ms"
            else -> "Command failed with exit code ${context.exitCode}"
        }

        // Generate key points
        val keyPoints = mutableListOf<String>()
        if (heuristics.totalLines > 100) {
            keyPoints.add("Output contains ${heuristics.totalLines} lines")
        }
        if (heuristics.errorCount > 0) {
            keyPoints.add("Found ${heuristics.errorCount} error messages")
        }
        if (heuristics.warningCount > 0) {
            keyPoints.add("Found ${heuristics.warningCount} warnings")
        }
        if (heuristics.hasTestResults) {
            keyPoints.add("Contains test execution results")
        }

        return LogSummaryResult(
            success = success,
            summary = summary,
            keyPoints = keyPoints,
            errors = errors,
            warnings = warnings,
            statistics = Statistics(
                totalLines = heuristics.totalLines,
                errorCount = heuristics.errorCount,
                warningCount = heuristics.warningCount
            ),
            nextSteps = if (success) emptyList() else listOf("Check error messages above", "Fix the issues and retry")
        )
    }

    companion object {
        private fun createDefinition() = AgentDefinition(
            name = "log_summary",
            displayName = "Log Summary SubAgent",
            description = "Summarizes long command outputs",
            promptConfig = PromptConfig(
                systemPrompt = "You are a Log Summary Agent specialized in analyzing command outputs."
            ),
            modelConfig = ModelConfig(modelId = "gpt-4"),
            runConfig = RunConfig(maxTurns = 3, maxTimeMinutes = 1)
        )
    }
}

/**
 * 日志摘要上下文
 */
@Serializable
data class LogSummaryContext(
    val command: String,
    val output: String,
    val exitCode: Int,
    val executionTime: Int
)

/**
 * 日志摘要结果
 */
@Serializable
data class LogSummaryResult(
    val success: Boolean,
    val summary: String,
    val keyPoints: List<String>,
    val errors: List<String>,
    val warnings: List<String>,
    val statistics: Statistics? = null,
    val nextSteps: List<String>? = null
)

/**
 * 统计信息
 */
@Serializable
data class Statistics(
    val totalLines: Int,
    val errorCount: Int,
    val warningCount: Int
)

/**
 * 启发式分析结果
 */
private data class Heuristics(
    val totalLines: Int,
    val errorCount: Int,
    val warningCount: Int,
    val hasTestResults: Boolean,
    val hasBuildInfo: Boolean,
    val successIndicators: Int,
    val failureIndicators: Int
)

/**
 * JSON 解析用的数据类
 */
@Serializable
private data class LogSummaryResultJson(
    val summary: String? = null,
    val keyPoints: List<String>? = null,
    val errors: List<String>? = null,
    val warnings: List<String>? = null,
    val nextSteps: List<String>? = null
)

