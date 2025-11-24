package cc.unitmesh.agent.subagent

import cc.unitmesh.agent.core.SubAgent
import cc.unitmesh.agent.logging.logger
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.model.PromptConfig
import cc.unitmesh.agent.model.RunConfig
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * AnalysisAgent Schema 定义
 */
object ContentHandlerSchema : DeclarativeToolSchema(
    description = "Intelligently analyze and summarize any type of content",
    properties = mapOf(
        "content" to string(
            description = "The content to be analyzed and summarized",
            required = true
        ),
        "contentType" to string(
            description = "Type of content: text, json, xml, log, code, error, file-list"
        ),
        "source" to string(
            description = "Source of the content (tool name or origin)"
        ),
        "metadata" to string(
            description = "Additional metadata about the content as JSON string"
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return "/$toolName content=\"content to analyze...\" contentType=\"log\" source=\"shell\""
    }
}

/**
 * 内容分析上下文
 */
@Serializable
data class ContentHandlerContext(
    val content: String,
    val contentType: String = "text", // text, json, xml, log, code, error, file-list
    val source: String = "unknown", // tool name or source
    val metadata: Map<String, String> = emptyMap()
) {
    override fun toString(): String = "AnalysisContext(contentType=$contentType, source=$source, length=${content.length})"
}

@Serializable
data class ContentHandlerResult(
    val summary: String,
    val keyPoints: List<String> = emptyList(),
    val structure: Map<String, String> = emptyMap(),
    val insights: List<String> = emptyList(),
    val originalLength: Int,
    val processedAt: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
)

class AnalysisAgent(
    private val llmService: KoogLLMService,
    private val contentThreshold: Int = 128000
) : SubAgent<ContentHandlerContext, ToolResult.AgentResult>(
    definition = createDefinition()
) {
    private val contentHistory = mutableListOf<Pair<ContentHandlerContext, ContentHandlerResult>>()
    private val conversationContext = mutableMapOf<String, Any>()
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private fun createDefinition() = AgentDefinition(
            name = ToolType.AnalysisAgent.name,
            displayName = "Analysis Agent",
            description = "Intelligently analyzes and summarizes any type of content with conversational access",
            promptConfig = PromptConfig(
                systemPrompt = buildSystemPrompt(),
                queryTemplate = null,
                initialMessages = emptyList()
            ),
            modelConfig = ModelConfig.default(),
            runConfig = RunConfig(
                maxTurns = 10,
                maxTimeMinutes = 5,
                terminateOnError = false
            )
        )

        private fun buildSystemPrompt(): String = """
            You are an Analysis Agent specialized in intelligently analyzing and summarizing any type of content.

            Your responsibilities:
            1. Analyze content of any type (logs, errors, file lists, code, data) and provide structured summaries
            2. Extract key points and actionable insights from complex information
            3. Maintain conversation context about analyzed content
            4. Answer questions about previously analyzed content
            5. Provide intelligent content navigation and search
            6. Handle both long content and specific content types that need analysis

            When analyzing content:
            - Identify the content type and structure automatically
            - Extract the most important and actionable information
            - Provide relevant insights based on content type
            - Maintain context for future questions
            - Focus on what developers need to know

            When answering questions:
            - Reference specific parts of the analyzed content
            - Provide context-aware and actionable responses
            - Help users understand and navigate complex information
        """.trimIndent()
    }

    override fun getParameterClass(): String = ContentHandlerContext::class.simpleName ?: "ContentHandlerContext"

    override fun validateInput(input: Map<String, Any>): ContentHandlerContext {
        val content = input["content"] as? String
            ?: throw IllegalArgumentException("content is required")
        val contentType = input["contentType"] as? String ?: "text"
        val source = input["source"] as? String ?: "unknown"
        val metadata = input["metadata"] as? Map<String, String> ?: emptyMap()

        return ContentHandlerContext(content, contentType, source, metadata)
    }

    override fun formatOutput(output: ToolResult.AgentResult): String {
        return output.content
    }

    /**
     * 检查内容是否需要特殊处理
     */
    fun needsHandling(content: String): Boolean = content.length > contentThreshold

    override suspend fun execute(
        input: ContentHandlerContext,
        onProgress: (String) -> Unit
    ): ToolResult.AgentResult {
        onProgress("🔍 Analysis Agent started")
        onProgress("Analyzing ${input.contentType} content from ${input.source} (${input.content.length} chars)")

        try {
            // 分析内容
            val result = analyzeContent(input, onProgress)
            
            // 保存到历史记录
            contentHistory.add(input to result)
            updateConversationContext(input, result)
            
            logger().info { "Analysis completed: $result" }
            
            return ToolResult.AgentResult(
                success = true,
                content = formatResult(result),
                metadata = mapOf(
                    "originalLength" to input.content.length.toString(),
                    "contentType" to input.contentType,
                    "source" to input.source,
                    "keyPointsCount" to result.keyPoints.size.toString(),
                    "insightsCount" to result.insights.size.toString(),
                    "handlerId" to "content-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}"
                )
            )
            
        } catch (e: Exception) {
            onProgress("❌ Content analysis failed: ${e.message}")
            return ToolResult.AgentResult(
                success = false,
                content = "Content analysis failed: ${e.message}",
                metadata = mapOf("error" to e.message.orEmpty())
            )
        }
    }

    /**
     * 分析内容
     */
    private suspend fun analyzeContent(
        input: ContentHandlerContext,
        onProgress: (String) -> Unit
    ): ContentHandlerResult {
        onProgress("Analyzing content structure...")

        // 构建分析提示
        val prompt = buildAnalysisPrompt(input)

        // 调用 LLM 进行分析
        val llmResponse = llmService.sendPrompt(prompt)

        // 解析响应
        return parseAnalysisResponse(llmResponse, input)
    }

    /**
     * 构建分析提示
     */
    private fun buildAnalysisPrompt(input: ContentHandlerContext): String {
        val contentPreview = if (input.content.length > 2000) {
            input.content.take(1000) + "\n\n... [CONTENT TRUNCATED] ...\n\n" + input.content.takeLast(1000)
        } else {
            input.content
        }

        return """
            Analyze the following ${input.contentType} content from ${input.source}:

            Content (${input.content.length} characters):
            ```
            $contentPreview
            ```

            Please provide a JSON response with the following structure:
            {
                "summary": "Brief summary of the content",
                "keyPoints": ["key point 1", "key point 2", ...],
                "structure": {
                    "type": "detected structure type",
                    "sections": ["section1", "section2", ...],
                    "patterns": ["pattern1", "pattern2", ...]
                },
                "insights": ["insight 1", "insight 2", ...]
            }

            Focus on:
            1. What is the main purpose/content of this data?
            2. What are the most important elements?
            3. What patterns or structures can you identify?
            4. What insights would be valuable for a developer?
        """.trimIndent()
    }

    /**
     * 解析分析响应
     */
    private fun parseAnalysisResponse(
        response: String,
        input: ContentHandlerContext
    ): ContentHandlerResult {
        return try {
            val jsonMatch = Regex("\\{[\\s\\S]*?\\}").find(response)
            if (jsonMatch != null) {
                val parsed = json.decodeFromString<ContentAnalysisJson>(jsonMatch.value)
                ContentHandlerResult(
                    summary = parsed.summary ?: "Content analyzed",
                    keyPoints = parsed.keyPoints ?: emptyList(),
                    structure = parsed.structure ?: emptyMap(),
                    insights = parsed.insights ?: emptyList(),
                    originalLength = input.content.length
                )
            } else {
                // 降级处理
                createFallbackResult(input, response)
            }
        } catch (e: Exception) {
            createFallbackResult(input, response)
        }
    }

    /**
     * 创建降级结果
     */
    private fun createFallbackResult(
        input: ContentHandlerContext,
        response: String
    ): ContentHandlerResult {
        val lines = input.content.lines()
        val summary = when (input.contentType) {
            "log" -> "Log file with ${lines.size} lines"
            "json" -> "JSON data structure"
            "code" -> "Code file with ${lines.size} lines"
            else -> "Content with ${input.content.length} characters"
        }

        return ContentHandlerResult(
            summary = summary,
            keyPoints = listOf("Content length: ${input.content.length} chars", "Lines: ${lines.size}"),
            structure = mapOf("type" to input.contentType, "lines" to lines.size.toString()),
            insights = listOf("Large content requiring analysis"),
            originalLength = input.content.length
        )
    }

    /**
     * 格式化结果
     */
    private fun formatResult(result: ContentHandlerResult): String {
        return buildString {
            appendLine("📊 Content Analysis Summary")
            appendLine("=========================================")
            appendLine()
            appendLine("📝 Summary: ${result.summary}")
            appendLine()

            if (result.keyPoints.isNotEmpty()) {
                appendLine("🔑 Key Points:")
                result.keyPoints.forEach { point ->
                    appendLine("  • $point")
                }
                appendLine()
            }

            if (result.insights.isNotEmpty()) {
                appendLine("💡 Insights:")
                result.insights.forEach { insight ->
                    appendLine("  • $insight")
                }
                appendLine()
            }

            appendLine("📏 Original content: ${result.originalLength} characters")
            appendLine("⏰ Processed at: ${kotlinx.datetime.Instant.fromEpochMilliseconds(result.processedAt)}")
        }
    }

    /**
     * 更新对话上下文
     */
    private fun updateConversationContext(
        input: ContentHandlerContext,
        result: ContentHandlerResult
    ) {
        conversationContext["lastProcessed"] = input
        conversationContext["lastResult"] = result
        conversationContext["totalProcessed"] = contentHistory.size
        conversationContext["contentTypes"] = contentHistory.map { it.first.contentType }.distinct()
    }

    /**
     * 处理来自其他 Agent 的问题
     * 重写父类方法以提供内容相关的问答能力
     */
    override suspend fun handleQuestion(
        question: String,
        context: Map<String, Any>
    ): ToolResult.AgentResult {
        if (contentHistory.isEmpty()) {
            return ToolResult.AgentResult(
                success = false,
                content = "No content has been processed yet. Please process some content first.",
                metadata = mapOf("subagent" to name, "historySize" to "0")
            )
        }

        try {
            // 构建问答提示
            val prompt = buildQuestionPrompt(question, context)

            // 调用 LLM 获取答案
            val response = llmService.sendPrompt(prompt)

            return ToolResult.AgentResult(
                success = true,
                content = response,
                metadata = mapOf(
                    "subagent" to name,
                    "question" to question,
                    "historySize" to contentHistory.size.toString(),
                    "contextKeys" to context.keys.joinToString(",")
                )
            )

        } catch (e: Exception) {
            return ToolResult.AgentResult(
                success = false,
                content = "Failed to answer question: ${e.message}",
                metadata = mapOf("error" to e.message.orEmpty())
            )
        }
    }

    /**
     * 构建问答提示
     */
    private fun buildQuestionPrompt(question: String, context: Map<String, Any>): String {
        val recentContent = contentHistory.takeLast(3) // 最近3个处理的内容

        return buildString {
            appendLine("You are a Content Handler Agent with access to processed content history.")
            appendLine("Answer the following question based on the content you have analyzed:")
            appendLine()
            appendLine("Question: $question")
            appendLine()
            appendLine("Available processed content:")

            recentContent.forEachIndexed { index, (input, result) ->
                appendLine("--- Content ${index + 1} ---")
                appendLine("Source: ${input.source}")
                appendLine("Type: ${input.contentType}")
                appendLine("Length: ${input.content.length} chars")
                appendLine("Summary: ${result.summary}")
                appendLine("Key Points: ${result.keyPoints.joinToString(", ")}")
                if (result.insights.isNotEmpty()) {
                    appendLine("Insights: ${result.insights.joinToString(", ")}")
                }
                appendLine()
            }

            if (context.isNotEmpty()) {
                appendLine("Additional context:")
                context.forEach { (key, value) ->
                    appendLine("$key: $value")
                }
                appendLine()
            }

            appendLine("Please provide a helpful and specific answer based on the processed content.")
            appendLine("If the question cannot be answered from the available content, say so clearly.")
        }
    }

    /**
     * 获取当前状态摘要
     */
    override fun getStateSummary(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "description" to description,
            "priority" to priority,
            "contentThreshold" to contentThreshold,
            "processedCount" to contentHistory.size,
            "contentTypes" to contentHistory.map { it.first.contentType }.distinct(),
            "lastProcessedAt" to (contentHistory.lastOrNull()?.second?.processedAt ?: 0),
            "conversationContext" to conversationContext.toMap()
        )
    }

    /**
     * 检查是否应该触发此 SubAgent
     * 当内容长度超过阈值时自动触发
     */
    override fun shouldTrigger(context: Map<String, Any>): Boolean {
        val content = context["content"] as? String ?: return false
        return needsHandling(content)
    }

    /**
     * 获取处理历史
     */
    fun getProcessingHistory(): List<Pair<ContentHandlerContext, ContentHandlerResult>> {
        return contentHistory.toList()
    }

    /**
     * 清理旧的处理历史（保留最近的N个）
     */
    fun cleanupHistory(keepLast: Int = 10) {
        if (contentHistory.size > keepLast) {
            val toRemove = contentHistory.size - keepLast
            repeat(toRemove) {
                contentHistory.removeAt(0)
            }
        }
    }
}

/**
 * 内容分析 JSON 响应结构
 */
@Serializable
private data class ContentAnalysisJson(
    val summary: String? = null,
    val keyPoints: List<String>? = null,
    val structure: Map<String, String>? = null,
    val insights: List<String>? = null
)
