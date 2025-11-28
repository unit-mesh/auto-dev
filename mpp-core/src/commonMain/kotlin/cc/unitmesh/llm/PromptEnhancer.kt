package cc.unitmesh.llm

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.subagent.DomainDictAgent
import cc.unitmesh.agent.subagent.DomainDictContext
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.parser.CodeFence
import cc.unitmesh.indexer.DomainDictService
import cc.unitmesh.indexer.template.TemplateEngine

/**
 * Enhancement mode for PromptEnhancer
 */
enum class EnhancementMode {
    /** Simple enhancement using existing domain.csv */
    SIMPLE,
    /** DeepResearch mode: iteratively improve domain.csv using DomainDictAgent */
    DEEP_RESEARCH
}

/**
 * Prompt Enhancer for improving user prompts using domain knowledge and context
 * 
 * This is a multiplatform version of the IDEA plugin's PromptEnhancer,
 * designed to work across JVM, JS, and other Kotlin targets.
 * 
 * Supports two enhancement modes:
 * - SIMPLE: Uses existing domain.csv for quick enhancement
 * - DEEP_RESEARCH: Uses DomainDictAgent to iteratively improve domain.csv before enhancement
 */
class PromptEnhancer(
    private val llmService: KoogLLMService,
    private val fileSystem: ProjectFileSystem,
    private val domainDictService: DomainDictService? = null,
    private val defaultMode: EnhancementMode = EnhancementMode.SIMPLE
) {
    private val logger = getLogger("PromptEnhancer")
    private val templateEngine = TemplateEngine()
    
    /**
     * Enhance a user prompt with domain knowledge and context
     * 
     * @param userInput The original user input to enhance
     * @param language Language preference ("zh" or "en")
     * @param mode Enhancement mode (SIMPLE or DEEP_RESEARCH)
     * @param onProgress Progress callback for DEEP_RESEARCH mode
     * @return Enhanced prompt text
     */
    suspend fun enhance(
        userInput: String,
        language: String = "zh",
        mode: EnhancementMode = defaultMode,
        onProgress: ((String) -> Unit)? = null
    ): String {
        if (userInput.isBlank()) {
            logger.warn("Empty user input provided for enhancement")
            return userInput
        }
        
        return when (mode) {
            EnhancementMode.SIMPLE -> enhanceSimple(userInput, language)
            EnhancementMode.DEEP_RESEARCH -> enhanceWithDeepResearch(userInput, language, onProgress ?: {})
        }
    }
    
    /**
     * Simple enhancement using existing domain.csv
     */
    private suspend fun enhanceSimple(userInput: String, language: String): String {
        try {
            // Build context with available information
            val context = buildContext(userInput, language)
            
            // Get enhancement template
            val template = getEnhancementTemplate(language)
            
            // Render the enhancement prompt
            val enhancementPrompt = templateEngine.render(template, mapOf("context" to context))
            
            logger.info("Sending enhancement request to LLM")
            
            // Call LLM for enhancement
            val result = StringBuilder()
            llmService.streamPrompt(
                userPrompt = enhancementPrompt,
                fileSystem = fileSystem,
                compileDevIns = false // Don't compile DevIns for enhancement prompts
            ).collect { chunk ->
                result.append(chunk)
            }
            
            // Extract enhanced content from LLM response
            val enhancedContent = extractEnhancedContent(result.toString())
            
            logger.info("Successfully enhanced prompt: ${userInput.take(50)}... -> ${enhancedContent.take(50)}...")
            
            return enhancedContent.ifEmpty { userInput }
            
        } catch (e: Exception) {
            logger.error("Failed to enhance prompt: ${e.message}", e)
            return userInput // Return original input on error
        }
    }
    
    /**
     * Deep research enhancement using DomainDictAgent
     * 
     * This mode:
     * 1. Uses DomainDictAgent to review and iteratively improve domain.csv
     * 2. Queries codebase for relevant domain terms
     * 3. Applies the optimized dictionary for final enhancement
     */
    private suspend fun enhanceWithDeepResearch(
        userInput: String,
        language: String,
        onProgress: (String) -> Unit
    ): String {
        if (domainDictService == null) {
            logger.warn("DomainDictService not available, falling back to simple enhancement")
            return enhanceSimple(userInput, language)
        }
        
        try {
            onProgress("🔍 Starting DeepResearch enhancement...")
            
            // Create and execute DomainDictAgent
            val dictAgent = DomainDictAgent(
                llmService = llmService,
                fileSystem = fileSystem,
                domainDictService = domainDictService,
                maxDefaultIterations = 3
            )
            
            val dictContext = DomainDictContext(
                userQuery = userInput,
                maxIterations = 3,
                focusArea = extractFocusArea(userInput)
            )
            
            onProgress("📚 Optimizing domain dictionary for: ${userInput.take(50)}...")
            
            // Execute dictionary optimization
            val dictResult = dictAgent.execute(dictContext, onProgress)
            
            if (dictResult.success) {
                onProgress("✅ Dictionary optimization complete")
                logger.info("Dictionary optimization completed: ${dictResult.metadata}")
            } else {
                onProgress("⚠️ Dictionary optimization failed, using existing dictionary")
                logger.warn("Dictionary optimization failed: ${dictResult.content}")
            }
            
            // Now perform simple enhancement with the optimized dictionary
            onProgress("🔄 Applying enhanced dictionary...")
            val result = enhanceSimple(userInput, language)
            
            onProgress("✅ Enhancement complete")
            return result
            
        } catch (e: Exception) {
            logger.error("DeepResearch enhancement failed: ${e.message}", e)
            onProgress("❌ DeepResearch failed, falling back to simple enhancement")
            return enhanceSimple(userInput, language)
        }
    }
    
    /**
     * Extract focus area from user input for targeted dictionary optimization
     */
    private fun extractFocusArea(userInput: String): String? {
        val keywords = listOf(
            "authentication" to "auth",
            "authorization" to "auth",
            "payment" to "payment",
            "agent" to "agent",
            "document" to "document",
            "database" to "database",
            "api" to "api",
            "认证" to "auth",
            "支付" to "payment",
            "代理" to "agent",
            "文档" to "document"
        )
        
        val lowerInput = userInput.lowercase()
        return keywords.find { lowerInput.contains(it.first) }?.second
    }
    
    /**
     * Build enhancement context with available information
     */
    private suspend fun buildContext(userInput: String, language: String): PromptEnhancerContext {
        val dict = loadDomainDict()
        val readme = loadReadmeContent()
        
        return PromptEnhancerContext.full(
            userInput = userInput,
            dict = dict,
            readme = readme,
            language = language
        )
    }
    
    /**
     * Load domain dictionary content
     */
    private suspend fun loadDomainDict(): String {
        return try {
            domainDictService?.loadContent() ?: ""
        } catch (e: Exception) {
            logger.warn("Failed to load domain dictionary: ${e.message}")
            ""
        }
    }
    
    /**
     * Load README file content
     */
    private suspend fun loadReadmeContent(): String {
        return try {
            findAndReadReadme()
        } catch (e: Exception) {
            logger.warn("Failed to load README: ${e.message}")
            ""
        }
    }
    
    /**
     * Find and read README file from project root
     */
    private suspend fun findAndReadReadme(): String {
        val readmeVariations = listOf(
            "README.md", "Readme.md", "readme.md",
            "README.txt", "Readme.txt", "readme.txt", 
            "README", "Readme", "readme"
        )
        
        for (variation in readmeVariations) {
            try {
                if (fileSystem.exists(variation)) {
                    val content = fileSystem.readFile(variation)
                    if (content != null) {
                        return content
                    }
                }
            } catch (e: Exception) {
                // Continue to next variation
                continue
            }
        }
        
        return ""
    }
    
    /**
     * Get enhancement template based on language
     */
    private fun getEnhancementTemplate(language: String): String {
        return when (language) {
            "en" -> getEnglishTemplate()
            "zh" -> getChineseTemplate()
            else -> getChineseTemplate() // Default to Chinese
        }
    }

    /**
     * Extract enhanced content from LLM response
     * Looks for markdown code blocks and extracts the content
     */
    private fun extractEnhancedContent(llmResponse: String): String {
        if (llmResponse.isBlank()) return ""

        try {
            // Try to parse as code fence first
            val codeFence = CodeFence.parse(llmResponse)
            if (codeFence.text.isNotBlank()) {
                return codeFence.text.trim()
            }

            // If no code fence found, return the response as-is (trimmed)
            return llmResponse.trim()

        } catch (e: Exception) {
            logger.warn("Failed to parse LLM response, returning as-is: ${e.message}")
            return llmResponse.trim()
        }
    }

    /**
     * Chinese enhancement template - optimized for CLI with English translation but Chinese response
     */
    private fun getChineseTemplate(): String {
        return """
你是一个专业的 AI 提示词优化专家。请帮我优化以下 prompt，并按照格式返回。

特别要求：
- 如果用户输入是中文，请将其翻译成英文并增强
- 如果用户输入是英文，请直接增强
- 无论输入语言如何，最终的回答都必须用中文

以下是用户提供的可参考的词汇表，请只考虑用户问题相关的部分。

```csv
${'$'}{context.dict}
```

以下是项目的 README 信息：
${'$'}{context.readme}

输出格式要求:

- 使用 markdown 代码块返回结果，方便我解析
- 如果用户输入是中文，请翻译成英文并增强专业术语
- 如果用户输入是英文，请直接增强专业术语
- 输出的内容只包含改进后的完整示例，不要添加任何其他内容
- 只包含改进后的完整示例，不要添加任何其他内容，不要返回过于丰富的内容
- 请不要做大量的联想，只针对用户的问题丰富词汇就行了

问答示例：

Question: 购买朝朝宝产品的流程
Answer: Purchase process for ZZB (朝朝宝) financial product (理财产品)

Question: 创建聚益生金金融产品的代码示例
Answer: Create code example for JYSJ (聚益生金) financial product (金融产品)

Question: Create user management system
Answer: Create user management system with authentication and authorization

现在，用户的 Question 是：${'$'}{context.userInput}
        """.trimIndent()
    }

    /**
     * English enhancement template
     */
    private fun getEnglishTemplate(): String {
        return """
You are a professional AI prompt optimization expert. Please help me optimize the following prompt and return it in the specified format.

Here is the domain vocabulary provided by the user, please only consider the parts relevant to the user's question.

```csv
${'$'}{context.dict}
```

Here is the project README information:
${'$'}{context.readme}

Output format requirements:

- Use markdown code blocks to return results for easy parsing
- The improved complete example should be consistent with the language of the user's prompt
- The improved complete example should be consistent with the information described in the user's prompt
- The output should only contain the improved complete example, do not add any other content
- Only include the improved complete example, do not add any other content, do not return overly rich content
- Please do not make extensive associations, just enrich the vocabulary for the user's question

Q&A Examples:

Question: Purchase process for ZZB product
Answer: Purchase process for ZZB (朝朝宝) financial (LiCai) product

Question: Code example for creating JYSJ financial product
Answer: Code example for creating JYSJ (聚益生金) FinancialProduct

Now, the user's Question is: ${'$'}{context.userInput}
        """.trimIndent()
    }
}
