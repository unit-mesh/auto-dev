package cc.unitmesh.agent.subagent

import cc.unitmesh.agent.core.SubAgent
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

@Serializable
data class ContentHandlerContext(
    val content: String,
    val contentType: String = "text",
    val source: String = "unknown",
    val metadata: Map<String, String> = emptyMap()
) {
    override fun toString(): String =
        "AnalysisContext(contentType=$contentType, source=$source, length=${content.length})"
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

    private fun buildAnalysisPrompt(input: ContentHandlerContext): String {
        val contentPreview = if (input.content.length > 8000) {
            input.content.take(8000) + "\n\n... [TRUNCATED ${input.content.length - 8000} chars] ..."
        } else {
            input.content
        }

        return """
            Summarize this ${input.contentType} content from ${input.source} (${input.content.length} chars).
            
            Content:
            ```
            $contentPreview
            ```
            
            Provide a concise summary in 3-5 sentences focusing on:
            - Main topics/themes
            - Key information that would help answer questions about this content
            - Important fileName/document path, entities, concepts, or patterns mentioned

            Format as plain text, not JSON.
        """.trimIndent()
    }

    private fun parseAnalysisResponse(
        response: String,
        input: ContentHandlerContext
    ): ContentHandlerResult {
        val cleanedResponse = response.trim()

        return if (cleanedResponse.isNotEmpty() && cleanedResponse.length > 20) {
            ContentHandlerResult(
                summary = cleanedResponse,
                keyPoints = emptyList(),
                structure = emptyMap(),
                insights = emptyList(),
                originalLength = input.content.length
            )
        } else {
            createSmartFallbackResult(input)
        }
    }

    private fun createSmartFallbackResult(
        input: ContentHandlerContext
    ): ContentHandlerResult {
        val lines = input.content.lines().filter { it.isNotBlank() }

        val meaningfulLines = lines.take(10)
        val preview = meaningfulLines.joinToString(" ").take(300)

        val summary = when (input.contentType) {
            "document-content" -> "Document excerpt: $preview..."
            "json" -> "JSON data: $preview..."
            "code" -> "Code snippet (${lines.size} lines): $preview..."
            else -> "Content preview: $preview..."
        }

        return ContentHandlerResult(
            summary = summary,
            keyPoints = emptyList(),
            structure = mapOf("type" to input.contentType, "lines" to lines.size.toString()),
            insights = emptyList(),
            originalLength = input.content.length
        )
    }

    private fun formatResult(result: ContentHandlerResult): String {
        return buildString {
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
            appendLine()
            appendLine("💬 **Need more details?** Use `/ask-agent agentName=\"analysis-agent\" question=\"<your question>\"` to ask follow-up questions about this content.")
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
            val prompt = buildQuestionPrompt(question, context)
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

    private fun buildQuestionPrompt(question: String, context: Map<String, Any>): String {
        val recentContent = contentHistory.takeLast(3)

        return buildString {
            appendLine("You are a Content Analysis Agent with access to previously analyzed content.")
            appendLine("Your job is to answer questions based on the ORIGINAL content you analyzed, not just the summaries.")
            appendLine()
            appendLine("## Question")
            appendLine(question)
            appendLine()
            appendLine("## Analyzed Content (${recentContent.size} items)")
            appendLine()

            recentContent.forEachIndexed { index, (input, result) ->
                appendLine("### Content ${index + 1}: ${input.source}")
                appendLine("- **Type**: ${input.contentType}")
                appendLine("- **Summary**: ${result.summary}")
                appendLine()

                val contentLimit = when (recentContent.size) {
                    1 -> 8000
                    2 -> 4000
                    else -> 2500
                }

                val originalContent = if (input.content.length > contentLimit) {
                    val start = input.content.take(contentLimit - 500)
                    val end = input.content.takeLast(500)
                    "$start\n\n... [${input.content.length - contentLimit} chars truncated] ...\n\n$end"
                } else {
                    input.content
                }

                appendLine("**Original Content:**")
                appendLine("```")
                appendLine(originalContent)
                appendLine("```")
                appendLine()
            }

            if (context.isNotEmpty()) {
                appendLine("## Additional Context")
                context.forEach { (key, value) ->
                    appendLine("- **$key**: $value")
                }
                appendLine()
            }

            appendLine("## Instructions")
            appendLine("1. Answer the question based on the ORIGINAL content above")
            appendLine("2. Be specific and include relevant details from the content")
            appendLine("3. If the content doesn't contain the answer, say so clearly")
            appendLine("4. Quote or reference specific parts of the content when relevant")
        }
    }

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

    override fun shouldTrigger(context: Map<String, Any>): Boolean {
        val content = context["content"] as? String ?: return false
        return needsHandling(content)
    }

    fun cleanupHistory(keepLast: Int = 10) {
        if (contentHistory.size > keepLast) {
            val toRemove = contentHistory.size - keepLast
            repeat(toRemove) {
                contentHistory.removeAt(0)
            }
        }
    }
}
