package cc.unitmesh.indexer

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
     */
    suspend fun generateStreaming(): Flow<String> {
        val prompt = buildPrompt()
        
        return llmService.streamPrompt(prompt)
            .map { chunk -> chunk }
    }
    
    /**
     * Generate domain dictionary and return complete result
     */
    suspend fun generate(): String {
        val prompt = buildPrompt()
        
        val result = StringBuilder()
        llmService.streamPrompt(prompt).collect { chunk ->
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

"Code Translation" means the ACTUAL class names, method names, or variable names that exist in the codebase. It can be:
- A single identifier: `Blog`, `Payment`, `UserService`
- Multiple related identifiers: `Blog | BlogPost | BlogEntry` (use pipe separator)

## Extraction Principles

✅ Content to extract:
- Core business entities and their code representations
- Business concepts with actual class/method/variable names from code
- Domain-specific terminology found in the codebase
- Pinyin abbreviations and their meanings

❌ Content to exclude:
1. Pure technical suffixes alone: Controller, Service, Repository, Mapper, DTO, VO, Entity, Request, Response, Config, Filter, Interceptor, Exception, Helper, Utils
2. Technical operation verbs: validate, check, convert, deserialize, serialize, encode, decode
3. Common library APIs and standard class names: List, Map, String, Object

## Processing Rules

1. Extract the BUSINESS CORE from code names:
   - `BlogController` → Code Translation: `Blog | BlogController`, Business: "博客"
   - `UserService` → Code Translation: `User | UserService`, Business: "用户"
   - `PaymentRepository` → Code Translation: `Payment | PaymentRepository`, Business: "支付"

2. Group related code identifiers:
   - If `Blog`, `BlogPost`, `BlogEntry` all exist → "Blog | BlogPost | BlogEntry"
   - If `Order`, `OrderItem`, `OrderStatus` all exist → "Order | OrderItem | OrderStatus"

3. For method-derived concepts:
   - `checkIfVipAccount()` → Code Translation: `VipAccount | checkIfVipAccount`, Business: "VIP账户"

## Project README

$readme

## Output Format

CSV format with header: Chinese,Code Translation,Description

Rules:
- Chinese: Business term in Chinese
- Code Translation: Actual code identifiers (use | to separate multiple), e.g., "Blog | BlogPost | BlogService"
- Description: Brief description of the concept
- Wrap fields containing commas in double quotes

Example:
```
Chinese,Code Translation,Description
博客,Blog | BlogPost | BlogService,Blog post entity and related services
评论,Comment | CommentItem,User comments on content
支付,Payment | PaymentOrder | PaymentService,Payment transaction processing
用户,User | UserProfile | UserAccount,System user and profile management
草图,Sketch | SketchRenderer | CodeSketch,IDE canvas for visual interactions
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
