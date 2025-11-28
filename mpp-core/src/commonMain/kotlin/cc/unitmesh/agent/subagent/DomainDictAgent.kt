package cc.unitmesh.agent.subagent

import cc.unitmesh.agent.core.SubAgent
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.model.PromptConfig
import cc.unitmesh.agent.model.RunConfig
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.impl.CodebaseInsightsParams
import cc.unitmesh.agent.tool.impl.CodebaseInsightsResult
import cc.unitmesh.agent.tool.impl.CodebaseInsightsTool
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.integer
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.parser.CodeFence
import cc.unitmesh.indexer.DomainDictService
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
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
 * Callbacks for DomainDictAgent progress reporting
 */
data class DomainDictCallbacks(
    /** Progress status messages (e.g., "Step 1/7: Clarify") */
    val onProgress: (String) -> Unit = {},
    /** AI streaming output for the current step */
    val onAIThinking: (String) -> Unit = {},
    /** Called when a step completes with its result summary */
    val onStepComplete: (step: Int, stepName: String, summary: String) -> Unit = { _, _, _ -> },
    /** Called with codebase analysis stats when available */
    val onCodebaseStats: (hotFiles: Int, coChangePatterns: Int, concepts: Int) -> Unit = { _, _, _ -> }
)

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
 * 
 * Features:
 * - AI streaming output for real-time feedback
 * - Detailed progress callbacks
 * - Cancellation support via coroutine Job
 * - Codebase analysis stats reporting
 */
class DomainDictAgent(
    private val llmService: KoogLLMService,
    private val fileSystem: ProjectFileSystem,
    private val domainDictService: DomainDictService,
    private val maxDefaultIterations: Int = 7,
    private val enableStreaming: Boolean = true
) : SubAgent<DomainDictContext, ToolResult.AgentResult>(
    definition = createDefinition()
) {
    private var researchState = DeepResearchState()
    private val fileContentCache = mutableMapOf<String, String>()
    private val reviewHistory = mutableListOf<DomainDictReviewResult>()
    private val allNewEntries = mutableListOf<DomainEntry>()
    
    // Current job for cancellation support
    private var currentJob: Job? = null
    
    // Async codebase analysis (starts at agent init, ready for review step)
    private var asyncInsightsJob: Deferred<CodebaseInsightsResult?>? = null
    private val projectPath = fileSystem.getProjectPath() ?: "."
    private val codebaseInsightsTool = CodebaseInsightsTool(
        DefaultToolFileSystem(projectPath),
        projectPath
    )
    
    // Callbacks holder for streaming
    private var callbacks: DomainDictCallbacks = DomainDictCallbacks()

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
        return executeWithCallbacks(input, DomainDictCallbacks(onProgress = onProgress))
    }
    
    /**
     * Execute with full callback support for streaming AI output
     */
    suspend fun executeWithCallbacks(
        input: DomainDictContext,
        callbacks: DomainDictCallbacks
    ): ToolResult.AgentResult {
        this.callbacks = callbacks
        val onProgress = callbacks.onProgress
        
        onProgress("🔬 Domain Dictionary Deep Research Agent Started")
        onProgress("=" .repeat(60))
        onProgress("📋 Requirement: ${input.userQuery}")
        if (input.focusArea != null) {
            onProgress("🎯 Focus Area: ${input.focusArea}")
        }
        onProgress("=" .repeat(60))

        try {
            // Reset state
            researchState = DeepResearchState()
            allNewEntries.clear()
            reviewHistory.clear()
            
            // Start async codebase analysis immediately (will be used in Step 7)
            onProgress("🔍 Starting codebase analysis (Git history + Import dependencies + Code structure)...")
            onProgress("   📊 This runs in background while AI research proceeds")
            startAsyncCodebaseAnalysis(input.focusArea, callbacks)

            // Load current dictionary
            val currentDict = input.currentDict ?: domainDictService.loadContent() ?: ""
            val entryCount = currentDict.lines().count { it.contains(",") }
            onProgress("📚 Loaded current dictionary ($entryCount entries)")

            // Execute 7-step Deep Research process
            
            // Step 1: Clarify - Problem Definition
            onProgress("\n## Step 1/7: Clarify - Problem Definition")
            val problemDef = step1Clarify(input, callbacks)
            researchState = researchState.copy(
                step = 1,
                stepName = "Clarify",
                problemDefinition = problemDef
            )
            callbacks.onStepComplete(1, "Clarify", "Goal: ${problemDef.goal.take(100)}")

            // Step 2: Decompose - Research Dimensions
            onProgress("\n## Step 2/7: Decompose - Research Dimensions")
            val dimensions = step2Decompose(input, problemDef, callbacks)
            researchState = researchState.copy(
                step = 2,
                stepName = "Decompose",
                dimensions = dimensions
            )
            callbacks.onStepComplete(2, "Decompose", "${dimensions.size} dimensions identified")

            // Step 3: Information Map
            onProgress("\n## Step 3/7: Information Map - Planning")
            val infoPlan = step3InformationMap(dimensions, callbacks)
            researchState = researchState.copy(
                step = 3,
                stepName = "Information Map",
                informationPlan = infoPlan
            )
            callbacks.onStepComplete(3, "Information Map", "${infoPlan.filePatterns.size} patterns, ${infoPlan.searchPaths.size} paths")

            // Step 4: Iterative Deep Research Loop
            onProgress("\n## Step 4/7: Iterative Deep Research Loop")
            val dimensionResults = step4IterativeResearch(
                dimensions,
                infoPlan,
                currentDict,
                input.maxIterations,
                callbacks
            )
            researchState = researchState.copy(
                step = 4,
                stepName = "Iterative Research",
                dimensionResults = dimensionResults
            )
            val totalNewEntries = dimensionResults.sumOf { it.newEntries.size }
            callbacks.onStepComplete(4, "Research", "$totalNewEntries entries from ${dimensionResults.size} dimensions")

            // Step 5: Second-Order Insights
            onProgress("\n## Step 5/7: Second-Order Insights")
            val insights = step5SecondOrderInsights(dimensionResults, callbacks)
            researchState = researchState.copy(
                step = 5,
                stepName = "Second-Order Insights",
                insights = insights
            )
            callbacks.onStepComplete(5, "Insights", "${insights.patterns.size} patterns, ${insights.principles.size} principles")

            // Step 6: Synthesis
            onProgress("\n## Step 6/7: Synthesis - Research Narrative")
            val narrative = step6Synthesis(problemDef, dimensionResults, insights, callbacks)
            researchState = researchState.copy(
                step = 6,
                stepName = "Synthesis",
                narrative = narrative
            )
            callbacks.onStepComplete(6, "Synthesis", "${narrative.keyFindings.size} findings, ${narrative.recommendations.size} recommendations")

            // Step 7: Actionization
            onProgress("\n## Step 7/7: Actionization - Final Deliverables")
            val deliverables = step7Actionization(currentDict, allNewEntries, narrative, callbacks)
            researchState = researchState.copy(
                step = 7,
                stepName = "Actionization",
                deliverables = deliverables,
                isComplete = true
            )
            callbacks.onStepComplete(7, "Actionization", "${allNewEntries.size} new entries added")

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

        } catch (e: CancellationException) {
            onProgress("⏹️ Research cancelled by user")
            return ToolResult.AgentResult(
                success = false,
                content = "Research cancelled at step ${researchState.step} (${researchState.stepName})",
                metadata = mapOf(
                    "cancelledStep" to researchState.step.toString(),
                    "reason" to "user_cancelled"
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
    
    /**
     * Cancel the current research
     */
    fun cancel() {
        currentJob?.cancel()
        asyncInsightsJob?.cancel()
    }

    // ============= Step 1: Clarify =============
    private suspend fun step1Clarify(
        input: DomainDictContext,
        callbacks: DomainDictCallbacks
    ): ProblemDefinition {
        callbacks.onProgress("🎯 Analyzing requirement and defining problem scope...")
        callbacks.onProgress("   💭 AI is thinking...")

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

        val response = streamLLMPrompt(prompt, callbacks)
        val parsed = parseProblemDefinition(response)

        callbacks.onProgress("   ✓ Goal: ${parsed.goal.take(80)}${if (parsed.goal.length > 80) "..." else ""}")
        callbacks.onProgress("   ✓ Scope: ${parsed.scope.take(80)}${if (parsed.scope.length > 80) "..." else ""}")
        callbacks.onProgress("   ✓ Depth: ${parsed.depth}")

        return parsed
    }
    
    /**
     * Stream LLM prompt and collect response, emitting chunks to AI thinking callback
     */
    private suspend fun streamLLMPrompt(prompt: String, callbacks: DomainDictCallbacks): String {
        val response = StringBuilder()
        
        if (enableStreaming) {
            try {
                llmService.streamPrompt(prompt, compileDevIns = false).collect { chunk ->
                    response.append(chunk)
                    callbacks.onAIThinking(chunk)
                }
            } catch (e: Exception) {
                // Fallback to non-streaming if streaming fails
                val result = llmService.sendPrompt(prompt)
                response.append(result)
                callbacks.onAIThinking(result)
            }
        } else {
            val result = llmService.sendPrompt(prompt)
            response.append(result)
            callbacks.onAIThinking(result)
        }
        
        return response.toString()
    }

    // ============= Step 2: Decompose =============
    private suspend fun step2Decompose(
        input: DomainDictContext,
        problemDef: ProblemDefinition,
        callbacks: DomainDictCallbacks
    ): List<ResearchDimension> {
        callbacks.onProgress("🔍 Decomposing problem into research dimensions...")
        callbacks.onProgress("   💭 AI is thinking...")

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

        val response = streamLLMPrompt(prompt, callbacks)
        val dimensions = parseResearchDimensions(response)

        callbacks.onProgress("   📋 Research dimensions identified:")
        dimensions.forEachIndexed { idx, dim ->
            callbacks.onProgress("      ${idx + 1}. ${dim.name} (Priority: ${dim.priority})")
        }

        return dimensions
    }

    // ============= Step 3: Information Map =============
    private suspend fun step3InformationMap(
        dimensions: List<ResearchDimension>,
        callbacks: DomainDictCallbacks
    ): InformationPlan {
        callbacks.onProgress("🗺️ Creating information gathering plan...")
        callbacks.onProgress("   💭 AI is thinking...")

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

        val response = streamLLMPrompt(prompt, callbacks)
        val plan = parseInformationPlan(response)

        callbacks.onProgress("   ✓ Search paths: ${plan.searchPaths.take(3).joinToString(", ")}")
        callbacks.onProgress("   ✓ File patterns: ${plan.filePatterns.take(3).joinToString(", ")}")
        callbacks.onProgress("   ✓ Analysis strategies: ${plan.analysisStrategies.size} defined")

        return plan
    }

    // ============= Step 4: Iterative Deep Research Loop =============
    private suspend fun step4IterativeResearch(
        dimensions: List<ResearchDimension>,
        infoPlan: InformationPlan,
        currentDict: String,
        maxIterations: Int,
        callbacks: DomainDictCallbacks
    ): List<DimensionResearchResult> {
        val results = mutableListOf<DimensionResearchResult>()
        var iterationDict = currentDict

        for ((idx, dimension) in dimensions.withIndex()) {
            callbacks.onProgress("\n### Researching Dimension ${idx + 1}/${dimensions.size}: ${dimension.name}")

            // Collect information
            callbacks.onProgress("   📥 Collecting information from codebase...")
            val collected = collectInformationForDimension(dimension, infoPlan, callbacks)
            callbacks.onProgress("      Found ${collected.size} code elements")

            // Organize information
            callbacks.onProgress("   📊 Organizing findings...")
            val organized = organizeInformation(collected, dimension)
            val classCount = organized["classes"]?.size ?: 0
            val funcCount = organized["functions"]?.size ?: 0
            callbacks.onProgress("      Classes: $classCount, Functions: $funcCount")

            // Validate findings
            callbacks.onProgress("   ✓ Validating against existing dictionary...")
            val conflicts = validateFindings(organized, iterationDict)
            if (conflicts.isNotEmpty()) {
                callbacks.onProgress("      ⚠️ Found ${conflicts.size} potential duplicates")
            }

            // Generate domain entries for this dimension
            callbacks.onProgress("   📝 Generating domain entries...")
            callbacks.onProgress("   💭 AI is thinking...")
            val dimensionEntries = generateDimensionEntries(
                dimension,
                organized,
                iterationDict,
                callbacks
            )

            allNewEntries.addAll(dimensionEntries)

            // Apply new entries
            if (dimensionEntries.isNotEmpty()) {
                iterationDict = applyNewEntries(iterationDict, dimensionEntries)
                callbacks.onProgress("   ➕ Added ${dimensionEntries.size} new entries:")
                dimensionEntries.take(3).forEach { entry ->
                    callbacks.onProgress("      • ${entry.chinese} -> ${entry.codeTranslation}")
                }
                if (dimensionEntries.size > 3) {
                    callbacks.onProgress("      ... and ${dimensionEntries.size - 3} more")
                }
            } else {
                callbacks.onProgress("   ℹ️ No new entries for this dimension")
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

            callbacks.onProgress("   ✅ Dimension complete: ${dimensionEntries.size} entries")
        }

        return results
    }

    private suspend fun collectInformationForDimension(
        dimension: ResearchDimension,
        infoPlan: InformationPlan,
        callbacks: DomainDictCallbacks
    ): List<String> {
        val collected = mutableListOf<String>()
        var filesSearched = 0

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
                            filesSearched++
                        }
                    } catch (e: Exception) {
                        // Skip files that can't be read
                        continue
                    }
                }
            } catch (e: Exception) {
                // Continue with other patterns
                callbacks.onProgress("      ⚠️ Pattern '$pattern' failed: ${e.message?.take(50)}")
            }
        }
        
        if (filesSearched > 0) {
            callbacks.onProgress("      Searched $filesSearched files")
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
        callbacks: DomainDictCallbacks
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

        val response = streamLLMPrompt(prompt, callbacks)
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
        callbacks: DomainDictCallbacks
    ): SecondOrderInsights {
        callbacks.onProgress("💡 Extracting second-order insights from ${dimensionResults.size} dimensions...")
        callbacks.onProgress("   💭 AI is thinking...")

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

        val response = streamLLMPrompt(prompt, callbacks)
        val insights = parseSecondOrderInsights(response)

        callbacks.onProgress("   ✓ Principles: ${insights.principles.size}")
        insights.principles.take(2).forEach { 
            callbacks.onProgress("      • ${it.take(60)}${if (it.length > 60) "..." else ""}")
        }
        callbacks.onProgress("   ✓ Patterns: ${insights.patterns.size}")
        insights.patterns.take(2).forEach {
            callbacks.onProgress("      • ${it.take(60)}${if (it.length > 60) "..." else ""}")
        }

        return insights
    }

    // ============= Step 6: Synthesis =============
    private suspend fun step6Synthesis(
        problemDef: ProblemDefinition,
        dimensionResults: List<DimensionResearchResult>,
        insights: SecondOrderInsights,
        callbacks: DomainDictCallbacks
    ): ResearchNarrative {
        callbacks.onProgress("📖 Synthesizing research narrative...")
        callbacks.onProgress("   💭 AI is thinking...")

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

        val response = streamLLMPrompt(prompt, callbacks)
        val narrative = parseResearchNarrative(response)

        callbacks.onProgress("   ✓ Summary: ${narrative.summary.take(80)}${if (narrative.summary.length > 80) "..." else ""}")
        callbacks.onProgress("   ✓ Key Findings:")
        narrative.keyFindings.take(3).forEach {
            callbacks.onProgress("      • ${it.take(60)}${if (it.length > 60) "..." else ""}")
        }
        callbacks.onProgress("   ✓ Recommendations: ${narrative.recommendations.size}")

        return narrative
    }

    // ============= Step 7: Actionization =============
    private suspend fun step7Actionization(
        currentDict: String,
        newEntries: List<DomainEntry>,
        narrative: ResearchNarrative,
        callbacks: DomainDictCallbacks
    ): FinalDeliverables {
        callbacks.onProgress("🚀 Creating final deliverables...")

        // Wait for async codebase analysis to complete and enrich entries
        val enrichedEntries = newEntries.toMutableList()
        try {
            callbacks.onProgress("   ⏳ Waiting for codebase analysis to complete...")
            val insights = asyncInsightsJob?.await()
            if (insights != null && insights.success) {
                callbacks.onProgress("   ✓ Codebase analysis completed:")
                callbacks.onProgress("      📊 Hot files: ${insights.hotFiles.size}")
                callbacks.onProgress("      🔗 Co-change patterns: ${insights.coChangePatterns.size}")
                callbacks.onProgress("      💡 Domain concepts: ${insights.domainConcepts.size}")
                
                // Report top concepts
                if (insights.domainConcepts.isNotEmpty()) {
                    val topConcepts = insights.domainConcepts.take(5).joinToString(", ") { it.name }
                    callbacks.onProgress("      🏷️ Top concepts: $topConcepts")
                }
                
                // Notify via callback
                callbacks.onCodebaseStats(
                    insights.hotFiles.size,
                    insights.coChangePatterns.size,
                    insights.domainConcepts.size
                )
                
                // Enrich entries with codebase insights
                callbacks.onProgress("   📝 Enriching entries with codebase insights...")
                callbacks.onProgress("   💭 AI is thinking...")
                val insightsEntries = enrichEntriesWithCodebaseInsights(insights, currentDict, callbacks)
                enrichedEntries.addAll(insightsEntries)
                if (insightsEntries.isNotEmpty()) {
                    callbacks.onProgress("   ➕ Added ${insightsEntries.size} entries from codebase analysis")
                }
            } else {
                callbacks.onProgress("   ℹ️ Codebase analysis not available")
            }
        } catch (e: CancellationException) {
            callbacks.onProgress("   ⏹️ Codebase analysis was cancelled")
        } catch (e: Exception) {
            callbacks.onProgress("   ⚠️ Codebase analysis failed: ${e.message}")
        }

        // Build updated dictionary
        val updatedDict = applyNewEntries(currentDict, enrichedEntries)
        val dictLines = updatedDict.lines().filter { it.contains(",") }

        // Calculate quality metrics
        val qualityMetrics = mapOf(
            "completeness" to (dictLines.size.toFloat() / (dictLines.size + 10)),  // Simplified metric
            "newEntriesRatio" to (enrichedEntries.size.toFloat() / maxOf(1, dictLines.size)),
            "dimensionsCovered" to (researchState.dimensions.size.toFloat() / 7f)
        )

        // Build change log
        val changeLog = enrichedEntries.map { entry ->
            "Added: ${entry.chinese} -> ${entry.codeTranslation}"
        }

        // Determine next steps
        val nextSteps = listOf(
            "Review the updated domain.csv for accuracy",
            "Test prompt enhancement with the new vocabulary",
            "Consider adding more specific terms for key modules"
        ) + narrative.recommendations.take(2)

        callbacks.onProgress("   ✓ Updated dictionary: ${dictLines.size} entries")
        callbacks.onProgress("   ✓ New entries added: ${enrichedEntries.size}")
        callbacks.onProgress("   ✓ Quality score: ${(qualityMetrics["completeness"]?.times(100))?.toInt()}%")

        return FinalDeliverables(
            updatedDictionary = updatedDict,
            changeLog = changeLog,
            qualityMetrics = qualityMetrics,
            nextSteps = nextSteps
        )
    }
    
    /**
     * Enrich domain entries with codebase insights
     * Uses hot files, clusters, and domain concepts from async analysis
     */
    private suspend fun enrichEntriesWithCodebaseInsights(
        insights: CodebaseInsightsResult,
        currentDict: String,
        callbacks: DomainDictCallbacks
    ): List<DomainEntry> {
        val existingTerms = currentDict.lines()
            .mapNotNull { line -> line.split(",").firstOrNull()?.trim()?.lowercase() }
            .toSet()
        
        val existingNewTerms = allNewEntries.map { it.chinese.lowercase() }.toSet()
        
        val enrichedEntries = mutableListOf<DomainEntry>()
        
        // 1. Add entries from high-frequency domain concepts
        val topConcepts = insights.domainConcepts
            .filter { it.occurrences >= 3 }
            .filter { it.name.lowercase() !in existingTerms && it.name.lowercase() !in existingNewTerms }
            .take(15)
        
        if (topConcepts.isNotEmpty()) {
            callbacks.onProgress("      📊 Processing ${topConcepts.size} high-frequency concepts...")
            
            val conceptsPrompt = buildString {
                appendLine("Based on these high-frequency domain concepts from codebase analysis:")
                topConcepts.forEach { concept ->
                    appendLine("- ${concept.name} (${concept.type}, ${concept.occurrences} occurrences)")
                    if (concept.usageContext.isNotEmpty()) {
                        appendLine("  Context: ${concept.usageContext.take(100)}")
                    }
                }
                appendLine()
                appendLine("Generate domain dictionary entries in CSV format (Chinese|CodeTranslation|Description):")
                appendLine("Focus on technical accuracy and practical usage in code.")
            }
            
            try {
                val response = streamLLMPrompt(conceptsPrompt, callbacks)
                val entries = parseEntriesFromCsvResponse(response)
                enrichedEntries.addAll(entries)
            } catch (e: Exception) {
                // Fallback: create basic entries
                for (concept in topConcepts.take(5)) {
                    enrichedEntries.add(DomainEntry(
                        chinese = concept.name,
                        codeTranslation = concept.name,
                        description = "Domain concept (${concept.type}, ${concept.occurrences} usages)"
                    ))
                }
            }
        }
        
        // 2. Add entries from co-change patterns (files that frequently change together)
        val topCoChangeFiles = insights.coChangePatterns.entries
            .sortedByDescending { it.value.size }
            .take(10)
        
        var coChangeCount = 0
        for ((filePath, coChangedFiles) in topCoChangeFiles) {
            val className = filePath.substringAfterLast("/").substringBeforeLast(".")
            if (className.length > 2 && 
                className.lowercase() !in existingTerms && 
                className.lowercase() !in existingNewTerms &&
                !enrichedEntries.any { it.chinese.equals(className, ignoreCase = true) }) {
                enrichedEntries.add(DomainEntry(
                    chinese = className,
                    codeTranslation = className,
                    description = "Frequently changed with ${coChangedFiles.size} other files"
                ))
                coChangeCount++
            }
        }
        if (coChangeCount > 0) {
            callbacks.onProgress("      🔗 Added $coChangeCount entries from co-change patterns")
        }
        
        // 3. Add entries from hot files' function signatures
        val hotFilesWithSignatures = insights.hotFiles
            .filter { it.functions.isNotEmpty() }
            .take(10)
        
        var hotFileCount = 0
        for (hotFile in hotFilesWithSignatures) {
            hotFile.className?.let { className ->
                if (className.lowercase() !in existingTerms && 
                    className.lowercase() !in existingNewTerms &&
                    !enrichedEntries.any { it.chinese.equals(className, ignoreCase = true) }) {
                    enrichedEntries.add(DomainEntry(
                        chinese = className,
                        codeTranslation = className,
                        description = "Hot file class with ${hotFile.functions.size} functions"
                    ))
                    hotFileCount++
                }
            }
        }
        if (hotFileCount > 0) {
            callbacks.onProgress("      🔥 Added $hotFileCount entries from hot files")
        }
        
        return enrichedEntries.distinctBy { it.chinese.lowercase() }
    }
    
    /**
     * Start async codebase analysis using CodebaseInsightsTool
     * This runs in parallel with the main research process and is used in Step 7
     */
    private fun startAsyncCodebaseAnalysis(focusArea: String?, callbacks: DomainDictCallbacks) {
        val scope = CoroutineScope(Dispatchers.Default)
        asyncInsightsJob = scope.async {
            try {
                val params = CodebaseInsightsParams(
                    analysisType = "full",
                    maxFiles = 50,
                    focusArea = focusArea
                )
                
                val result = codebaseInsightsTool.startAsyncAnalysis(params, scope).await()
                
                // Report stats when analysis completes (may happen during any step)
                if (result != null && result.success) {
                    callbacks.onCodebaseStats(
                        result.hotFiles.size,
                        result.coChangePatterns.size,
                        result.domainConcepts.size
                    )
                }
                
                result
            } catch (e: CancellationException) {
                null
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Get codebase insights result (for external use)
     */
    suspend fun getCodebaseInsights(): CodebaseInsightsResult? {
        return asyncInsightsJob?.await()
    }
    
    /**
     * Check if codebase analysis is complete
     */
    fun isCodebaseAnalysisComplete(): Boolean {
        return asyncInsightsJob?.isCompleted == true
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
    
    /**
     * Parse domain entries from CSV-like response (Chinese|CodeTranslation|Description)
     */
    private fun parseEntriesFromCsvResponse(response: String): List<DomainEntry> {
        val entries = mutableListOf<DomainEntry>()
        
        // Try to parse CSV format: Chinese|CodeTranslation|Description
        val lines = response.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) {
                continue
            }
            
            // Try pipe separator first
            val parts = if (trimmed.contains("|")) {
                trimmed.split("|").map { it.trim() }
            } else if (trimmed.contains(",")) {
                // Fallback to comma
                trimmed.split(",").map { it.trim() }
            } else {
                continue
            }
            
            if (parts.size >= 2) {
                entries.add(DomainEntry(
                    chinese = parts[0],
                    codeTranslation = parts[1],
                    description = if (parts.size >= 3) parts[2] else ""
                ))
            }
        }
        
        return entries.take(20)
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
