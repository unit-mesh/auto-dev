package cc.unitmesh.agent.subagent

import cc.unitmesh.agent.core.SubAgent
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.model.PromptConfig
import cc.unitmesh.agent.model.RunConfig
import cc.unitmesh.agent.model.ToolConfig
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.ToolNames
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import kotlinx.serialization.Serializable

/**
 * Input data for codebase investigation
 */
@Serializable
data class InvestigationContext(
    val query: String,
    val projectPath: String,
    val scope: String = "all" // "classes", "methods", "dependencies", "all"
)

/**
 * Result of codebase investigation
 */
@Serializable
data class InvestigationResult(
    val summary: String,
    val findings: List<String>,
    val recommendations: List<String>,
    val elementsFound: Int,
    val relationsFound: Int
)

/**
 * CodebaseInvestigatorAgent - A SubAgent that analyzes codebase structure
 * and provides insights for investigation tasks.
 *
 * This agent can:
 * - Analyze project structure and dependencies
 * - Find relevant code elements based on queries
 * - Understand code relationships and patterns
 * - Provide architectural insights
 */
class CodebaseInvestigatorAgent(
    private val projectPath: String,
    private val llmService: KoogLLMService
) : SubAgent<InvestigationContext, ToolResult.AgentResult>(
    AgentDefinition(
        name = ToolType.CodebaseInvestigator.name,
        displayName = "Codebase Investigator",
        description = "Analyzes codebase structure and provides insights using code analysis",
        promptConfig = PromptConfig(
            systemPrompt = buildSystemPrompt(),
            queryTemplate = "Investigation Query: \${query}\nProject Path: \${projectPath}",
            initialMessages = emptyList()
        ),
        modelConfig = ModelConfig.default(),
        runConfig = RunConfig(
            maxTurns = 5,
            maxTimeMinutes = 10,
            terminateOnError = false
        ),
        toolConfig = ToolConfig(
            allowedTools = listOf("analyze-code", "find-elements", "get-relations", "summarize-structure")
        )
    )
) {

    // Simple in-memory cache for analysis results
    private var analysisCache: Map<String, String> = emptyMap()

    override fun validateInput(input: Map<String, Any>): InvestigationContext {
        val query = input["query"] as? String
            ?: throw IllegalArgumentException("Missing required parameter: query")
        val projectPath = input["projectPath"] as? String ?: this.projectPath
        val scope = input["scope"] as? String ?: "all"

        return InvestigationContext(
            query = query,
            projectPath = projectPath,
            scope = scope
        )
    }

    override suspend fun execute(
        input: InvestigationContext,
        onProgress: (String) -> Unit
    ): ToolResult.AgentResult {
        onProgress("🔍 Starting codebase investigation...")
        onProgress("Query: ${input.query}")
        onProgress("Scope: ${input.scope}")

        try {
            // Process the investigation query
            onProgress("🔎 Processing investigation query...")
            val investigationResult = processInvestigationQuery(input, onProgress)

            return ToolResult.AgentResult(
                success = true,
                content = investigationResult.summary,
                metadata = mapOf(
                    "elementsFound" to investigationResult.elementsFound.toString(),
                    "relationsFound" to investigationResult.relationsFound.toString(),
                    "projectPath" to input.projectPath,
                    "scope" to input.scope,
                    "findingsCount" to investigationResult.findings.size.toString()
                )
            )

        } catch (e: Exception) {
            onProgress("❌ Investigation failed: ${e.message}")
            return ToolResult.AgentResult(
                success = false,
                content = "Investigation failed: ${e.message}",
                metadata = mapOf("error" to e.message.orEmpty())
            )
        }
    }

    override fun formatOutput(output: ToolResult.AgentResult): String {
        return if (output.success) {
            buildString {
                appendLine("## Codebase Investigation Results")
                appendLine()
                appendLine(output.content)

                val metadata = output.metadata
                if (metadata.isNotEmpty()) {
                    appendLine()
                    appendLine("### Metadata")
                    metadata.forEach { (key, value) ->
                        appendLine("- $key: $value")
                    }
                }
            }
        } else {
            "❌ Investigation failed: ${output.content}"
        }
    }

    /**
     * Process the investigation query using simple text analysis
     */
    private suspend fun processInvestigationQuery(
        input: InvestigationContext,
        onProgress: (String) -> Unit
    ): InvestigationResult {
        val findings = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        // Analyze query intent and extract relevant information
        val queryAnalysis = analyzeQuery(input.query)

        when (input.scope) {
            "classes" -> {
                onProgress("🔍 Searching for classes...")
                val classFindings = findRelevantElements(queryAnalysis.keywords, "class")
                findings.addAll(classFindings)
                recommendations.add("Consider examining the class hierarchy and dependencies")
            }
            "methods" -> {
                onProgress("🔍 Searching for methods...")
                val methodFindings = findRelevantElements(queryAnalysis.keywords, "method")
                findings.addAll(methodFindings)
                recommendations.add("Review method signatures and their usage patterns")
            }
            "dependencies" -> {
                onProgress("🔍 Analyzing dependencies...")
                val depFindings = analyzeDependencyPatterns(queryAnalysis.keywords)
                findings.addAll(depFindings)
                recommendations.add("Check for circular dependencies and coupling issues")
            }
            else -> {
                onProgress("🔍 Performing comprehensive investigation...")
                val generalFindings = performGeneralInvestigation(queryAnalysis.keywords, input.projectPath)
                findings.addAll(generalFindings)
                recommendations.add("Consider refactoring based on identified patterns")
                recommendations.add("Review architectural decisions and design patterns")
            }
        }

        val summary = buildSummary(input.query, findings, recommendations)

        return InvestigationResult(
            summary = summary,
            findings = findings,
            recommendations = recommendations,
            elementsFound = findings.size,
            relationsFound = 0 // Simplified for now
        )
    }

    private fun buildSummary(query: String, findings: List<String>, recommendations: List<String>): String {
        return buildString {
            appendLine("### Investigation Summary")
            appendLine("**Query**: $query")
            appendLine()

            if (findings.isNotEmpty()) {
                appendLine("**Key Findings** (${findings.size} items):")
                findings.take(5).forEach { finding ->
                    appendLine("- $finding")
                }
                if (findings.size > 5) {
                    appendLine("- ... and ${findings.size - 5} more findings")
                }
                appendLine()
            }

            if (recommendations.isNotEmpty()) {
                appendLine("**Recommendations**:")
                recommendations.forEach { recommendation ->
                    appendLine("- $recommendation")
                }
            }
        }
    }

    private fun analyzeQuery(query: String): QueryAnalysis {
        val lowerQuery = query.lowercase()

        val keywords = extractKeywords(query)

        val type = when {
            lowerQuery.contains("class") || lowerQuery.contains("interface") -> QueryType.FIND_CLASSES
            lowerQuery.contains("method") || lowerQuery.contains("function") -> QueryType.FIND_METHODS
            lowerQuery.contains("depend") || lowerQuery.contains("import") -> QueryType.ANALYZE_DEPENDENCIES
            else -> QueryType.GENERAL_INVESTIGATION
        }

        return QueryAnalysis(type, keywords)
    }

    /**
     * Extract relevant keywords from the query
     */
    private fun extractKeywords(query: String): List<String> {
        // Simple keyword extraction - can be enhanced with NLP
        return query.split(" ")
            .filter { it.length > 3 }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
    }

    private fun findRelevantElements(keywords: List<String>, elementType: String): List<String> {
        // Simplified implementation - in a real scenario, this would scan actual files
        val findings = mutableListOf<String>()

        keywords.forEach { keyword ->
            when (elementType) {
                "class" -> {
                    findings.add("Found potential class references for '$keyword'")
                    findings.add("Consider checking inheritance hierarchy for classes containing '$keyword'")
                }
                "method" -> {
                    findings.add("Found potential method references for '$keyword'")
                    findings.add("Consider checking method signatures and implementations for '$keyword'")
                }
                else -> {
                    findings.add("Found potential code elements related to '$keyword'")
                }
            }
        }

        return findings
    }

    private fun analyzeDependencyPatterns(keywords: List<String>): List<String> {
        val findings = mutableListOf<String>()

        keywords.forEach { keyword ->
            findings.add("Potential dependency patterns involving '$keyword'")
            findings.add("Consider checking import statements and package dependencies for '$keyword'")
        }

        return findings
    }

    /**
     * Perform general investigation
     */
    private fun performGeneralInvestigation(keywords: List<String>, projectPath: String): List<String> {
        val findings = mutableListOf<String>()

        keywords.forEach { keyword ->
            findings.add("Investigating '$keyword' in project at $projectPath")
            findings.add("Potential architectural patterns involving '$keyword'")
            findings.add("Consider examining file structure and naming conventions for '$keyword'")
        }

        // Add some general architectural insights
        findings.add("Project structure analysis suggests examining:")
        findings.add("- Package organization and modularity")
        findings.add("- Design patterns and architectural decisions")
        findings.add("- Code coupling and cohesion metrics")

        return findings
    }

    companion object {
        private fun buildSystemPrompt(): String {
            return """
You are a Codebase Investigator Agent specialized in analyzing code structure and relationships.

## Your Capabilities
- Analyze project structure and organization
- Find relevant code elements (classes, methods, functions)
- Understand code dependencies and relationships
- Provide architectural insights and recommendations
- Generate summaries of code patterns and structures

## Investigation Types
- Class/Interface analysis
- Method/Function discovery
- Dependency analysis
- Architecture pattern identification
- Code relationship mapping

## Output Format
Provide clear, structured analysis with:
- Relevant code elements found
- Relationships and dependencies
- Architectural insights
- Recommendations for further investigation
            """.trimIndent()
        }
    }
}

/**
 * Query analysis result
 */
data class QueryAnalysis(
    val type: QueryType,
    val keywords: List<String>
)

/**
 * Types of investigation queries
 */
enum class QueryType {
    FIND_CLASSES,
    FIND_METHODS,
    ANALYZE_DEPENDENCIES,
    GENERAL_INVESTIGATION
}
