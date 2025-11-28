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
            description = "Maximum research iterations (default: 7)"
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
    val maxIterations: Int = 7,
    val focusArea: String? = null,
    val projectContext: Map<String, String> = emptyMap()
) {
    override fun toString(): String =
        "DomainDictContext(query='${userQuery.take(50)}...', focusArea=$focusArea, maxIterations=$maxIterations)"
}

// ============= Deep Research Data Models =============

/**
 * Step 1: Problem Definition
 */
@Serializable
data class ProblemDefinition(
    val goal: String,
    val scope: String,
    val depth: String,
    val deliverableFormat: String,
    val constraints: List<String> = emptyList()
)

/**
 * Step 2: Research Dimensions
 */
@Serializable
data class ResearchDimension(
    val name: String,
    val description: String,
    val priority: Int,  // 1-5, higher is more important
    val queries: List<String> = emptyList()
)

/**
 * Step 3: Information Plan
 */
@Serializable
data class InformationPlan(
    val searchPaths: List<String>,
    val filePatterns: List<String>,
    val knowledgeSources: List<String>,
    val analysisStrategies: List<String>
)

/**
 * Step 4: Dimension Research Result
 */
@Serializable
data class DimensionResearchResult(
    val dimension: String,
    val collected: List<String>,
    val organized: Map<String, List<String>>,
    val validated: Boolean,
    val conflicts: List<String>,
    val conclusion: String,
    val newEntries: List<DomainEntry> = emptyList()
)

/**
 * Step 5: Second-Order Insights
 */
@Serializable
data class SecondOrderInsights(
    val principles: List<String>,
    val patterns: List<String>,
    val frameworks: List<String>,
    val unifiedModel: String
)

/**
 * Step 6: Research Narrative
 */
@Serializable
data class ResearchNarrative(
    val summary: String,
    val keyFindings: List<String>,
    val implications: List<String>,
    val recommendations: List<String>
)

/**
 * Step 7: Final Deliverables
 */
@Serializable
data class FinalDeliverables(
    val updatedDictionary: String,
    val changeLog: List<String>,
    val qualityMetrics: Map<String, Float>,
    val nextSteps: List<String>
)

/**
 * Complete Deep Research State
 */
@Serializable
data class DeepResearchState(
    val step: Int = 0,
    val stepName: String = "",
    val problemDefinition: ProblemDefinition? = null,
    val dimensions: List<ResearchDimension> = emptyList(),
    val informationPlan: InformationPlan? = null,
    val dimensionResults: List<DimensionResearchResult> = emptyList(),
    val insights: SecondOrderInsights? = null,
    val narrative: ResearchNarrative? = null,
    val deliverables: FinalDeliverables? = null,
    val isComplete: Boolean = false
)

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
 * Legacy compatibility types
 */
@Serializable
data class DomainDictReviewResult(
    val iteration: Int,
    val assessment: DictAssessment,
    val suggestions: List<DictSuggestion> = emptyList(),
    val queriesNeeded: List<String> = emptyList(),
    val newEntries: List<DomainEntry> = emptyList()
)

@Serializable
data class DictAssessment(
    val satisfiesRequirement: Boolean,
    val completenessScore: Float,
    val relevanceScore: Float,
    val gaps: List<String> = emptyList(),
    val reasoning: String
)

@Serializable
data class DictSuggestion(
    val type: SuggestionType,
    val description: String,
    val filesToQuery: List<String> = emptyList(),
    val termsToAdd: List<String> = emptyList()
)

@Serializable
enum class SuggestionType {
    ADD_TERMS,
    REFINE_TRANSLATION,
    ADD_DESCRIPTION,
    QUERY_MORE_FILES,
    CLUSTER_ANALYSIS,
    COMPLETE
}

/**
 * DomainDictAgent - DeepResearch style agent for domain dictionary optimization
 *
 * Implements the complete 7-step Deep Research flow:
 * 1. Clarify - Problem Definition
 * 2. Decompose - Research Dimensions
 * 3. Information Map - Information Plan
 * 4. Iterative Deep Research Loop - Research each dimension
 * 5. Second-Order Insights - Extract patterns and principles
 * 6. Synthesis - Research Narrative
 * 7. Actionization - Final Deliverables
 */
class DomainDictAgent(
    private val llmService: KoogLLMService,
    private val fileSystem: ProjectFileSystem,
    private val domainDictService: DomainDictService,
    private val maxDefaultIterations: Int = 7
) : SubAgent<DomainDictContext, ToolResult.AgentResult>(
    definition = createDefinition()
) {
    private var researchState = DeepResearchState()
    private val fileContentCache = mutableMapOf<String, String>()
    private val reviewHistory = mutableListOf<DomainDictReviewResult>()
    private val allNewEntries = mutableListOf<DomainEntry>()

    companion object {
        private fun createDefinition() = AgentDefinition(
            name = ToolType.DomainDictAgent.name,
            displayName = "Domain Dictionary Deep Research Agent",
            description = "DeepResearch agent that iteratively reviews and improves domain dictionary through a 7-step research process",
            promptConfig = PromptConfig(
                systemPrompt = buildSystemPrompt(),
                queryTemplate = null,
                initialMessages = emptyList()
            ),
            modelConfig = ModelConfig.default(),
            runConfig = RunConfig(
                maxTurns = 15,
                maxTimeMinutes = 10,
                terminateOnError = false
            )
        )

        private fun buildSystemPrompt(): String = """
            You are a Domain Dictionary Deep Research Agent specialized in comprehensive domain vocabulary analysis.

            You follow a rigorous 7-step Deep Research methodology:
            
            ## Step 1 - Clarify (Problem Definition)
            - Confirm goal, scope, depth, and deliverable format
            - Output: Problem Definition with clear constraints
            
            ## Step 2 - Decompose (Research Dimensions)  
            - Split the problem into 3-7 meaningful dimensions
            - Each dimension should be independently researchable
            - Output: Prioritized list of research dimensions
            
            ## Step 3 - Information Map (Information Plan)
            - Plan search paths and knowledge sources
            - Identify file patterns and analysis strategies
            - Output: Comprehensive information gathering plan
            
            ## Step 4 - Iterative Deep Research Loop
            For each dimension:
            - Collect: Gather relevant information from codebase
            - Organize: Structure the collected information
            - Validate: Verify accuracy and completeness
            - Conflict Analysis: Identify inconsistencies
            - Conclude: Draw dimension-specific conclusions
            - Output: Structured research notes with new domain entries
            
            ## Step 5 - Second-Order Insights
            From all research notes, abstract:
            - Principles: Core truths discovered
            - Patterns: Recurring structures
            - Frameworks: Mental models
            - Unified Model: Integrated understanding
            
            ## Step 6 - Synthesis (Research Narrative)
            Integrate all content into a clear narrative:
            - Summary of findings
            - Key implications
            - Recommendations
            
            ## Step 7 - Actionization (Final Deliverables)
            Produce actionable outputs:
            - Updated domain dictionary (CSV format)
            - Change log
            - Quality metrics
            - Next steps
            
            Domain Entry Format (CSV):
            Chinese,Code Translation,Description
            
            Always output JSON responses for structured data.
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

    override fun formatOutput(output: ToolResult.AgentResult): String = output.content

    override val priority: Int = 50

    override suspend fun execute(
        input: DomainDictContext,
        onProgress: (String) -> Unit
    ): ToolResult.AgentResult {
        onProgress("🔬 Domain Dictionary Deep Research Agent Started")
        onProgress("=" .repeat(60))
        onProgress("📋 Requirement: ${input.userQuery}")
        onProgress("=" .repeat(60))

        try {
            // Reset state
            researchState = DeepResearchState()
            allNewEntries.clear()
            reviewHistory.clear()

            // Load current dictionary
            val currentDict = input.currentDict ?: domainDictService.loadContent() ?: ""
            onProgress("📚 Loaded current dictionary (${currentDict.lines().size} entries)")

            // Execute 7-step Deep Research process
            
            // Step 1: Clarify - Problem Definition
            onProgress("\n## Step 1/7: Clarify - Problem Definition")
            val problemDef = step1Clarify(input, onProgress)
            researchState = researchState.copy(
                step = 1,
                stepName = "Clarify",
                problemDefinition = problemDef
            )

            // Step 2: Decompose - Research Dimensions
            onProgress("\n## Step 2/7: Decompose - Research Dimensions")
            val dimensions = step2Decompose(input, problemDef, onProgress)
            researchState = researchState.copy(
                step = 2,
                stepName = "Decompose",
                dimensions = dimensions
            )

            // Step 3: Information Map
            onProgress("\n## Step 3/7: Information Map - Planning")
            val infoPlan = step3InformationMap(dimensions, onProgress)
            researchState = researchState.copy(
                step = 3,
                stepName = "Information Map",
                informationPlan = infoPlan
            )

            // Step 4: Iterative Deep Research Loop
            onProgress("\n## Step 4/7: Iterative Deep Research Loop")
            val dimensionResults = step4IterativeResearch(
                dimensions,
                infoPlan,
                currentDict,
                input.maxIterations,
                onProgress
            )
            researchState = researchState.copy(
                step = 4,
                stepName = "Iterative Research",
                dimensionResults = dimensionResults
            )

            // Step 5: Second-Order Insights
            onProgress("\n## Step 5/7: Second-Order Insights")
            val insights = step5SecondOrderInsights(dimensionResults, onProgress)
            researchState = researchState.copy(
                step = 5,
                stepName = "Second-Order Insights",
                insights = insights
            )

            // Step 6: Synthesis
            onProgress("\n## Step 6/7: Synthesis - Research Narrative")
            val narrative = step6Synthesis(problemDef, dimensionResults, insights, onProgress)
            researchState = researchState.copy(
                step = 6,
                stepName = "Synthesis",
                narrative = narrative
            )

            // Step 7: Actionization
            onProgress("\n## Step 7/7: Actionization - Final Deliverables")
            val deliverables = step7Actionization(currentDict, allNewEntries, narrative, onProgress)
            researchState = researchState.copy(
                step = 7,
                stepName = "Actionization",
                deliverables = deliverables,
                isComplete = true
            )

            // Save the final dictionary
            val saved = domainDictService.saveContent(deliverables.updatedDictionary)
            if (saved) {
                onProgress("💾 Saved updated dictionary to prompts/domain.csv")
            }

            // Generate final report
            val report = generateFinalReport(input, deliverables)
            onProgress("\n" + "=".repeat(60))
            onProgress("✅ Deep Research Complete!")
            onProgress("=".repeat(60))

            return ToolResult.AgentResult(
                success = true,
                content = report,
                metadata = mapOf(
                    "steps" to "7",
                    "dimensions" to dimensions.size.toString(),
                    "newEntries" to allNewEntries.size.toString(),
                    "completeness" to (deliverables.qualityMetrics["completeness"]?.toString() ?: "N/A")
                )
            )

        } catch (e: Exception) {
            onProgress("❌ Deep Research failed: ${e.message}")
            return ToolResult.AgentResult(
                success = false,
                content = "Deep Research failed at step ${researchState.step} (${researchState.stepName}): ${e.message}",
                metadata = mapOf(
                    "failedStep" to researchState.step.toString(),
                    "error" to e.message.orEmpty()
                )
            )
        }
    }

    // ============= Step 1: Clarify =============
    private suspend fun step1Clarify(
        input: DomainDictContext,
        onProgress: (String) -> Unit
    ): ProblemDefinition {
        onProgress("🎯 Analyzing requirement and defining problem scope...")

        val prompt = """
            Analyze the following domain dictionary optimization request and define the problem clearly.
            
            User Request: ${input.userQuery}
            Focus Area: ${input.focusArea ?: "General"}
            
            Provide a problem definition in JSON format:
            ```json
            {
                "goal": "What we want to achieve",
                "scope": "What's included and excluded",
                "depth": "How deep should the analysis go",
                "deliverableFormat": "Expected output format",
                "constraints": ["constraint1", "constraint2"]
            }
            ```
        """.trimIndent()

        val response = llmService.sendPrompt(prompt)
        val parsed = parseProblemDefinition(response)

        onProgress("   ✓ Goal: ${parsed.goal}")
        onProgress("   ✓ Scope: ${parsed.scope}")
        onProgress("   ✓ Depth: ${parsed.depth}")

        return parsed
    }

    // ============= Step 2: Decompose =============
    private suspend fun step2Decompose(
        input: DomainDictContext,
        problemDef: ProblemDefinition,
        onProgress: (String) -> Unit
    ): List<ResearchDimension> {
        onProgress("🔍 Decomposing problem into research dimensions...")

        val prompt = """
            Based on this problem definition, decompose it into 3-7 research dimensions.
            
            Problem: ${problemDef.goal}
            Scope: ${problemDef.scope}
            Focus Area: ${input.focusArea ?: "General domain vocabulary"}
            
            Each dimension should be:
            - Independently researchable
            - Relevant to domain vocabulary discovery
            - Actionable for code analysis
            
            Provide dimensions in JSON format:
            ```json
            {
                "dimensions": [
                    {
                        "name": "Dimension Name",
                        "description": "What this dimension covers",
                        "priority": 1-5,
                        "queries": ["search pattern 1", "search pattern 2"]
                    }
                ]
            }
            ```
        """.trimIndent()

        val response = llmService.sendPrompt(prompt)
        val dimensions = parseResearchDimensions(response)

        dimensions.forEachIndexed { idx, dim ->
            onProgress("   ${idx + 1}. ${dim.name} (Priority: ${dim.priority})")
        }

        return dimensions
    }

    // ============= Step 3: Information Map =============
    private suspend fun step3InformationMap(
        dimensions: List<ResearchDimension>,
        onProgress: (String) -> Unit
    ): InformationPlan {
        onProgress("🗺️ Creating information gathering plan...")

        val dimensionList = dimensions.joinToString("\n") { "- ${it.name}: ${it.description}" }

        val prompt = """
            Create an information gathering plan for these research dimensions:
            
            $dimensionList
            
            Consider:
            - What file patterns to search (*.kt, *.java, etc.)
            - What code structures to analyze (classes, functions, interfaces)
            - What documentation to review
            
            Provide plan in JSON format:
            ```json
            {
                "searchPaths": ["src/main", "src/commonMain"],
                "filePatterns": ["*Agent*.kt", "*Service*.kt"],
                "knowledgeSources": ["source code", "README files"],
                "analysisStrategies": ["class name analysis", "function signature analysis"]
            }
            ```
        """.trimIndent()

        val response = llmService.sendPrompt(prompt)
        val plan = parseInformationPlan(response)

        onProgress("   ✓ Search paths: ${plan.searchPaths.take(3).joinToString(", ")}")
        onProgress("   ✓ File patterns: ${plan.filePatterns.take(3).joinToString(", ")}")
        onProgress("   ✓ Analysis strategies: ${plan.analysisStrategies.size} defined")

        return plan
    }

    // ============= Step 4: Iterative Deep Research Loop =============
    private suspend fun step4IterativeResearch(
        dimensions: List<ResearchDimension>,
        infoPlan: InformationPlan,
        currentDict: String,
        maxIterations: Int,
        onProgress: (String) -> Unit
    ): List<DimensionResearchResult> {
        val results = mutableListOf<DimensionResearchResult>()
        var iterationDict = currentDict

        for ((idx, dimension) in dimensions.withIndex()) {
            onProgress("\n### Researching Dimension ${idx + 1}/${dimensions.size}: ${dimension.name}")

            // Collect information
            onProgress("   📥 Collecting information...")
            val collected = collectInformationForDimension(dimension, infoPlan, onProgress)

            // Organize information
            onProgress("   📊 Organizing findings...")
            val organized = organizeInformation(collected, dimension)

            // Validate findings
            onProgress("   ✓ Validating...")
            val conflicts = validateFindings(organized, iterationDict)

            // Generate domain entries for this dimension
            onProgress("   📝 Generating domain entries...")
            val dimensionEntries = generateDimensionEntries(
                dimension,
                organized,
                iterationDict,
                onProgress
            )

            allNewEntries.addAll(dimensionEntries)

            // Apply new entries
            if (dimensionEntries.isNotEmpty()) {
                iterationDict = applyNewEntries(iterationDict, dimensionEntries)
                onProgress("   ➕ Added ${dimensionEntries.size} new entries")
            }

            // Conclude dimension research
            val conclusion = concludeDimensionResearch(dimension, organized, dimensionEntries)

            results.add(
                DimensionResearchResult(
                    dimension = dimension.name,
                    collected = collected,
                    organized = organized,
                    validated = conflicts.isEmpty(),
                    conflicts = conflicts,
                    conclusion = conclusion,
                    newEntries = dimensionEntries
                )
            )

            onProgress("   ✅ Dimension complete: ${dimensionEntries.size} entries, ${conflicts.size} conflicts")
        }

        return results
    }

    private suspend fun collectInformationForDimension(
        dimension: ResearchDimension,
        infoPlan: InformationPlan,
        onProgress: (String) -> Unit
    ): List<String> {
        val collected = mutableListOf<String>()

        // Use dimension queries + info plan patterns
        val patterns = dimension.queries + infoPlan.filePatterns.map { pattern ->
            if (dimension.name.isNotEmpty()) {
                pattern.replace("*", "*${dimension.name.take(10)}*")
            } else pattern
        }

        for (pattern in patterns.take(5)) {
            try {
                val files = fileSystem.searchFiles(pattern, maxDepth = 5, maxResults = 10)
                    .filter { file ->
                        // Skip problematic paths
                        !file.contains("kcef-cache") &&
                        !file.contains("node_modules") &&
                        !file.contains("/build/") &&
                        !file.contains("/.gradle/") &&
                        !file.contains("/bin/")
                    }
                    
                for (file in files.take(5)) {
                    try {
                        val content = fileSystem.readFile(file)
                        if (content != null) {
                            // Extract class/function names
                            val names = extractSemanticNames(content, file)
                            collected.addAll(names)
                            fileContentCache[file] = content
                        }
                    } catch (e: Exception) {
                        // Skip files that can't be read
                        continue
                    }
                }
            } catch (e: Exception) {
                // Continue with other patterns
                onProgress("   ⚠️ Pattern '$pattern' failed: ${e.message?.take(50)}")
            }
        }

        return collected.distinct()
    }

    private fun extractSemanticNames(content: String, filePath: String): List<String> {
        val names = mutableListOf<String>()

        // Extract class names
        val classPattern = "(?:class|interface|object|enum)\\s+(\\w+)".toRegex()
        classPattern.findAll(content).forEach { match ->
            names.add("class:${match.groupValues[1]}")
        }

        // Extract function names
        val funPattern = "(?:fun|suspend fun)\\s+(\\w+)".toRegex()
        funPattern.findAll(content).forEach { match ->
            names.add("fun:${match.groupValues[1]}")
        }

        // Extract file name
        val fileName = filePath.substringAfterLast("/").substringBeforeLast(".")
        names.add("file:$fileName")

        return names
    }

    private fun organizeInformation(
        collected: List<String>,
        dimension: ResearchDimension
    ): Map<String, List<String>> {
        val organized = mutableMapOf<String, MutableList<String>>()

        for (item in collected) {
            val category = when {
                item.startsWith("class:") -> "classes"
                item.startsWith("fun:") -> "functions"
                item.startsWith("file:") -> "files"
                else -> "other"
            }
            organized.getOrPut(category) { mutableListOf() }.add(item.substringAfter(":"))
        }

        return organized
    }

    private fun validateFindings(
        organized: Map<String, List<String>>,
        currentDict: String
    ): List<String> {
        val conflicts = mutableListOf<String>()

        // Check for duplicate entries
        val existingTerms = currentDict.lines()
            .filter { it.contains(",") }
            .mapNotNull { it.split(",").firstOrNull()?.trim() }
            .toSet()

        val allNames = organized.values.flatten()
        for (name in allNames) {
            if (existingTerms.any { it.equals(name, ignoreCase = true) }) {
                conflicts.add("Potential duplicate: $name")
            }
        }

        return conflicts.take(5)  // Limit conflicts reported
    }

    private suspend fun generateDimensionEntries(
        dimension: ResearchDimension,
        organized: Map<String, List<String>>,
        currentDict: String,
        onProgress: (String) -> Unit
    ): List<DomainEntry> {
        val organizedStr = organized.entries.joinToString("\n") { (cat, items) ->
            "$cat: ${items.take(20).joinToString(", ")}"
        }

        val prompt = """
            Based on the following code analysis for dimension "${dimension.name}":
            
            $organizedStr
            
            Current dictionary has ${currentDict.lines().size} entries.
            
            Generate domain dictionary entries in JSON format:
            ```json
            {
                "entries": [
                    {
                        "chinese": "Chinese term or concept",
                        "codeTranslation": "CodeName | AltName | Package.Name",
                        "description": "Brief description of this domain concept"
                    }
                ]
            }
            ```
            
            Rules:
            - Only include meaningful domain concepts
            - Avoid common programming terms (like "List", "Map", "String")
            - Focus on business/domain-specific vocabulary
            - Maximum 10 entries per dimension
        """.trimIndent()

        val response = llmService.sendPrompt(prompt)
        return parseNewEntries(response)
    }

    private fun concludeDimensionResearch(
        dimension: ResearchDimension,
        organized: Map<String, List<String>>,
        entries: List<DomainEntry>
    ): String {
        val classCount = organized["classes"]?.size ?: 0
        val funcCount = organized["functions"]?.size ?: 0
        return "Analyzed $classCount classes, $funcCount functions. Generated ${entries.size} domain entries."
    }

    // ============= Step 5: Second-Order Insights =============
    private suspend fun step5SecondOrderInsights(
        dimensionResults: List<DimensionResearchResult>,
        onProgress: (String) -> Unit
    ): SecondOrderInsights {
        onProgress("💡 Extracting second-order insights...")

        val summaries = dimensionResults.joinToString("\n") { result ->
            "- ${result.dimension}: ${result.conclusion}"
        }

        val prompt = """
            From these research findings, extract higher-order insights:
            
            $summaries
            
            Total new entries: ${dimensionResults.sumOf { it.newEntries.size }}
            
            Provide insights in JSON format:
            ```json
            {
                "principles": ["Core truths about this codebase's domain vocabulary"],
                "patterns": ["Recurring naming or structural patterns"],
                "frameworks": ["Mental models for understanding the domain"],
                "unifiedModel": "A single integrated understanding of the domain vocabulary"
            }
            ```
        """.trimIndent()

        val response = llmService.sendPrompt(prompt)
        val insights = parseSecondOrderInsights(response)

        onProgress("   ✓ Principles: ${insights.principles.size}")
        onProgress("   ✓ Patterns: ${insights.patterns.size}")
        onProgress("   ✓ Unified Model: ${insights.unifiedModel.take(100)}...")

        return insights
    }

    // ============= Step 6: Synthesis =============
    private suspend fun step6Synthesis(
        problemDef: ProblemDefinition,
        dimensionResults: List<DimensionResearchResult>,
        insights: SecondOrderInsights,
        onProgress: (String) -> Unit
    ): ResearchNarrative {
        onProgress("📖 Synthesizing research narrative...")

        val prompt = """
            Synthesize the following research into a coherent narrative:
            
            Problem: ${problemDef.goal}
            
            Dimension Results:
            ${dimensionResults.joinToString("\n") { "- ${it.dimension}: ${it.conclusion}" }}
            
            Key Insights:
            - Principles: ${insights.principles.take(3).joinToString(", ")}
            - Patterns: ${insights.patterns.take(3).joinToString(", ")}
            
            Provide narrative in JSON format:
            ```json
            {
                "summary": "Executive summary of the research",
                "keyFindings": ["Finding 1", "Finding 2", "Finding 3"],
                "implications": ["What this means for the project"],
                "recommendations": ["Actionable recommendations"]
            }
            ```
        """.trimIndent()

        val response = llmService.sendPrompt(prompt)
        val narrative = parseResearchNarrative(response)

        onProgress("   ✓ Summary: ${narrative.summary.take(100)}...")
        onProgress("   ✓ Key Findings: ${narrative.keyFindings.size}")
        onProgress("   ✓ Recommendations: ${narrative.recommendations.size}")

        return narrative
    }

    // ============= Step 7: Actionization =============
    private suspend fun step7Actionization(
        currentDict: String,
        newEntries: List<DomainEntry>,
        narrative: ResearchNarrative,
        onProgress: (String) -> Unit
    ): FinalDeliverables {
        onProgress("🚀 Creating final deliverables...")

        // Build updated dictionary
        val updatedDict = applyNewEntries(currentDict, newEntries)
        val dictLines = updatedDict.lines().filter { it.contains(",") }

        // Calculate quality metrics
        val qualityMetrics = mapOf(
            "completeness" to (dictLines.size.toFloat() / (dictLines.size + 10)),  // Simplified metric
            "newEntriesRatio" to (newEntries.size.toFloat() / maxOf(1, dictLines.size)),
            "dimensionsCovered" to (researchState.dimensions.size.toFloat() / 7f)
        )

        // Build change log
        val changeLog = newEntries.map { entry ->
            "Added: ${entry.chinese} -> ${entry.codeTranslation}"
        }

        // Determine next steps
        val nextSteps = listOf(
            "Review the updated domain.csv for accuracy",
            "Test prompt enhancement with the new vocabulary",
            "Consider adding more specific terms for key modules"
        ) + narrative.recommendations.take(2)

        onProgress("   ✓ Updated dictionary: ${dictLines.size} entries")
        onProgress("   ✓ New entries added: ${newEntries.size}")
        onProgress("   ✓ Quality score: ${(qualityMetrics["completeness"]?.times(100))?.toInt()}%")

        return FinalDeliverables(
            updatedDictionary = updatedDict,
            changeLog = changeLog,
            qualityMetrics = qualityMetrics,
            nextSteps = nextSteps
        )
    }

    // ============= Helper Methods =============

    private fun applyNewEntries(currentDict: String, newEntries: List<DomainEntry>): String {
        val existingLines = currentDict.lines().toMutableList()

        val existingChinese = existingLines.mapNotNull { line ->
            line.split(",").firstOrNull()?.trim()
        }.toSet()

        val newLines = newEntries
            .filter { it.chinese !in existingChinese }
            .map { it.toCsvRow() }

        if (newLines.isEmpty()) return currentDict

        return (existingLines + newLines).joinToString("\n")
    }

    private fun generateFinalReport(
        input: DomainDictContext,
        deliverables: FinalDeliverables
    ): String {
        return buildString {
            appendLine("# Domain Dictionary Deep Research Report")
            appendLine()
            appendLine("## Summary")
            appendLine("- **Requirement**: ${input.userQuery}")
            appendLine("- **Research Steps**: 7/7 completed")
            appendLine("- **Dimensions Analyzed**: ${researchState.dimensions.size}")
            appendLine("- **New Entries Added**: ${allNewEntries.size}")
            appendLine()

            researchState.narrative?.let { narrative ->
                appendLine("## Key Findings")
                narrative.keyFindings.forEach { finding ->
                    appendLine("- $finding")
                }
                appendLine()
            }

            researchState.insights?.let { insights ->
                appendLine("## Insights")
                appendLine("### Patterns")
                insights.patterns.take(5).forEach { pattern ->
                    appendLine("- $pattern")
                }
                appendLine()
                appendLine("### Unified Model")
                appendLine(insights.unifiedModel)
                appendLine()
            }

            appendLine("## Quality Metrics")
            deliverables.qualityMetrics.forEach { (metric, value) ->
                appendLine("- **$metric**: ${(value * 100).toInt()}%")
            }
            appendLine()

            appendLine("## Change Log")
            deliverables.changeLog.take(10).forEach { change ->
                appendLine("- $change")
            }
            if (deliverables.changeLog.size > 10) {
                appendLine("- ... and ${deliverables.changeLog.size - 10} more")
            }
            appendLine()

            appendLine("## Next Steps")
            deliverables.nextSteps.forEach { step ->
                appendLine("1. $step")
            }
            appendLine()

            appendLine("## Updated Dictionary Preview")
            appendLine("```csv")
            appendLine(deliverables.updatedDictionary.lines().take(15).joinToString("\n"))
            if (deliverables.updatedDictionary.lines().size > 15) {
                appendLine("... (${deliverables.updatedDictionary.lines().size - 15} more entries)")
            }
            appendLine("```")
        }
    }

    // ============= Parsing Methods =============

    private fun parseProblemDefinition(response: String): ProblemDefinition {
        val json = extractJsonFromResponse(response)
        return try {
            ProblemDefinition(
                goal = extractString(json, "goal") ?: "Optimize domain dictionary",
                scope = extractString(json, "scope") ?: "Project-wide vocabulary",
                depth = extractString(json, "depth") ?: "Comprehensive",
                deliverableFormat = extractString(json, "deliverableFormat") ?: "CSV",
                constraints = extractStringArray(json, "constraints")
            )
        } catch (e: Exception) {
            ProblemDefinition(
                goal = "Optimize domain dictionary based on user requirements",
                scope = "Project-wide vocabulary enhancement",
                depth = "Comprehensive analysis",
                deliverableFormat = "CSV domain dictionary"
            )
        }
    }

    private fun parseResearchDimensions(response: String): List<ResearchDimension> {
        val json = extractJsonFromResponse(response)
        val dimensions = mutableListOf<ResearchDimension>()

        // Try to parse dimensions array
        val dimPattern = "\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*\"description\"\\s*:\\s*\"([^\"]+)\"[^}]*\"priority\"\\s*:\\s*(\\d+)".toRegex()
        dimPattern.findAll(json).forEach { match ->
            dimensions.add(
                ResearchDimension(
                    name = match.groupValues[1],
                    description = match.groupValues[2],
                    priority = match.groupValues[3].toIntOrNull() ?: 3
                )
            )
        }

        return dimensions.ifEmpty {
            listOf(
                ResearchDimension("Core Domain", "Main business logic entities", 5),
                ResearchDimension("Infrastructure", "Technical infrastructure components", 3),
                ResearchDimension("API Layer", "API and service interfaces", 4)
            )
        }
    }

    private fun parseInformationPlan(response: String): InformationPlan {
        val json = extractJsonFromResponse(response)
        return InformationPlan(
            searchPaths = extractStringArray(json, "searchPaths").ifEmpty { listOf("src/main", "src/commonMain") },
            filePatterns = extractStringArray(json, "filePatterns").ifEmpty { listOf("*.kt", "*.java") },
            knowledgeSources = extractStringArray(json, "knowledgeSources").ifEmpty { listOf("source code") },
            analysisStrategies = extractStringArray(json, "analysisStrategies").ifEmpty { listOf("class analysis") }
        )
    }

    private fun parseNewEntries(response: String): List<DomainEntry> {
        val json = extractJsonFromResponse(response)
        val entries = mutableListOf<DomainEntry>()

        val entryPattern = "\"chinese\"\\s*:\\s*\"([^\"]+)\"[^}]*\"codeTranslation\"\\s*:\\s*\"([^\"]+)\"[^}]*\"description\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        entryPattern.findAll(json).forEach { match ->
            entries.add(
                DomainEntry(
                    chinese = match.groupValues[1],
                    codeTranslation = match.groupValues[2],
                    description = match.groupValues[3]
                )
            )
        }

        return entries.take(10)
    }

    private fun parseSecondOrderInsights(response: String): SecondOrderInsights {
        val json = extractJsonFromResponse(response)
        return SecondOrderInsights(
            principles = extractStringArray(json, "principles"),
            patterns = extractStringArray(json, "patterns"),
            frameworks = extractStringArray(json, "frameworks"),
            unifiedModel = extractString(json, "unifiedModel") ?: "Domain vocabulary reflects core business concepts"
        )
    }

    private fun parseResearchNarrative(response: String): ResearchNarrative {
        val json = extractJsonFromResponse(response)
        return ResearchNarrative(
            summary = extractString(json, "summary") ?: "Research completed successfully",
            keyFindings = extractStringArray(json, "keyFindings"),
            implications = extractStringArray(json, "implications"),
            recommendations = extractStringArray(json, "recommendations")
        )
    }

    private fun extractJsonFromResponse(response: String): String {
        return CodeFence.parse(response).text.ifBlank { response }
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

    // ============= SubAgent Interface =============

    override suspend fun handleQuestion(
        question: String,
        context: Map<String, Any>
    ): ToolResult.AgentResult {
        if (!researchState.isComplete) {
            return ToolResult.AgentResult(
                success = false,
                content = "No research has been completed yet. Run a research session first.",
                metadata = mapOf("subagent" to name)
            )
        }

        val prompt = buildQuestionPrompt(question)
        val response = llmService.sendPrompt(prompt)

        return ToolResult.AgentResult(
            success = true,
            content = response,
            metadata = mapOf(
                "subagent" to name,
                "question" to question,
                "researchComplete" to "true"
            )
        )
    }

    private fun buildQuestionPrompt(question: String): String {
        return buildString {
            appendLine("You are answering questions about a completed domain dictionary research.")
            appendLine()
            appendLine("## Question")
            appendLine(question)
            appendLine()
            appendLine("## Research Summary")
            researchState.narrative?.let { appendLine("Summary: ${it.summary}") }
            researchState.insights?.let { appendLine("Key Pattern: ${it.patterns.firstOrNull()}") }
            appendLine("Dimensions Analyzed: ${researchState.dimensions.size}")
            appendLine("New Entries: ${allNewEntries.size}")
            appendLine()
            appendLine("Answer based on the research findings.")
        }
    }

    override fun getStateSummary(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "priority" to priority,
            "currentStep" to researchState.step,
            "stepName" to researchState.stepName,
            "isComplete" to researchState.isComplete,
            "dimensions" to researchState.dimensions.size,
            "newEntries" to allNewEntries.size
        )
    }

    override fun shouldTrigger(context: Map<String, Any>): Boolean {
        val query = context["query"] as? String ?: return false
        return query.contains("domain", ignoreCase = true) ||
               query.contains("dictionary", ignoreCase = true) ||
               query.contains("deep research", ignoreCase = true) ||
               query.contains("vocabulary", ignoreCase = true)
    }

    fun reset() {
        researchState = DeepResearchState()
        allNewEntries.clear()
        fileContentCache.clear()
        reviewHistory.clear()
    }
}
