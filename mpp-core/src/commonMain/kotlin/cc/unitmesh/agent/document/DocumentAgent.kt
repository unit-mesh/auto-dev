package cc.unitmesh.agent.document

import cc.unitmesh.agent.core.MainAgent
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.agent.model.PromptConfig
import cc.unitmesh.agent.model.RunConfig
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.devins.document.DocumentParserService
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import kotlinx.coroutines.flow.collect

/**
 * Document Task - represents a user query about a document
 */
data class DocumentTask(
    val query: String,
    val documentPath: String? = null
)

/**
 * Document Agent - handles document queries using HeadingQL and ChapterQL
 * Extends MainAgent for consistency with the agent framework
 */
class DocumentAgent(
    private val llmService: KoogLLMService,
    private val parserService: DocumentParserService,
    private val renderer: CodingAgentRenderer
) : MainAgent<DocumentTask, ToolResult.AgentResult>(
    AgentDefinition(
        name = "DocumentAgent",
        displayName = "Document Query Agent",
        description = "Agent for answering questions about documents using HeadingQL and ChapterQL",
        promptConfig = PromptConfig(
            systemPrompt = buildSystemPrompt(),
            queryTemplate = null,
            initialMessages = emptyList()
        ),
        modelConfig = ModelConfig.default(),
        runConfig = RunConfig(
            maxTurns = 3,
            maxTimeMinutes = 5,
            terminateOnError = false
        )
    )
) {
    private val history = mutableListOf<Message>()

    /**
     * Validate and parse input parameters
     */
    override fun validateInput(input: Map<String, Any>): DocumentTask {
        val query = input["query"] as? String 
            ?: throw IllegalArgumentException("Missing required parameter 'query'")
        val documentPath = input["documentPath"] as? String
        
        return DocumentTask(query = query, documentPath = documentPath)
    }

    /**
     * Format agent result as string
     */
    override fun formatOutput(output: ToolResult.AgentResult): String {
        return if (output.success) {
            output.content
        } else {
            "Error: ${output.content}"
        }
    }

    override suspend fun execute(
        input: DocumentTask,
        onProgress: (String) -> Unit
    ): ToolResult.AgentResult {
        history.clear()
        history.add(Message(MessageRole.USER, input.query))

        var success = true
        var finalMessage = ""
        var iterations = 0
        val maxIterations = 3

        try {
            while (iterations < maxIterations) {
                iterations++
                currentIteration = iterations

                renderer.renderIterationHeader(iterations, maxIterations)
                
                val response = callLLM(input.query)
                finalMessage = response
                
                // Check for commands
                val command = parseCommand(response)
                if (command != null) {
                    val result = executeCommand(command)
                    
                    // Add tool result to history
                    val toolResultMessage = "Tool Output:\n$result\n\nNow please answer the user's question based on this information."
                    history.add(Message(MessageRole.USER, toolResultMessage))
                    
                    // Continue loop to let LLM generate final answer
                } else {
                    // No command, assume final answer
                    break
                }
            }

            renderer.renderTaskComplete()
            renderer.renderFinalResult(true, finalMessage, iterations)

        } catch (e: Exception) {
            success = false
            finalMessage = "Error: ${e.message}"
            renderer.renderError(finalMessage)
        }

        return ToolResult.AgentResult(
            success = success,
            content = finalMessage,
            metadata = mapOf(
                "iterations" to iterations.toString(),
                "commands_executed" to history.count { it.role == MessageRole.USER && it.content.contains("Tool Output") }.toString()
            )
        )
    }

    /**
     * Call LLM with the current conversation history
     */
    private suspend fun callLLM(userPrompt: String): String {
        renderer.renderLLMResponseStart()
        var fullResponse = ""

        val messagesToSend = listOf(Message(MessageRole.SYSTEM, buildSystemPrompt())) + history.dropLast(1)

        llmService.streamPrompt(
            userPrompt = userPrompt,
            historyMessages = messagesToSend,
            compileDevIns = false
        ).collect { chunk ->
            renderer.renderLLMResponseChunk(chunk)
            fullResponse += chunk
        }

        renderer.renderLLMResponseEnd()
        history.add(Message(MessageRole.ASSISTANT, fullResponse))

        return fullResponse
    }

    /**
     * Parse command from LLM response
     */
    private fun parseCommand(response: String): Command? {
        val headingRegex = Regex("""heading\("([^"]+)"\)""")
        val chapterRegex = Regex("""chapter\("([^"]+)"\)""")

        headingRegex.find(response)?.let {
            return Command.Heading(it.groupValues[1])
        }

        chapterRegex.find(response)?.let {
            return Command.Chapter(it.groupValues[1])
        }

        return null
    }

    /**
     * Execute document query command
     */
    private suspend fun executeCommand(command: Command): String {
        return when (command) {
            is Command.Heading -> {
                renderer.renderToolCall("heading", "keyword=\"${command.keyword}\"")
                val chunks = parserService.queryHeading(command.keyword)
                val result = if (chunks.isNotEmpty()) {
                    chunks.joinToString("\n---\n") { "Chapter: ${it.chapterTitle}\nContent:\n${it.content}" }
                } else {
                    "No sections found matching '${command.keyword}'"
                }
                renderer.renderToolResult("heading", true, result, result)
                result
            }
            is Command.Chapter -> {
                renderer.renderToolCall("chapter", "id=\"${command.id}\"")
                val chunk = parserService.queryChapter(command.id)
                val result = if (chunk != null) {
                    "Chapter: ${chunk.chapterTitle}\nContent:\n${chunk.content}"
                } else {
                    "Chapter '${command.id}' not found"
                }
                renderer.renderToolResult("chapter", true, result, result)
                result
            }
        }
    }

    /**
     * Command types for document queries
     */
    sealed class Command {
        data class Heading(val keyword: String) : Command()
        data class Chapter(val id: String) : Command()
    }

    companion object {
        /**
         * Build system prompt for the document agent
         */
        private fun buildSystemPrompt(): String {
            return """
                You are a helpful document assistant.
                You can query the document using the following commands:
                
                HeadingQL:
                heading("keyword")
                -> Returns the most matching section title and its content.
                
                ChapterQL:
                chapter("chapter_id")
                -> Returns the content of the chapter and its sub-chapters.
                
                When a user asks a question:
                1. Analyze the topic.
                2. If you need information from the document, output the command on a separate line.
                3. If you have enough information, answer the user directly.
                
                Example:
                User: "What is the architecture?"
                Assistant: I will search for architecture.
                heading("architecture")
            """.trimIndent()
        }
    }
}
