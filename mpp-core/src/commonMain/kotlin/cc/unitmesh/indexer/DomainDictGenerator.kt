package cc.unitmesh.indexer

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Generator for domain dictionaries using LLM.
 * Integrates semantic collection, template rendering, and LLM processing.
 */
class DomainDictGenerator(
    private val fileSystem: ProjectFileSystem,
    private val modelConfig: ModelConfig,
    private var maxTokenLength: Int = 128000
) {
    private val logger = getLogger("DomainDictGenerator")
    init {
        require(maxTokenLength <= modelConfig.maxTokens) {
            "maxTokenLength ($maxTokenLength) cannot exceed model's max tokens (${modelConfig.maxTokens})"
        }

        maxTokenLength = modelConfig.maxTokens * 9 / 10  // Use 90% of model max tokens
    }

    private val domainDictService = DomainDictService(fileSystem)
    private val llmService = KoogLLMService.create(modelConfig)
    
    /**
     * Generate domain dictionary and return as streaming response
     * Uses flow {} to ensure prompt building happens inside the flow collector,
     * avoiding blocking the caller before streaming starts.
     */
    fun generateStreaming(): Flow<String> = flow {
        val prompt = buildPrompt()
        emitAll(llmService.streamPrompt(prompt, compileDevIns = false))
    }
    
    /**
     * Generate domain dictionary and return complete result
     */
    suspend fun generate(): String {
        val prompt = buildPrompt()
        
        val result = StringBuilder()
        llmService.streamPrompt(prompt, compileDevIns = false).collect { chunk ->
            result.append(chunk)
        }
        
        return cleanCsvOutput(result.toString())
    }
    
    /**
     * Generate and save domain dictionary to file
     */
    suspend fun generateAndSave(): GenerationResult {
        return try {
            val content = generate()
            val saved = domainDictService.saveContent(content)
            
            if (saved) {
                GenerationResult.Success(content)
            } else {
                GenerationResult.Error("Failed to save domain dictionary to file")
            }
        } catch (e: Exception) {
            GenerationResult.Error("Generation failed: ${e.message}")
        }
    }
    
    /**
     * Build the LLM prompt for domain dictionary generation
     */
    private suspend fun buildPrompt(): String {
        // Collect semantic names from code
        val domainDict = domainDictService.collectSemanticNames(maxTokenLength)
        
        // Get weighted list for better LLM attention
        val codeContext = domainDict.toWeightedList()
        
        if (codeContext.isEmpty()) {
            throw IllegalStateException("No code context found for domain dictionary generation")
        }
        
        // Calculate token usage
        val codeTokens = domainDict.getTotalTokens()
        val reservedTokens = 1000  // Reserve tokens for prompt template and response
        val remainingTokenBudget = maxTokenLength - codeTokens - reservedTokens
        
        // Get README content if there's sufficient token budget
        val readme = if (remainingTokenBudget > 500) {
            val fullReadme = domainDictService.getReadmeContent()
            // Truncate README if it's too large
            if (fullReadme.length > remainingTokenBudget * 4) {  // Rough estimate: 4 chars per token
                fullReadme.take(remainingTokenBudget * 4)
            } else {
                fullReadme
            }
        } else {
            ""
        }
        
        // Log generation info
        logger.info { "Domain Dictionary Generation:" }
        logger.info { "- Level 1 count: ${domainDict.metadata["level1_count"]}" }
        logger.info { "- Level 2 count: ${domainDict.metadata["level2_count"]}" }
        logger.info { "- Total tokens: $codeTokens" }
        logger.info { "- README included: ${readme.isNotEmpty()}" }

        val weightStats = domainDict.getWeightStatistics()
        logger.info { "- Weight stats: Avg=${weightStats["averageWeight"]}, " +
                "Critical=${weightStats["criticalCount"]}, High=${weightStats["highCount"]}" }
        
        // Build prompt directly
        return buildIndexerPrompt(codeContext, readme)
    }
    
    /**
     * Build the indexer prompt template
     * Based on IDEA version: core/src/main/resources/genius/en/code/indexer.vm
     */
    private fun buildIndexerPrompt(code: String, readme: String): String {
        return """You are a DDD (Domain-Driven Design) expert building a business-oriented dictionary index from a codebase. Extract important business concepts and map them to their actual code identifiers.

## What is "Code Translation"?

"Code Translation" means the ACTUAL class names, method names, or variable names that exist in the codebase.

IMPORTANT RULES:
1. Use COMPLETE identifiers as they appear in code - DO NOT split compound names
   - ✅ Correct: `HttpClient` (single complete name)
   - ❌ Wrong: `Http | Client` (incorrectly split)
   - ✅ Correct: `WebFetch | WebFetcher`
   - ❌ Wrong: `Web | Fetch`
   - ✅ Correct: `SmartTask | SmartEditTask`
   - ❌ Wrong: `Smart | Task`

2. For multiplatform projects (KMP), only include the core concept, NOT platform variants:
   - ✅ Correct: `Executor` or `ShellExecutor`
   - ❌ Wrong: `Executor | Executor.jvm | Executor.js | Executor.android`
   - ✅ Correct: `FileSystem | DefaultFileSystem`
   - ❌ Wrong: `FileSystem | FileSystem.jvm | FileSystem.ios`
   - Platform suffixes like `.jvm`, `.js`, `.wasm`, `.ios`, `.android` should be EXCLUDED

3. Group only SEMANTICALLY related classes, not just similar prefixes:
   - ✅ Correct: `Blog | BlogPost | BlogComment` (all about blog domain)
   - ❌ Wrong: `Code | CodeReview | CodeFormatter` (different concerns)

## Extraction Principles

✅ Content to extract:
- Core business entities: User, Order, Payment, Product, Blog
- Domain models with business meaning
- Domain-specific terminology unique to this project
- Compound names that represent business concepts: HttpClient, WebFetcher, TaskExecutor

❌ Content to exclude:
1. Technical infrastructure patterns alone: Controller, Service, Repository, Factory, Registry
2. Platform-specific implementations: anything with `.jvm`, `.js`, `.wasm`, `.ios`, `.android` suffix
3. Generic technical terms without business context: Parser, Executor, Handler, Manager
4. Common library APIs: List, Map, String, Flow, Channel

## Project README

$readme

## Output Format

CSV format with header: Chinese,Code Translation,Description

Rules:
- Chinese: Business term in Chinese (should be a domain concept, not technical term)
- Code Translation: Complete code identifiers (DO NOT split compound words), use | for related classes only
- Description: Brief description focusing on BUSINESS purpose

Good Examples:
```
Chinese,Code Translation,Description
博客,Blog | BlogPost | BlogService,博客文章管理
HTTP客户端,HttpClient | OkHttpClient,HTTP网络请求客户端
代码审查,CodeReview | ReviewComment,代码审查和评论
智能编辑,SmartEdit | SmartEditTask,AI辅助的智能代码编辑
领域字典,DomainDict | DomainDictService,业务术语字典生成
```

Bad Examples (DO NOT generate like this):
```
HTTP客户端,Http | Client,... ← WRONG: split compound word
执行器,Executor | Executor.jvm | Executor.js,... ← WRONG: platform variants
策略,Strategy | Policy | Decision,... ← WRONG: unrelated concepts grouped
```

## Code Context

Extract business concepts from the following code:

$code
"""
    }
    
    /**
     * Clean up LLM output to ensure valid CSV format
     */
    private fun cleanCsvOutput(rawOutput: String): String {
        val lines = rawOutput.lines()
        val csvLines = mutableListOf<String>()
        var hasHeader = false
        
        for (line in lines) {
            val trimmedLine = line.trim()
            
            // Skip empty lines and explanations
            if (trimmedLine.isEmpty() || isExplanationText(trimmedLine)) {
                continue
            }
            
            // Check if this looks like a CSV line
            if (trimmedLine.contains(",") && !trimmedLine.startsWith("#") && !trimmedLine.startsWith("*")) {
                // Add header if we haven't seen one yet
                if (!hasHeader) {
                    csvLines.add("Chinese,Code Translation,Description")
                    hasHeader = true
                }
                
                // Skip if this line looks like a header
                if (!isHeaderLine(trimmedLine)) {
                    csvLines.add(trimmedLine)
                }
            }
        }
        
        return csvLines.joinToString("\n")
    }
    
    /**
     * Check if a line looks like explanation text (not CSV data)
     */
    private fun isExplanationText(line: String): Boolean {
        val lowerLine = line.lowercase()
        
        val patterns = listOf(
            "以下", "content excluded", "excluded", "排除", "technical", "implementation",
            "说明", "解释", "备注", "notes", "csv", "返回", "return", "结果", "result",
            "完成", "done", "finished", "end", "结束", "没有", "none", "没有了",
            "based on", "according to", "here are", "here is"
        )
        
        return patterns.any { lowerLine.contains(it) }
    }
    
    /**
     * Check if a line is a CSV header
     */
    private fun isHeaderLine(line: String): Boolean {
        val lowerLine = line.lowercase()
        return lowerLine.contains("中文,代码翻译,描述") || 
               lowerLine.contains("chinese,code translation,description") ||
               lowerLine.contains("chinese,english,description") ||
               lowerLine.contains("名称,类型,来源")
    }
}

/**
 * Result of domain dictionary generation
 */
sealed class GenerationResult {
    data class Success(val content: String) : GenerationResult()
    data class Error(val message: String) : GenerationResult()
}
