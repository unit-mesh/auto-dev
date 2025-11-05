package cc.unitmesh.indexer

import cc.unitmesh.indexer.template.TemplateManager
import cc.unitmesh.indexer.template.DomainDictTemplateContext
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
    private var maxTokenLength: Int = 8192
) {
    init {
        require(maxTokenLength <= modelConfig.maxTokens) {
            "maxTokenLength ($maxTokenLength) cannot exceed model's max tokens (${modelConfig.maxTokens})"
        }

        maxTokenLength = modelConfig.maxTokens * 9 / 10  // Use 90% of model max tokens
    }

    private val domainDictService = DomainDictService(fileSystem)
    private val templateManager = TemplateManager()
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
        println("Domain Dictionary Generation:")
        println("- Level 1 count: ${domainDict.metadata["level1_count"]}")
        println("- Level 2 count: ${domainDict.metadata["level2_count"]}")
        println("- Total tokens: $codeTokens")
        println("- README included: ${readme.isNotEmpty()}")
        
        val weightStats = domainDict.getWeightStatistics()
        println("- Weight stats: Avg=${weightStats["averageWeight"]}, " +
                "Critical=${weightStats["criticalCount"]}, High=${weightStats["highCount"]}")
        
        // Create template context
        val context = DomainDictTemplateContext(codeContext, readme)
        
        // Render template
        val templateName = if (isChineseEnvironment()) "indexer.vm" else "indexer_en.vm"
        return templateManager.renderTemplate(templateName, context)
    }
    
    /**
     * Clean up LLM output to ensure valid CSV format
     */
    private fun cleanCsvOutput(rawOutput: String): String {
        val lines = rawOutput.lines()
        val csvLines = mutableListOf<String>()
        var inCsvSection = false
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
                    if (isChineseEnvironment()) {
                        csvLines.add("中文,代码翻译,描述")
                    } else {
                        csvLines.add("Chinese,English,Description")
                    }
                    hasHeader = true
                }
                
                // Skip if this line looks like a header
                if (!isHeaderLine(trimmedLine)) {
                    csvLines.add(trimmedLine)
                }
                inCsvSection = true
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
               lowerLine.contains("chinese,english,description") ||
               lowerLine.contains("名称,类型,来源")
    }
    
    /**
     * Detect if we're in a Chinese environment
     */
    private fun isChineseEnvironment(): Boolean {
        // Simple heuristic - could be made more sophisticated
        return true  // Default to Chinese for now
    }
}

/**
 * Result of domain dictionary generation
 */
sealed class GenerationResult {
    data class Success(val content: String) : GenerationResult()
    data class Error(val message: String) : GenerationResult()
}
