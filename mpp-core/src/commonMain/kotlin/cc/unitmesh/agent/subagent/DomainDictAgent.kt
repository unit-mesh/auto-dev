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
import cc.unitmesh.agent.tool.impl.HotFileInfo
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.parser.CodeFence
import cc.unitmesh.indexer.DomainDictService
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.Serializable

// ============= Schema =============

object DomainDictAgentSchema : DeclarativeToolSchema(
    description = "Generate domain dictionary from codebase analysis",
    properties = mapOf(
        "focusArea" to string(
            description = "Focus on specific module (e.g., 'agent', 'tool', 'ui')"
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return "/$toolName focusArea=\"agent\""
    }
}

// ============= Data Models =============

@Serializable
data class DomainDictContext(
    val userQuery: String = "",
    val focusArea: String? = null,
    val currentDict: String? = null,
    val maxIterations: Int = 1
)

@Serializable
data class DomainEntry(
    val chinese: String,
    val codeTranslation: String,
    val description: String
) {
    fun toCsvRow(): String = "$chinese,$codeTranslation,$description"
}

/**
 * Callbacks for progress reporting
 */
data class DomainDictCallbacks(
    val onProgress: (String) -> Unit = {},
    val onAIThinking: (String) -> Unit = {},
    val onStepComplete: (step: Int, stepName: String, summary: String) -> Unit = { _, _, _ -> },
    val onCodebaseStats: (hotFiles: Int, coChangePatterns: Int, concepts: Int) -> Unit = { _, _, _ -> },
    val onEntryAdded: ((DomainEntry) -> Unit)? = null
)

/**
 * DomainDictAgent - Simple, DDD-focused domain dictionary generator
 * 
 * Design principles:
 * 1. Extract REAL data from codebase (class names, patterns)
 * 2. Filter and clean (remove generic terms, tests)
 * 3. Use AI ONLY for translation/description (with strict input)
 * 
 * 3-Step Process:
 * 1. Analyze: Scan codebase for meaningful class/concept names
 * 2. Generate: Use AI to translate names to Chinese with descriptions
 * 3. Save: Merge with existing dictionary
 */
class DomainDictAgent(
    private val llmService: KoogLLMService,
    private val fileSystem: ProjectFileSystem,
    private val domainDictService: DomainDictService,
    maxDefaultIterations: Int = 1,
    private val enableStreaming: Boolean = true
) : SubAgent<DomainDictContext, ToolResult.AgentResult>(
    definition = AgentDefinition(
        name = ToolType.DomainDictAgent.name,
        displayName = "Domain Dictionary Generator",
        description = "Generate domain dictionary from codebase",
        promptConfig = PromptConfig(systemPrompt = "", queryTemplate = null, initialMessages = emptyList()),
        modelConfig = ModelConfig.default(),
        runConfig = RunConfig(maxTurns = 3, maxTimeMinutes = 5, terminateOnError = false)
    )
) {
    private val projectPath = fileSystem.getProjectPath() ?: "."
    private val codebaseInsightsTool = CodebaseInsightsTool(
        DefaultToolFileSystem(projectPath),
        projectPath
    )

    private var currentJob: Job? = null
    private var callbacks: DomainDictCallbacks = DomainDictCallbacks()
    
    override fun getParameterClass(): String = "DomainDictContext"

    override fun validateInput(input: Map<String, Any>): DomainDictContext {
        return DomainDictContext(
            focusArea = input["focusArea"] as? String,
            userQuery = input["userQuery"] as? String ?: ""
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
    
    suspend fun executeWithCallbacks(
        input: DomainDictContext,
        callbacks: DomainDictCallbacks
    ): ToolResult.AgentResult {
        this.callbacks = callbacks
        val onProgress = callbacks.onProgress
        
        onProgress("🔬 Domain Dictionary Generator")
        onProgress("=" .repeat(50))
        
        try {
            // Load current dictionary
            val currentDict = input.currentDict ?: domainDictService.loadContent() ?: ""
            val existingTerms = parseExistingTerms(currentDict)
            onProgress("📚 Current: ${existingTerms.size} entries")
            
            // ============= Step 1: Analyze Codebase =============
            onProgress("\n## Step 1/3: Analyzing Codebase")
            
            val insights = analyzeCodebase(input.focusArea, onProgress)
            if (insights == null) {
                return ToolResult.AgentResult(
                    success = false,
                    content = "Codebase analysis failed"
                )
            }
            
            callbacks.onCodebaseStats(
                insights.hotFiles.size,
                insights.coChangePatterns.size,
                insights.domainConcepts.size
            )
            
            // Extract meaningful names from hot files
            val codebaseNames = extractMeaningfulNames(insights, onProgress)
            onProgress("   📋 Found ${codebaseNames.size} candidate names")
            
            // Filter out existing terms
            val newNames = codebaseNames.filter { name ->
                existingTerms.none { it.equals(name, ignoreCase = true) }
            }
            onProgress("   ✅ ${newNames.size} new names to process")
            
            callbacks.onStepComplete(1, "Analyze", "${newNames.size} new names")
            
            if (newNames.isEmpty()) {
                onProgress("\n✅ Dictionary is up to date!")
            return ToolResult.AgentResult(
                success = true,
                    content = "No new entries needed",
                    metadata = mapOf("newEntries" to "0")
                )
            }
            
            // ============= Step 2: Generate Entries =============
            onProgress("\n## Step 2/3: Generating Entries")
            
            // Limit to 20 names per batch for faster response
            val namesToProcess = newNames.take(20)
            onProgress("   💭 Translating ${namesToProcess.size} terms...")
            
            val newEntries = generateEntries(namesToProcess, callbacks)
            onProgress("   ✅ Generated ${newEntries.size} entries")
            
            // Show generated entries
            newEntries.take(10).forEach { entry ->
                onProgress("      • ${entry.chinese} → ${entry.codeTranslation}")
                callbacks.onEntryAdded?.invoke(entry)
            }
            if (newEntries.size > 10) {
                onProgress("      ... and ${newEntries.size - 10} more")
            }
            
            callbacks.onStepComplete(2, "Generate", "${newEntries.size} entries")
            
            // ============= Step 3: Save =============
            onProgress("\n## Step 3/3: Saving")
            
            val updatedDict = mergeEntries(currentDict, newEntries)
            val saved = domainDictService.saveContent(updatedDict)
            
            val finalCount = updatedDict.lines().count { it.contains(",") }
            
            if (saved) {
                onProgress("   💾 Saved to prompts/domain.csv")
            }
            
            onProgress("\n" + "=" .repeat(50))
            onProgress("✅ Done! Added ${newEntries.size} entries (total: $finalCount)")
            
            callbacks.onStepComplete(3, "Save", "$finalCount total")
            
            return ToolResult.AgentResult(
                success = true,
                content = buildReport(existingTerms.size, finalCount, newEntries),
                metadata = mapOf(
                    "newEntries" to newEntries.size.toString(),
                    "totalEntries" to finalCount.toString()
                )
            )
            
        } catch (e: CancellationException) {
            onProgress("⏹️ Cancelled")
            return ToolResult.AgentResult(success = false, content = "Cancelled")
        } catch (e: Exception) {
            onProgress("❌ Error: ${e.message}")
            return ToolResult.AgentResult(success = false, content = "Error: ${e.message}")
        }
    }
    
    fun cancel() {
        currentJob?.cancel()
    }
    
    // ============= Step 1: Analyze =============
    
    private suspend fun analyzeCodebase(
        focusArea: String?,
        onProgress: (String) -> Unit
    ): CodebaseInsightsResult? {
        onProgress("   🔍 Scanning Git history and code structure...")
        
        val params = CodebaseInsightsParams(
            analysisType = "full",
            maxFiles = 100,
            focusArea = focusArea
        )
        
        val result = codebaseInsightsTool.analyze(params)
        
        if (!result.success) {
            onProgress("   ⚠️ Analysis failed: ${result.errorMessage}")
            return null
        }
        
        onProgress("   📊 Found ${result.hotFiles.size} hot files")
        
        // Show top hot files
        result.hotFiles.take(10).forEachIndexed { idx, file ->
            val name = file.path.substringAfterLast("/")
            onProgress("      ${idx + 1}. $name (${file.changeCount} changes)")
        }
        
        return result
    }
    
    private fun extractMeaningfulNames(
        insights: CodebaseInsightsResult,
        onProgress: (String) -> Unit
    ): List<String> {
        val names = mutableSetOf<String>()
        
        // 1. Extract from hot file names (most important)
        for (file in insights.hotFiles) {
            val fileName = file.path.substringAfterLast("/").substringBeforeLast(".")
            if (isValidDomainName(fileName)) {
                names.add(fileName)
            }
            
            // Extract class name if available
            file.className?.let { className ->
                if (isValidDomainName(className)) {
                    names.add(className)
                }
            }
        }
        
        // 2. Extract from domain concepts (filtered)
        for (concept in insights.domainConcepts) {
            if (isValidDomainName(concept.name) && concept.occurrences >= 2) {
                names.add(concept.name)
            }
        }
        
        return names.toList().sortedBy { it }
    }
    
    /**
     * Check if a name is a valid domain concept (not a generic term)
     */
    private fun isValidDomainName(name: String): Boolean {
        if (name.length < 4) return false  // Skip very short names
        if (name.length > 50) return false
        
        // Skip generic/common terms
        val skipTerms = setOf(
            // Testing
            "test", "tests", "spec", "mock", "stub", "fake",
            // Implementation details
            "impl", "util", "utils", "helper", "helpers", "factory",
            "base", "abstract", "interface", "default", "common",
            // Build/config
            "main", "app", "application", "index",
            "run", "build", "gradle", "config", "settings",
            // Generic programming concepts (too common)
            "activity", "action", "event", "listener", "handler", "callback",
            "model", "data", "item", "entry", "node", "element",
            "list", "map", "set", "array", "collection", "queue",
            "context", "state", "status", "type", "kind", "mode",
            "info", "detail", "result", "response", "request",
            "color", "border", "icon", "image", "font", "style",
            "file", "path", "name", "key", "value", "id",
            "size", "width", "height", "offset", "padding", "margin",
            "consumer", "producer", "provider", "service", "manager",
            "builder", "creator", "generator", "loader", "reader", "writer",
            "parser", "formatter", "converter", "adapter", "wrapper",
            "view", "panel", "dialog", "screen", "page", "component",
            "button", "text", "label", "field", "input", "output",
            "editor", "renderer", "painter", "drawer",
            "exception", "error", "warning", "message",
            "checks", "diff", "check"
        )
        
        val lowerName = name.lowercase()
        
        // Exact match skip
        if (lowerName in skipTerms) return false
        
        // Skip IntelliJ platform concepts
        val platformTerms = setOf(
            "anaction", "applicationmanager", "project", "psifile", "psielement",
            "virtualfile", "document", "editor", "intention", "inspection",
            "psiclass", "psimethod", "psifield", "psitype", "psivariable",
            "language", "filetype", "module", "facet", "artifact",
            "toolwindow", "notification", "progress", "indicator",
            "runnable", "callable", "future", "promise", "deferred"
        )
        if (platformTerms.any { lowerName.contains(it) }) return false
        
        // Contains skip (for compound names like "TestHelper")
        val containsSkip = setOf("test", "spec", "mock", "fake", "stub", "factory", "util")
        if (containsSkip.any { lowerName.contains(it) }) return false
        
        // Skip if all uppercase (likely constants)
        if (name.all { it.isUpperCase() || it == '_' }) return false
        
        // Skip if contains underscore (likely generated/config)
        if (name.contains("_")) return false
        
        // Must start with uppercase (class name convention)
        if (!name.first().isUpperCase()) return false
        
        // Must have at least 2 capital letters (compound name like DocQLTool)
        val capitalCount = name.count { it.isUpperCase() }
        if (capitalCount < 2) return false
        
        return true
    }
    
    // ============= Step 2: Generate =============
    
    private suspend fun generateEntries(
        names: List<String>,
        callbacks: DomainDictCallbacks
    ): List<DomainEntry> {
        if (names.isEmpty()) return emptyList()
        
        val namesList = names.joinToString("\n") { "- $it" }

        val prompt = """
你是一个技术文档翻译专家。请将以下代码中的类名/概念名翻译成简洁的中文术语。
            
## 要翻译的名称:
$namesList
            
## 输出格式 (JSON):
            ```json
            {
                "entries": [
    {"chinese": "中文术语", "codeTranslation": "ClassName", "description": "一句话描述功能"}
                ]
            }
            ```
            
## 规则:
1. chinese: 简洁的中文术语(2-6个字)
2. codeTranslation: 保持原始类名
3. description: 一句话描述(不超过30字)
4. 只翻译有意义的领域概念
5. 跳过无法理解或太通用的名称

请直接输出JSON，不要其他解释。
        """.trimIndent()

        val response = streamLLMPrompt(prompt, callbacks)
        return parseEntries(response)
    }
    
    private suspend fun streamLLMPrompt(prompt: String, callbacks: DomainDictCallbacks): String {
        val response = StringBuilder()
        
        if (enableStreaming) {
            try {
                llmService.streamPrompt(prompt, compileDevIns = false).collect { chunk ->
                    response.append(chunk)
                    callbacks.onAIThinking(chunk)
            }
        } catch (e: Exception) {
                val result = llmService.sendPrompt(prompt)
                response.append(result)
            }
        } else {
            val result = llmService.sendPrompt(prompt)
            response.append(result)
        }
        
        return response.toString()
    }
    
    private fun parseEntries(response: String): List<DomainEntry> {
        val entries = mutableListOf<DomainEntry>()
        
        val json = CodeFence.parse(response).text.ifBlank { response }
        
        // Parse entries using regex (simple approach)
        val pattern = """"chinese"\s*:\s*"([^"]+)"[^}]*"codeTranslation"\s*:\s*"([^"]+)"[^}]*"description"\s*:\s*"([^"]+)"""".toRegex()
        
        pattern.findAll(json).forEach { match ->
            val chinese = match.groupValues[1].trim()
            val code = match.groupValues[2].trim()
            val desc = match.groupValues[3].trim()
            
            if (chinese.isNotBlank() && code.isNotBlank()) {
                entries.add(DomainEntry(chinese, code, desc))
            }
        }
        
        return entries
    }
    
    // ============= Step 3: Save =============
    
    private fun parseExistingTerms(csv: String): Set<String> {
        return csv.lines()
            .filter { it.contains(",") }
            .mapNotNull { line ->
                val parts = line.split(",")
                if (parts.size >= 2) parts[1].trim() else null
            }
            .flatMap { it.split("|").map { s -> s.trim() } }
            .filter { it.isNotBlank() }
            .toSet()
    }
    
    private fun mergeEntries(currentDict: String, newEntries: List<DomainEntry>): String {
        val existingLines = currentDict.lines().toMutableList()
        
        // Ensure header exists
        if (existingLines.isEmpty() || !existingLines[0].contains("Chinese")) {
            existingLines.add(0, "Chinese,Code Translation,Description")
        }
        
        // Remove empty first line if exists
        if (existingLines.isNotEmpty() && existingLines[0].isBlank()) {
            existingLines.removeAt(0)
        }
        
        // Get existing code translations to avoid duplicates
        val existingCodes = existingLines
            .filter { it.contains(",") }
            .mapNotNull { line -> line.split(",").getOrNull(1)?.trim()?.lowercase() }
            .toSet()
        
        // Add new entries
        for (entry in newEntries) {
            if (entry.codeTranslation.lowercase() !in existingCodes) {
                existingLines.add(entry.toCsvRow())
            }
        }
        
        return existingLines.joinToString("\n")
    }
    
    private fun buildReport(before: Int, after: Int, newEntries: List<DomainEntry>): String {
        return buildString {
            appendLine("# Domain Dictionary Update")
            appendLine()
            appendLine("- Before: $before entries")
            appendLine("- After: $after entries")
            appendLine("- Added: ${newEntries.size} entries")
            appendLine()
            if (newEntries.isNotEmpty()) {
                appendLine("## New Entries:")
                newEntries.forEach { entry ->
                    appendLine("- ${entry.chinese} → ${entry.codeTranslation}")
                }
            }
        }
    }

    // ============= SubAgent Interface =============

    override suspend fun handleQuestion(
        question: String,
        context: Map<String, Any>
    ): ToolResult.AgentResult {
            return ToolResult.AgentResult(
                success = false,
            content = "Use execute() to generate dictionary"
        )
    }
    
    override fun getStateSummary(): Map<String, Any> = mapOf("name" to name)

    override fun shouldTrigger(context: Map<String, Any>): Boolean {
        val query = context["query"] as? String ?: return false
        return query.contains("domain", ignoreCase = true) ||
               query.contains("dictionary", ignoreCase = true)
    }
}
