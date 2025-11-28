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
import cc.unitmesh.codegraph.model.CodeElementType
import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.Language
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
 * DomainDictAgent - DDD-focused domain dictionary generator
 *
 * Design Principles (DDD perspective):
 * 1. Extract REAL business entities from code (not technical infrastructure)
 * 2. Focus on HOT FILES (frequently changed = core business logic)
 * 3. Use TreeSitter to parse class/function names from important files
 * 4. Filter out technical suffixes (Controller, Service, Repository, etc.)
 * 5. AI only translates business concepts, NOT implementation details
 *
 * 3-Step Process:
 * 1. Analyze: Scan Git history for hot files, use TreeSitter to extract class/function names
 * 2. Generate: Use AI with DDD principles to translate business concepts
 * 3. Save: Merge with existing dictionary
 */
class DomainDictAgent(
    private val llmService: KoogLLMService,
    private val fileSystem: ProjectFileSystem,
    private val domainDictService: DomainDictService,
    private val codeParser: CodeParser? = null,
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
        onProgress("=".repeat(50))

        try {
            // Load current dictionary
            val currentDict = input.currentDict ?: domainDictService.loadContent() ?: ""
            val existingTerms = parseExistingTerms(currentDict)
            onProgress("📚 Current: ${existingTerms.size} entries")

            // ============= Step 1: Analyze Codebase =============
            onProgress("\n## Step 1/3: Analyzing Codebase")

            val insights = analyzeCodebase(input.focusArea, onProgress) ?: return ToolResult.AgentResult(
                success = false,
                content = "Codebase analysis failed"
            )

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

            val namesToProcess = newNames.take(3000)
            onProgress("   💭 Translating ${namesToProcess.size} terms (of ${newNames.size} total)...")

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

            onProgress("\n" + "=".repeat(50))
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
            maxFiles = 3000,
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

    /**
     * Extract meaningful names from TWO sources:
     * 1. Hot files (TreeSitter parsing) - core business logic
     * 2. All domain concepts (480+ concepts from full codebase analysis)
     */
    private suspend fun extractMeaningfulNames(
        insights: CodebaseInsightsResult,
        onProgress: (String) -> Unit
    ): List<String> {
        val hotFileNames = mutableSetOf<String>()
        val allConceptNames = mutableSetOf<String>()

        // ========== Source 1: Hot Files (TreeSitter deep parsing) ==========
        if (codeParser != null) {
            onProgress("   🌲 Using TreeSitter to parse hot files...")
            val hotFilesWithCode = parseHotFilesWithTreeSitter(insights.hotFiles, onProgress)
            hotFileNames.addAll(hotFilesWithCode)
        }

        // Also extract from hot file names
        for (file in insights.hotFiles) {
            val fileName = file.path.substringAfterLast("/").substringBeforeLast(".")
            val domainName = extractDomainFromFileName(fileName)
            if (domainName != null && isValidDomainName(domainName)) {
                hotFileNames.add(domainName)
            }

            // Extract class name if available
            file.className?.let { className ->
                val extracted = extractDomainFromClassName(className)
                if (extracted != null && isValidDomainName(extracted)) {
                    hotFileNames.add(extracted)
                }
            }
        }

        onProgress("   🔥 Hot files: ${hotFileNames.size} concepts")

        // ========== Source 2: All Domain Concepts (from full codebase) ==========
        onProgress("   📚 Processing ${insights.domainConcepts.size} codebase concepts...")

        // Sort by occurrences (more frequent = more important)
        val sortedConcepts = insights.domainConcepts.sortedByDescending { it.occurrences }

        for (concept in sortedConcepts) {
            val name = concept.name
            // Less strict filter for domain concepts (they're already extracted from code)
            if (isValidDomainConceptName(name)) {
                allConceptNames.add(name)
            }
        }

        onProgress("   📋 All concepts: ${allConceptNames.size} valid names")

        // Merge: Hot files first (priority), then all concepts
        val result = mutableListOf<String>()
        result.addAll(hotFileNames.sorted())
        result.addAll(allConceptNames.filter { it !in hotFileNames }.sorted())

        onProgress("   ✅ Total: ${result.size} candidate names")

        return result
    }

    /**
     * Less strict validation for domain concepts (already extracted from code)
     */
    private fun isValidDomainConceptName(name: String): Boolean {
        if (name.length < 3) return false
        if (name.length > 60) return false

        val lowerName = name.lowercase()

        // Skip very common/generic names
        val skipExact = setOf(
            "unknown", "init", "test", "main", "app", "get", "set", "is", "has",
            "string", "int", "list", "map", "object", "class", "function",
            "true", "false", "null", "void", "return", "if", "else", "for", "while"
        )
        if (lowerName in skipExact) return false

        // Skip if starts with lowercase and is a single word method
        if (name[0].isLowerCase() && !name.contains(Regex("[A-Z]"))) return false

        // Skip special characters
        if (name.contains("<") || name.contains(">") || name.contains("$")) return false

        return true
    }

    /**
     * Parse hot files using TreeSitter to extract class and function names
     * These are the REAL important concepts in the codebase
     */
    private suspend fun parseHotFilesWithTreeSitter(
        hotFiles: List<HotFileInfo>,
        onProgress: (String) -> Unit
    ): Set<String> {
        val names = mutableSetOf<String>()
        val parser = codeParser ?: return names

        // Take top 30 hot files for deep analysis
        val topHotFiles = hotFiles.take(30)
        var parsedCount = 0

        for (file in topHotFiles) {
            val language = detectLanguage(file.path) ?: continue

            try {
                val content = fileSystem.readFile(file.path) ?: continue
                val nodes = parser.parseNodes(content, file.path, language)

                // Extract class names and function names
                for (node in nodes) {
                    when (node.type) {
                        CodeElementType.CLASS, CodeElementType.INTERFACE, CodeElementType.ENUM -> {
                            val domainName = extractDomainFromClassName(node.name)
                            if (domainName != null && isValidDomainName(domainName)) {
                                names.add(domainName)
                            }
                        }

                        CodeElementType.METHOD, CodeElementType.FUNCTION -> {
                            // Extract domain concepts from method names
                            val methodDomain = extractDomainFromMethodName(node.name)
                            if (methodDomain != null && isValidDomainName(methodDomain)) {
                                names.add(methodDomain)
                            }
                        }

                        else -> {}
                    }
                }
                parsedCount++
            } catch (e: Exception) {
                // Skip files that fail to parse
            }
        }

        if (parsedCount > 0) {
            onProgress("   📦 Parsed $parsedCount hot files, found ${names.size} domain concepts")
        }

        return names
    }

    /**
     * Detect programming language from file extension
     */
    private fun detectLanguage(filePath: String): Language? {
        val ext = filePath.substringAfterLast(".", "").lowercase()
        return when (ext) {
            "java" -> Language.JAVA
            "kt", "kts" -> Language.KOTLIN
            "py" -> Language.PYTHON
            "js", "jsx" -> Language.JAVASCRIPT
            "ts", "tsx" -> Language.TYPESCRIPT
            "go" -> Language.GO
            "rs" -> Language.RUST
            else -> null
        }
    }

    /**
     * Extract domain concept from file name (remove technical suffixes)
     * e.g., "DomainDictAgent" -> "DomainDict"
     */
    private fun extractDomainFromFileName(fileName: String): String? {
        // Remove technical suffixes
        val suffixes = listOf(
            "Controller", "Service", "Repository", "Dao", "Mapper",
            "Impl", "Helper", "Utils", "Util", "Factory", "Builder",
            "Handler", "Listener", "Adapter", "Wrapper", "Provider",
            "Agent", "Tool", "Config", "Configuration", "Settings",
            "Test", "Spec", "Mock", "Fake", "Stub"
        )

        var name = fileName
        for (suffix in suffixes) {
            if (name.endsWith(suffix) && name.length > suffix.length) {
                name = name.removeSuffix(suffix)
                break
            }
        }

        return if (name.length >= 3) name else null
    }

    /**
     * Extract domain concept from class name
     */
    private fun extractDomainFromClassName(className: String): String? {
        return extractDomainFromFileName(className)
    }

    /**
     * Extract domain concept from method name
     * e.g., "createBlogPost" -> "BlogPost"
     * e.g., "validatePayment" -> "Payment"
     */
    private fun extractDomainFromMethodName(methodName: String): String? {
        // Skip common prefixes
        val prefixes = listOf(
            "get", "set", "is", "has", "can", "should", "will",
            "create", "update", "delete", "find", "fetch", "load",
            "save", "add", "remove", "build", "parse", "validate",
            "check", "process", "handle", "execute", "run", "init",
            "on", "to", "from"
        )

        var name = methodName
        for (prefix in prefixes) {
            if (name.startsWith(prefix) && name.length > prefix.length) {
                val remainder = name.removePrefix(prefix)
                if (remainder.isNotEmpty() && remainder[0].isUpperCase()) {
                    name = remainder
                    break
                }
            }
        }

        return if (name.length >= 4 && name[0].isUpperCase()) name else null
    }

    /**
     * Check if a name is a valid domain concept (not a generic term)
     * Using DDD principles to filter out technical infrastructure
     */
    private fun isValidDomainName(name: String): Boolean {
        if (name.length < 4) return false  // Skip very short names
        if (name.length > 50) return false

        val lowerName = name.lowercase()

        // Skip generic/common terms (infrastructure, not domain)
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
            "checks", "diff", "check", "unknown"
        )

        // Exact match skip
        if (lowerName in skipTerms) return false

        // Skip IntelliJ platform concepts (infrastructure)
        val platformTerms = setOf(
            "anaction", "applicationmanager", "project", "psifile", "psielement",
            "virtualfile", "document", "editor", "intention", "inspection",
            "psiclass", "psimethod", "psifield", "psitype", "psivariable",
            "language", "filetype", "module", "facet", "artifact",
            "toolwindow", "notification", "progress", "indicator",
            "runnable", "callable", "future", "promise", "deferred",
            // JetBrains specific
            "jbcolor", "jbinsets", "jbui", "jbpopup", "jblist",
            // Java Swing/AWT
            "jcomponent", "jpanel", "jbutton", "jlabel", "jframe",
            "swing", "awt", "graphics"
        )
        if (platformTerms.any { lowerName.contains(it) }) return false

        // Skip technical suffixes that indicate infrastructure
        val technicalSuffixes = setOf(
            "controller", "service", "repository", "dao", "mapper",
            "dto", "vo", "po", "entity", "request", "response",
            "config", "configuration", "settings", "properties",
            "handler", "listener", "callback", "adapter", "wrapper",
            "factory", "builder", "provider", "manager", "registry",
            "helper", "util", "utils", "tool", "tools",
            "impl", "implementation", "abstract", "base", "default",
            "exception", "error", "filter", "interceptor",
            "capable", "aware", "enabled", "disabled"
        )
        if (technicalSuffixes.any { lowerName.endsWith(it) }) return false

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

        // DDD-focused prompt, inspired by indexer.vm
        val prompt = """
你是一个 DDD（领域驱动设计）专家，负责构建业务导向的中英文词典。请从以下代码名称中提取重要的业务概念。

**提取原则：**

✅ 应该提取的内容：
- 核心业务实体（如：Blog、Comment、Payment、User 等名词）
- 业务概念和领域模型（如：Member、Points、Order）
- 难以理解的词汇或拼音缩写
- 领域特定术语

❌ 应该排除的内容：
1. 技术词汇：Controller、Service、Repository、Mapper、DTO、VO、PO、Entity、Request、Response、Config 等
2. 实现细节和数据传输对象：包含 "Request"、"Response"、"Dto"、"Entity" 后缀的条目
3. 技术操作动词：validate、check、convert、deserialize、serialize、encode、decode 等
4. 方法名中的技术操作：如 "checkIfVipAccount" 应只提取 "VIP Account"
5. 通用库 API（如 Spring、OkHttp）和通用类名（如 List、Map）

**处理规则：**
1. 如果提取的条目包含技术后缀（如 "CreateCommentDto"），转换为纯业务概念（如 "Comment"）
2. 如果方法名包含技术操作（如 "checkIfVipAccount"），提取业务含义（"VIP Account"）
3. 如果类名包含技术词汇后缀，移除后缀再添加到词典

## 要分析的名称:
$namesList

## 输出格式 (JSON):
```json
{
  "entries": [
    {"chinese": "博客", "codeTranslation": "Blog", "description": "博客文章"}
  ]
}
```

## 输出规则:
1. chinese: 简洁的中文术语（2-6个字）
2. codeTranslation: 纯业务概念名（移除技术后缀）
3. description: 一句话业务描述（不超过20字）
4. 只输出有意义的业务概念，跳过技术实现细节
5. 如果无法理解或太通用，直接跳过不输出

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
        val pattern =
            """"chinese"\s*:\s*"([^"]+)"[^}]*"codeTranslation"\s*:\s*"([^"]+)"[^}]*"description"\s*:\s*"([^"]+)"""".toRegex()

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
