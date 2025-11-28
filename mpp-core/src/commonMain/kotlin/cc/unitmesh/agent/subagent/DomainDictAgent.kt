package cc.unitmesh.agent.subagent

import cc.unitmesh.agent.core.SubAgent
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.model.PromptConfig
import cc.unitmesh.agent.model.RunConfig
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.integer
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.parser.CodeFence
import cc.unitmesh.indexer.DomainDictService
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Schema for DomainDictAgent tool
 */
object DomainDictAgentSchema : DeclarativeToolSchema(
    description = "DeepResearch agent for reviewing and iteratively improving domain dictionary (domain.csv) based on user requirements",
    properties = mapOf(
        "userQuery" to string(
            description = "The user's query or requirement for domain vocabulary enhancement",
            required = true
        ),
        "currentDict" to string(
            description = "Current domain.csv content (optional, will be loaded if not provided)"
        ),
        "maxIterations" to integer(
            description = "Maximum review iterations (default: 5)"
        ),
        "focusArea" to string(
            description = "Specific area to focus on, e.g., 'authentication', 'payment', 'agent'"
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return "/$toolName userQuery=\"Add authentication related terms\" focusArea=\"auth\""
    }
}

/**
 * Input context for domain dictionary agent
 */
@Serializable
data class DomainDictContext(
    val userQuery: String,
    val currentDict: String? = null,
    val maxIterations: Int = 5,
    val focusArea: String? = null,
    val projectContext: Map<String, String> = emptyMap()
) {
    override fun toString(): String =
        "DomainDictContext(query='${userQuery.take(50)}...', focusArea=$focusArea, maxIterations=$maxIterations)"
}

/**
 * Review result from each iteration
 */
@Serializable
data class DomainDictReviewResult(
    val iteration: Int,
    val assessment: DictAssessment,
    val suggestions: List<DictSuggestion>,
    val queriesNeeded: List<String> = emptyList(),
    val newEntries: List<DomainEntry> = emptyList()
)

/**
 * Assessment of current dictionary quality
 */
@Serializable
data class DictAssessment(
    val satisfiesRequirement: Boolean,
    val completenessScore: Float,  // 0.0 - 1.0
    val relevanceScore: Float,     // 0.0 - 1.0
    val gaps: List<String> = emptyList(),
    val reasoning: String
)

/**
 * Suggestion for improving the dictionary
 */
@Serializable
data class DictSuggestion(
    val type: SuggestionType,
    val description: String,
    val filesToQuery: List<String> = emptyList(),
    val termsToAdd: List<String> = emptyList()
)

@Serializable
enum class SuggestionType {
    ADD_TERMS,           // Add new domain terms
    REFINE_TRANSLATION,  // Improve code translations
    ADD_DESCRIPTION,     // Add or improve descriptions
    QUERY_MORE_FILES,    // Need to read more source files
    CLUSTER_ANALYSIS,    // Need clustering analysis
    COMPLETE             // Dictionary is complete
}

/**
 * A single domain entry for the CSV
 */
@Serializable
data class DomainEntry(
    val chinese: String,
    val codeTranslation: String,
    val description: String
) {
    fun toCsvRow(): String = "$chinese,$codeTranslation,$description"
}

/**
 * DomainDictAgent - DeepResearch style agent for domain dictionary optimization
 *
 * This agent uses an iterative approach similar to DocumentAgent to:
 * 1. Review the current domain.csv against user requirements
 * 2. Query the codebase for relevant information using DocQL-like patterns
 * 3. Suggest and apply improvements iteratively
 * 4. Stop when the dictionary meets the requirements or max iterations reached
 *
 * Follows the SubAgent pattern for integration with CodingAgent and DocumentAgent.
 */
class DomainDictAgent(
    private val llmService: KoogLLMService,
    private val fileSystem: ProjectFileSystem,
    private val domainDictService: DomainDictService,
    private val maxDefaultIterations: Int = 5
) : SubAgent<DomainDictContext, ToolResult.AgentResult>(
    definition = createDefinition()
) {
    private val reviewHistory = mutableListOf<DomainDictReviewResult>()
    private val conversationContext = mutableMapOf<String, Any>()
    private val fileContentCache = mutableMapOf<String, String>()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private fun createDefinition() = AgentDefinition(
            name = ToolType.DomainDictAgent.name,
            displayName = "Domain Dictionary Agent",
            description = "DeepResearch agent that iteratively reviews and improves domain dictionary based on user requirements",
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
            You are a Domain Dictionary Research Agent specialized in improving project domain vocabulary.

            Your responsibilities:
            1. Review domain.csv against user requirements and project context
            2. Identify gaps in domain coverage
            3. Query codebase for relevant class names, function names, and domain concepts
            4. Generate high-quality domain entries with accurate Chinese/English translations
            5. Iteratively improve until requirements are met

            Domain Entry Format (CSV):
            Chinese,Code Translation,Description

            Review Criteria:
            - Completeness: Does the dictionary cover all relevant domain concepts?
            - Accuracy: Are the translations and descriptions accurate?
            - Relevance: Are the entries relevant to user's query?
            - Quality: Are descriptions clear and helpful for prompt enhancement?

            When reviewing:
            1. First assess if current dictionary satisfies the requirement
            2. If not, identify what's missing and what files to query
            3. Suggest specific terms to add based on codebase analysis
            4. Generate new entries following the CSV format
            5. Repeat until complete or max iterations reached
        """.trimIndent()
    }

    override fun getParameterClass(): String = DomainDictContext::class.simpleName ?: "DomainDictContext"

    override fun validateInput(input: Map<String, Any>): DomainDictContext {
        val userQuery = input["userQuery"] as? String
            ?: throw IllegalArgumentException("userQuery is required")
        val currentDict = input["currentDict"] as? String
        val maxIterations = (input["maxIterations"] as? Number)?.toInt() ?: maxDefaultIterations
        val focusArea = input["focusArea"] as? String

        return DomainDictContext(
            userQuery = userQuery,
            currentDict = currentDict,
            maxIterations = maxIterations,
            focusArea = focusArea
        )
    }

    override fun formatOutput(output: ToolResult.AgentResult): String {
        return output.content
    }

    override val priority: Int = 50  // Higher priority than AnalysisAgent

    override suspend fun execute(
        input: DomainDictContext,
        onProgress: (String) -> Unit
    ): ToolResult.AgentResult {
        onProgress("🔍 Domain Dictionary Agent started")
        onProgress("Requirement: ${input.userQuery}")

        try {
            // Load current dictionary if not provided
            val currentDict = input.currentDict ?: domainDictService.loadContent() ?: ""
            onProgress("📚 Loaded current dictionary (${currentDict.lines().size} entries)")

            var iterationDict = currentDict
            var iteration = 0

            while (iteration < input.maxIterations) {
                iteration++
                onProgress("📝 Iteration $iteration/${input.maxIterations}")

                // Step 1: Review current dictionary
                val reviewResult = reviewDictionary(
                    userQuery = input.userQuery,
                    currentDict = iterationDict,
                    focusArea = input.focusArea,
                    iteration = iteration,
                    onProgress = onProgress
                )

                reviewHistory.add(reviewResult)

                // Step 2: Check if requirement is satisfied
                if (reviewResult.assessment.satisfiesRequirement) {
                    onProgress("✅ Dictionary satisfies requirement after $iteration iterations")
                    break
                }

                // Step 3: Query codebase if needed
                if (reviewResult.queriesNeeded.isNotEmpty()) {
                    onProgress("🔎 Querying ${reviewResult.queriesNeeded.size} patterns...")
                    val queryResults = queryCodebase(reviewResult.queriesNeeded, onProgress)
                    updateFileCache(queryResults)
                }

                // Step 4: Generate and apply improvements
                if (reviewResult.newEntries.isNotEmpty()) {
                    onProgress("➕ Adding ${reviewResult.newEntries.size} new entries")
                    iterationDict = applyNewEntries(iterationDict, reviewResult.newEntries)
                }

                // Step 5: Handle other suggestions
                for (suggestion in reviewResult.suggestions) {
                    when (suggestion.type) {
                        SuggestionType.QUERY_MORE_FILES -> {
                            val results = queryCodebase(suggestion.filesToQuery.map { "file:$it" }, onProgress)
                            updateFileCache(results)
                        }
                        SuggestionType.CLUSTER_ANALYSIS -> {
                            onProgress("📊 Performing clustering analysis...")
                            performClusteringAnalysis(onProgress)
                        }
                        SuggestionType.COMPLETE -> {
                            onProgress("✅ Dictionary is complete")
                            break
                        }
                        else -> { /* Handle in next iteration */ }
                    }
                }
            }

            // Save the final dictionary
            val saved = domainDictService.saveContent(iterationDict)
            if (saved) {
                onProgress("💾 Saved updated dictionary")
            }

            // Generate final report
            val report = generateFinalReport(input, iterationDict, iteration)

            return ToolResult.AgentResult(
                success = true,
                content = report,
                metadata = mapOf(
                    "iterations" to iteration.toString(),
                    "entriesCount" to iterationDict.lines().filter { it.contains(",") }.size.toString(),
                    "satisfied" to (reviewHistory.lastOrNull()?.assessment?.satisfiesRequirement?.toString() ?: "unknown")
                )
            )

        } catch (e: Exception) {
            onProgress("❌ Domain dictionary optimization failed: ${e.message}")
            return ToolResult.AgentResult(
                success = false,
                content = "Domain dictionary optimization failed: ${e.message}",
                metadata = mapOf("error" to e.message.orEmpty())
            )
        }
    }

    /**
     * Review dictionary against user requirements
     */
    private suspend fun reviewDictionary(
        userQuery: String,
        currentDict: String,
        focusArea: String?,
        iteration: Int,
        onProgress: (String) -> Unit
    ): DomainDictReviewResult {
        onProgress("Analyzing dictionary coverage...")

        val prompt = buildReviewPrompt(userQuery, currentDict, focusArea, iteration)
        val response = llmService.sendPrompt(prompt)

        return parseReviewResponse(response, iteration)
    }

    private fun buildReviewPrompt(
        userQuery: String,
        currentDict: String,
        focusArea: String?,
        iteration: Int
    ): String {
        val previousReviews = reviewHistory.takeLast(2).joinToString("\n") {
            "Iteration ${it.iteration}: Score=${it.assessment.completenessScore}, Gaps=${it.assessment.gaps.take(3)}"
        }

        val cachedFileInfo = fileContentCache.entries.take(5).joinToString("\n") { (path, content) ->
            "- $path: ${content.take(200)}..."
        }

        return """
            You are reviewing a domain dictionary for a software project.

            ## User Requirement
            $userQuery
            ${focusArea?.let { "\n## Focus Area: $it" } ?: ""}

            ## Current Dictionary (domain.csv)
            ```csv
            $currentDict
            ```

            ## Previous Review History
            $previousReviews

            ## Cached File Information
            $cachedFileInfo

            ## Iteration
            This is iteration $iteration. Provide a detailed assessment.

            ## Your Task
            1. Assess if the current dictionary satisfies the user's requirement
            2. Score completeness (0.0-1.0) and relevance (0.0-1.0)
            3. Identify specific gaps that need to be filled
            4. Suggest what queries to make or files to read
            5. Generate new domain entries if you have enough information

            ## Response Format
            Respond in the following JSON format:
            ```json
            {
                "assessment": {
                    "satisfiesRequirement": boolean,
                    "completenessScore": 0.0-1.0,
                    "relevanceScore": 0.0-1.0,
                    "gaps": ["gap1", "gap2"],
                    "reasoning": "explanation"
                },
                "suggestions": [
                    {
                        "type": "ADD_TERMS|REFINE_TRANSLATION|QUERY_MORE_FILES|CLUSTER_ANALYSIS|COMPLETE",
                        "description": "what to do",
                        "filesToQuery": ["pattern1", "pattern2"],
                        "termsToAdd": ["term1", "term2"]
                    }
                ],
                "queriesNeeded": ["$.code.class(*Agent*)", "$.code.function(*process*)"],
                "newEntries": [
                    {
                        "chinese": "中文术语",
                        "codeTranslation": "CodeTerm | AnotherTerm",
                        "description": "功能描述"
                    }
                ]
            }
            ```
        """.trimIndent()
    }

    private fun parseReviewResponse(response: String, iteration: Int): DomainDictReviewResult {
        try {
            // Extract JSON from response
            val jsonContent = CodeFence.parse(response).text.ifBlank { response }
            
            // Try to parse as JSON
            val jsonObject = json.parseToJsonElement(jsonContent)
            
            // This is simplified parsing - in production, use proper JSON deserialization
            return DomainDictReviewResult(
                iteration = iteration,
                assessment = parseAssessment(jsonObject.toString()),
                suggestions = parseSuggestions(jsonObject.toString()),
                queriesNeeded = parseQueriesNeeded(jsonObject.toString()),
                newEntries = parseNewEntries(jsonObject.toString())
            )
        } catch (e: Exception) {
            // Fallback: create a default result indicating need for more analysis
            return DomainDictReviewResult(
                iteration = iteration,
                assessment = DictAssessment(
                    satisfiesRequirement = false,
                    completenessScore = 0.5f,
                    relevanceScore = 0.5f,
                    gaps = listOf("Could not parse LLM response"),
                    reasoning = "Parsing failed: ${e.message}"
                ),
                suggestions = listOf(
                    DictSuggestion(
                        type = SuggestionType.QUERY_MORE_FILES,
                        description = "Query more files to gather information",
                        filesToQuery = listOf("src/**/*.kt")
                    )
                )
            )
        }
    }

    private fun parseAssessment(json: String): DictAssessment {
        // Simplified parsing - extract key values from JSON string
        val satisfies = json.contains("\"satisfiesRequirement\"\\s*:\\s*true".toRegex())
        val completeness = extractFloat(json, "completenessScore") ?: 0.5f
        val relevance = extractFloat(json, "relevanceScore") ?: 0.5f
        val reasoning = extractString(json, "reasoning") ?: "No reasoning provided"
        val gaps = extractStringArray(json, "gaps")

        return DictAssessment(
            satisfiesRequirement = satisfies,
            completenessScore = completeness,
            relevanceScore = relevance,
            gaps = gaps,
            reasoning = reasoning
        )
    }

    private fun parseSuggestions(json: String): List<DictSuggestion> {
        // Simplified - return empty list if parsing fails
        return emptyList()
    }

    private fun parseQueriesNeeded(json: String): List<String> {
        return extractStringArray(json, "queriesNeeded")
    }

    private fun parseNewEntries(json: String): List<DomainEntry> {
        // Extract entries from newEntries array in JSON
        val entriesPattern = "\"chinese\"\\s*:\\s*\"([^\"]+)\"[^}]*\"codeTranslation\"\\s*:\\s*\"([^\"]+)\"[^}]*\"description\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return entriesPattern.findAll(json).map { match ->
            DomainEntry(
                chinese = match.groupValues[1],
                codeTranslation = match.groupValues[2],
                description = match.groupValues[3]
            )
        }.toList()
    }

    private fun extractFloat(json: String, key: String): Float? {
        val pattern = "\"$key\"\\s*:\\s*([0-9.]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toFloatOrNull()
    }

    private fun extractString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun extractStringArray(json: String, key: String): List<String> {
        val pattern = "\"$key\"\\s*:\\s*\\[([^\\]]+)\\]".toRegex()
        val match = pattern.find(json) ?: return emptyList()
        val arrayContent = match.groupValues[1]
        return "\"([^\"]+)\"".toRegex().findAll(arrayContent)
            .map { it.groupValues[1] }
            .toList()
    }

    /**
     * Query codebase for patterns
     */
    private suspend fun queryCodebase(
        patterns: List<String>,
        onProgress: (String) -> Unit
    ): Map<String, String> {
        val results = mutableMapOf<String, String>()

        for (pattern in patterns) {
            onProgress("Querying: $pattern")
            try {
                // Use file system to search for files matching pattern
                val searchPattern = pattern.removePrefix("file:").removePrefix("$.code.")
                val files = fileSystem.searchFiles(searchPattern, maxDepth = 5, maxResults = 10)
                
                for (file in files) {
                    val content = fileSystem.readFile(file)
                    if (content != null) {
                        results[file] = content
                    }
                }
            } catch (e: Exception) {
                onProgress("⚠️ Query failed: $pattern - ${e.message}")
            }
        }

        return results
    }

    private fun updateFileCache(results: Map<String, String>) {
        // Keep cache limited to last 20 files
        if (fileContentCache.size + results.size > 20) {
            val toRemove = fileContentCache.keys.take(results.size)
            toRemove.forEach { fileContentCache.remove(it) }
        }
        fileContentCache.putAll(results)
    }

    /**
     * Apply new entries to dictionary
     */
    private fun applyNewEntries(currentDict: String, newEntries: List<DomainEntry>): String {
        val existingLines = currentDict.lines().toMutableList()
        
        // Find existing entries to avoid duplicates
        val existingChinese = existingLines.mapNotNull { line ->
            line.split(",").firstOrNull()?.trim()
        }.toSet()

        val newLines = newEntries
            .filter { it.chinese !in existingChinese }
            .map { it.toCsvRow() }

        if (newLines.isEmpty()) return currentDict

        return (existingLines + newLines).joinToString("\n")
    }

    /**
     * Perform clustering analysis for domain term discovery
     */
    private suspend fun performClusteringAnalysis(onProgress: (String) -> Unit) {
        try {
            val clusteredDict = domainDictService.collectSemanticNamesWithClustering(
                maxTokenLength = 64000,
                maxClusters = 15
            )
            
            onProgress("Found ${clusteredDict.clusters.size} clusters with ${clusteredDict.clusterTerms.size} terms")
            
            // Cache cluster information for next iteration
            conversationContext["clusters"] = clusteredDict.clusters.map { it.name }
            conversationContext["clusterTerms"] = clusteredDict.clusterTerms.take(50).map { it.name }
        } catch (e: Exception) {
            onProgress("⚠️ Clustering failed: ${e.message}")
        }
    }

    /**
     * Generate final report
     */
    private fun generateFinalReport(
        input: DomainDictContext,
        finalDict: String,
        iterations: Int
    ): String {
        val lastReview = reviewHistory.lastOrNull()
        val entriesCount = finalDict.lines().filter { it.contains(",") }.size

        return buildString {
            appendLine("# Domain Dictionary Optimization Report")
            appendLine()
            appendLine("## Summary")
            appendLine("- **Requirement**: ${input.userQuery}")
            appendLine("- **Iterations**: $iterations/${input.maxIterations}")
            appendLine("- **Total Entries**: $entriesCount")
            appendLine()
            
            lastReview?.let { review ->
                appendLine("## Final Assessment")
                appendLine("- **Satisfies Requirement**: ${review.assessment.satisfiesRequirement}")
                appendLine("- **Completeness Score**: ${(review.assessment.completenessScore * 100).toInt()}%")
                appendLine("- **Relevance Score**: ${(review.assessment.relevanceScore * 100).toInt()}%")
                appendLine("- **Reasoning**: ${review.assessment.reasoning}")
                appendLine()
                
                if (review.assessment.gaps.isNotEmpty()) {
                    appendLine("## Remaining Gaps")
                    review.assessment.gaps.forEach { gap ->
                        appendLine("- $gap")
                    }
                    appendLine()
                }
            }

            appendLine("## Updated Dictionary Preview")
            appendLine("```csv")
            appendLine(finalDict.lines().take(20).joinToString("\n"))
            if (finalDict.lines().size > 20) {
                appendLine("... (${finalDict.lines().size - 20} more entries)")
            }
            appendLine("```")
            appendLine()
            appendLine("💡 Use `/ask-agent agentName=\"domain-dict-agent\" question=\"...\"` to ask follow-up questions.")
        }
    }

    /**
     * Handle questions from other agents
     */
    override suspend fun handleQuestion(
        question: String,
        context: Map<String, Any>
    ): ToolResult.AgentResult {
        if (reviewHistory.isEmpty()) {
            return ToolResult.AgentResult(
                success = false,
                content = "No domain dictionary reviews have been performed yet. Run a review first.",
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
                    "reviewHistorySize" to reviewHistory.size.toString()
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
        val recentReviews = reviewHistory.takeLast(3)

        return buildString {
            appendLine("You are a Domain Dictionary Agent answering questions about previous reviews.")
            appendLine()
            appendLine("## Question")
            appendLine(question)
            appendLine()
            appendLine("## Review History (${recentReviews.size} items)")
            
            recentReviews.forEach { review ->
                appendLine("### Iteration ${review.iteration}")
                appendLine("- Satisfies: ${review.assessment.satisfiesRequirement}")
                appendLine("- Completeness: ${review.assessment.completenessScore}")
                appendLine("- Gaps: ${review.assessment.gaps.take(3).joinToString(", ")}")
                appendLine("- New Entries: ${review.newEntries.size}")
                appendLine()
            }

            appendLine("## Instructions")
            appendLine("1. Answer based on the review history above")
            appendLine("2. Be specific about which iteration you're referencing")
            appendLine("3. Suggest next steps if applicable")
        }
    }

    override fun getStateSummary(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "description" to description,
            "priority" to priority,
            "reviewCount" to reviewHistory.size,
            "lastAssessment" to (reviewHistory.lastOrNull()?.assessment?.let {
                mapOf(
                    "satisfies" to it.satisfiesRequirement,
                    "completeness" to it.completenessScore,
                    "relevance" to it.relevanceScore
                )
            } ?: emptyMap<String, Any>()),
            "cachedFiles" to fileContentCache.keys.toList()
        )
    }

    override fun shouldTrigger(context: Map<String, Any>): Boolean {
        val query = context["query"] as? String ?: return false
        return query.contains("domain", ignoreCase = true) ||
               query.contains("dictionary", ignoreCase = true) ||
               query.contains("词典", ignoreCase = true) ||
               query.contains("术语", ignoreCase = true)
    }

    /**
     * Clear review history and caches
     */
    fun reset() {
        reviewHistory.clear()
        fileContentCache.clear()
        conversationContext.clear()
    }
}

