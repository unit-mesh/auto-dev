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
        
        return llmService.streamPrompt(prompt, compileDevIns = false)
            .map { chunk -> chunk }
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
        return """You are a DDD (Domain-Driven Design) expert building a business-oriented English-Chinese dictionary index from a codebase. You need to extract important concepts from the given code snippets to help others understand and use them.

**Extraction Principles:**

Content that should be extracted:
- Core business entities (e.g.: Blog, Comment, Payment, User as nouns)
- Business concepts and domain models (e.g.: Member, Points, Order)
- Incomprehensible words or pinyin abbreviations
- Domain-specific terminology

Content that should be excluded:
1. Technical vocabulary: Controller, Service, Repository, Mapper, DTO, VO, PO, Entity, Request, Response, Config, Filter, Interceptor, Exception, Helper, Utils, Util, etc.
2. Implementation details and data transfer objects: entries containing suffixes like "Request", "Response", "Dto", "Entity"
3. Technical operation verbs: validate, check, convert, deserialize, serialize, encode, decode, etc.
4. Technical operations in method names: e.g., "checkIfVipAccount" should extract only "VIP Account", "isLimitExceeded" should extract only "Limit"
5. Common library APIs (e.g., Spring, OkHttp, Retrofit) and common class names (e.g., List, Map)

**Processing Rules:**
1. If the extracted entry contains technical suffixes (e.g., "CreateCommentDto"), convert it to pure business concepts (e.g., "Comment" not "Create Comment Data Transfer Object")
2. If method names contain technical operations (e.g., "checkIfVipAccount"), extract business meaning ("VIP Account" not "Check If VIP Account")
3. If class names contain technical vocabulary suffixes, remove the suffix before adding to the dictionary

Project README information:

$readme

**Output Format Requirements:**

MUST return CSV format (comma-separated values)
CSV header: Chinese,Code Translation,Description
Each line contains one concept: [Chinese],[Code Translation],[Description]
Return ONLY data, no other text, explanations, tables, or markdown formatting
If data contains commas, wrap the field in double quotes, e.g.: "Concept A,Concept B",CodeConcept,Description

Example:
```
Chinese,Code Translation,Description
Blog,Blog,a blog post
Comment,Comment,a comment on a blog
Payment,Payment,a payment transaction
```

Based on the following filenames and code snippets, extract important business concepts and return them in CSV format:

$code
"""
    }
    
    /**
     * Clean up LLM output to ensure valid CSV format
     */
    private fun cleanCsvOutput(rawOutput: String): String {
        return CsvOutputCleaner.clean(rawOutput)
    }
}

/**
 * Utility class for cleaning CSV output from LLM
 * Exposed for testing
 */
object CsvOutputCleaner {
    private val EXPLANATION_PATTERNS = listOf(
        "以下", "content excluded", "excluded", "排除", "technical", "implementation",
        "说明", "解释", "备注", "notes", "csv", "返回", "return", "结果", "result",
        "完成", "done", "finished", "end", "结束", "没有", "none", "没有了",
        "based on", "according to", "here are", "here is"
    )
    
    private val HEADER_PATTERNS = listOf(
        "中文,代码翻译,描述",
        "chinese,code translation,description",
        "chinese,english,description",
        "名称,类型,来源"
    )
    
    /**
     * Clean up LLM output to ensure valid CSV format
     */
    fun clean(rawOutput: String): String {
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
    fun isExplanationText(line: String): Boolean {
        val lowerLine = line.lowercase()
        return EXPLANATION_PATTERNS.any { lowerLine.contains(it) }
    }
    
    /**
     * Check if a line is a CSV header
     */
    fun isHeaderLine(line: String): Boolean {
        val lowerLine = line.lowercase()
        return HEADER_PATTERNS.any { lowerLine.contains(it) }
    }
}

/**
 * Result of domain dictionary generation
 */
sealed class GenerationResult {
    data class Success(val content: String) : GenerationResult()
    data class Error(val message: String) : GenerationResult()
}
