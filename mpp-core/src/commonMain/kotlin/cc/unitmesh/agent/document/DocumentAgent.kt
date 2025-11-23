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
     * Supports both legacy commands (heading, chapter) and new DocQL syntax
     */
    private fun parseCommand(response: String): Command? {
        // Try DocQL syntax first (e.g., $.content.heading("keyword"))
        val docqlRegex = Regex("""\$\.content\.(heading|chapter|h[1-6]|grep)\("([^"]+)"\)""")
        docqlRegex.find(response)?.let {
            val function = it.groupValues[1]
            val argument = it.groupValues[2]
            return Command.DocQL("\$.content.$function(\"$argument\")")
        }

        // Legacy syntax: heading("keyword")
        val headingRegex = Regex("""heading\("([^"]+)"\)""")
        headingRegex.find(response)?.let {
            return Command.Heading(it.groupValues[1])
        }

        // Legacy syntax: chapter("id")
        val chapterRegex = Regex("""chapter\("([^"]+)"\)""")
        chapterRegex.find(response)?.let {
            return Command.Chapter(it.groupValues[1])
        }

        // Full DocQL query (e.g., $.toc[?(@.level==1)])
        val fullDocqlRegex = Regex("""\$\.[a-zA-Z]+(\[[^\]]*\]|\.[\w()"\[\]@=~<>?]+)*""")
        fullDocqlRegex.find(response)?.let {
            return Command.DocQL(it.value)
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
            is Command.DocQL -> {
                renderer.renderToolCall("docql", "query=\"${command.query}\"")
                val docqlResult = executeDocQLQuery(command.query)
                renderer.renderToolResult("docql", docqlResult.first, docqlResult.second, docqlResult.second)
                docqlResult.second
            }
        }
    }
    
    /**
     * Execute DocQL query
     */
    private suspend fun executeDocQLQuery(query: String): Pair<Boolean, String> {
        return try {
            val result = cc.unitmesh.devins.document.docql.executeDocQL(
                queryString = query,
                documentFile = null, // Will be provided by parser service context
                parserService = parserService
            )
            
            when (result) {
                is cc.unitmesh.devins.document.docql.DocQLResult.TocItems -> {
                    val formatted = result.items.joinToString("\n") { toc ->
                        "${"  ".repeat(toc.level - 1)}${toc.level}. ${toc.title}"
                    }
                    Pair(true, "Found ${result.items.size} TOC items:\n$formatted")
                }
                
                is cc.unitmesh.devins.document.docql.DocQLResult.Entities -> {
                    val formatted = result.items.joinToString("\n") { entity ->
                        when (entity) {
                            is cc.unitmesh.devins.document.Entity.Term -> 
                                "Term: ${entity.name} - ${entity.definition}"
                            is cc.unitmesh.devins.document.Entity.API -> 
                                "API: ${entity.name} - ${entity.signature}"
                            is cc.unitmesh.devins.document.Entity.ClassEntity -> 
                                "Class: ${entity.name} (${entity.packageName})"
                            is cc.unitmesh.devins.document.Entity.FunctionEntity -> 
                                "Function: ${entity.name} - ${entity.signature}"
                        }
                    }
                    Pair(true, "Found ${result.items.size} entities:\n$formatted")
                }
                
                is cc.unitmesh.devins.document.docql.DocQLResult.Chunks -> {
                    val formatted = result.items.joinToString("\n---\n") { chunk ->
                        "Chapter: ${chunk.chapterTitle}\nContent:\n${chunk.content}"
                    }
                    Pair(true, formatted)
                }
                
                is cc.unitmesh.devins.document.docql.DocQLResult.Empty -> {
                    Pair(true, "No results found for query: $query")
                }
                
                is cc.unitmesh.devins.document.docql.DocQLResult.Error -> {
                    Pair(false, "Error executing DocQL query: ${result.message}")
                }
                
                is cc.unitmesh.devins.document.docql.DocQLResult.CodeBlocks -> {
                    Pair(true, "Found ${result.items.size} code blocks (not yet implemented)")
                }
                
                is cc.unitmesh.devins.document.docql.DocQLResult.Tables -> {
                    Pair(true, "Found ${result.items.size} tables (not yet implemented)")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Error executing DocQL query: ${e.message}")
        }
    }

    /**
     * Command types for document queries
     */
    sealed class Command {
        data class Heading(val keyword: String) : Command()
        data class Chapter(val id: String) : Command()
        data class DocQL(val query: String) : Command()
    }

    companion object {
        /**
         * Build system prompt for the document agent
         */
        private fun buildSystemPrompt(): String {
            return """
                You are a helpful document assistant with advanced query capabilities.
                You can query the document using DocQL (Document Query Language), a JSONPath-like syntax.
                
                DocQL Syntax Examples:
                
                1. TOC (Table of Contents) Queries:
                   $.toc[*]                      - All TOC items
                   $.toc[0]                      - First TOC item
                   $.toc[?(@.level==1)]          - Level 1 headings
                   $.toc[?(@.title~="架构")]     - TOC items with "架构" in title
                   $.toc[?(@.level>1)]           - TOC items with level > 1
                
                2. Entity Queries:
                   $.entities[*]                 - All entities
                   $.entities[?(@.type=="API")]  - API entities
                   $.entities[?(@.name~="User")] - Entities with "User" in name
                
                3. Content Queries:
                   $.content.heading("keyword")  - Sections with "keyword" in heading
                   $.content.chapter("1.2")      - Chapter 1.2 content
                   $.content.h1("Introduction")  - H1 with "Introduction"
                   $.content.h2("Design")        - H2 with "Design"
                   $.content.grep("keyword")     - Full-text search
                
                4. Operators:
                   ==  (equals)
                   ~=  (contains)
                   >   (greater than)
                   <   (less than)
                
                Legacy Commands (still supported):
                - heading("keyword") - Search by heading
                - chapter("chapter_id") - Get chapter content
                
                When a user asks a question:
                1. Analyze the topic.
                2. If you need information from the document, output the DocQL query on a separate line.
                3. If you have enough information, answer the user directly.
                
                Example:
                User: "What are the level 1 headings?"
                Assistant: I will search for level 1 headings.
                $.toc[?(@.level==1)]
                
                User: "Show me the system architecture section"
                Assistant: I will search for the architecture section.
                $.content.heading("architecture")
            """.trimIndent()
        }
    }
}
