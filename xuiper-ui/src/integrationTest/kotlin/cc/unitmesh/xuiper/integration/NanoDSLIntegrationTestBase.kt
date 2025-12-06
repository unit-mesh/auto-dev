package cc.unitmesh.xuiper.integration

import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.xuiper.config.ConfigLoader
import cc.unitmesh.xuiper.dsl.NanoDSL
import cc.unitmesh.xuiper.parser.ParseResult
import cc.unitmesh.xuiper.prompt.PromptTemplateRegistry
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Base class for NanoDSL integration tests.
 * 
 * These tests:
 * 1. Take a user requirement as input
 * 2. Call LLM to generate NanoDSL code
 * 3. Verify the generated DSL is compilable (parseable)
 * 
 * Test execution requires:
 * - Valid API key in ~/.autodev/config.yaml or environment variables
 * - Run with: ./gradlew :xuiper-ui:integrationTest
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class NanoDSLIntegrationTestBase {
    
    protected lateinit var llmService: KoogLLMService
    protected var isConfigured = false
    
    companion object {
        private const val TEST_CASES_DIR = "testcases"
        private const val EXPECT_DIR = "$TEST_CASES_DIR/expect"
        private const val ACTUAL_DIR = "$TEST_CASES_DIR/actual/integration"
    }
    
    @BeforeAll
    fun setup() {
        val activeConfig = ConfigLoader.loadActiveConfig()
        if (activeConfig != null && activeConfig.apiKey.isNotBlank()) {
            val provider = try {
                LLMProviderType.valueOf(activeConfig.provider.uppercase())
            } catch (e: Exception) {
                LLMProviderType.OPENAI
            }
            
            val modelConfig = ModelConfig(
                provider = provider,
                modelName = activeConfig.model,
                apiKey = activeConfig.apiKey,
                temperature = activeConfig.temperature,
                maxTokens = activeConfig.maxTokens,
                baseUrl = activeConfig.baseUrl
            )
            
            llmService = KoogLLMService.create(modelConfig)
            isConfigured = true
            logger.info { "Integration test configured with provider: ${activeConfig.provider}" }
        } else {
            // Try environment variables
            val apiKey = System.getenv("DEEPSEEK_API_KEY") 
                ?: System.getenv("OPENAI_API_KEY")
                ?: System.getenv("ANTHROPIC_API_KEY")
            
            if (!apiKey.isNullOrBlank()) {
                val provider = when {
                    System.getenv("DEEPSEEK_API_KEY") != null -> LLMProviderType.DEEPSEEK
                    System.getenv("OPENAI_API_KEY") != null -> LLMProviderType.OPENAI
                    else -> LLMProviderType.ANTHROPIC
                }
                
                val modelConfig = ModelConfig(
                    provider = provider,
                    modelName = getDefaultModel(provider),
                    apiKey = apiKey,
                    temperature = 0.0,
                    maxTokens = 2048
                )
                
                llmService = KoogLLMService.create(modelConfig)
                isConfigured = true
                logger.info { "Integration test configured with provider: $provider" }
            }
        }
        
        // Create output directory
        File(ACTUAL_DIR).mkdirs()
    }
    
    private fun getDefaultModel(provider: LLMProviderType): String = when (provider) {
        LLMProviderType.DEEPSEEK -> "deepseek-chat"
        LLMProviderType.OPENAI -> "gpt-4o"
        LLMProviderType.ANTHROPIC -> "claude-3-5-sonnet-20241022"
        else -> "gpt-4o"
    }
    
    /**
     * Skip test if LLM is not configured
     */
    protected fun assumeConfigured() {
        assumeTrue(isConfigured, "Skipping: LLM not configured (set ~/.autodev/config.yaml or env vars)")
    }
    
    /**
     * Generate NanoDSL from user prompt using LLM
     */
    protected suspend fun generateDsl(userPrompt: String): String {
        val template = PromptTemplateRegistry.get("default")
        val rendered = template.render(mapOf("user_prompt" to userPrompt))
        
        val messages = listOf(
            Message(role = MessageRole.SYSTEM, content = rendered.systemPrompt)
        )
        
        val responseFlow = llmService.streamPrompt(
            userPrompt = rendered.userPrompt,
            historyMessages = messages,
            compileDevIns = false
        )
        
        val response = responseFlow.toList().joinToString("")
        return extractDslFromResponse(response)
    }
    
    /**
     * Verify DSL is valid and can be parsed
     */
    protected fun verifyDslCompiles(dsl: String, testId: String): ParseResult {
        val result = NanoDSL.parseResult(dsl)
        
        // Save actual output for debugging
        saveActualOutput(testId, dsl, result)
        
        return result
    }
    
    /**
     * Load expected DSL from testcases/expect directory
     */
    protected fun loadExpectedDsl(filename: String): String {
        val file = File("$EXPECT_DIR/$filename")
        require(file.exists()) { "Expected DSL file not found: ${file.absolutePath}" }
        return file.readText()
    }
    
    private fun extractDslFromResponse(response: String): String {
        val fencePattern = Regex("```(?:nanodsl)?\\s*\\n([\\s\\S]*?)\\n```")
        val match = fencePattern.find(response)
        return match?.groupValues?.get(1)?.trim() ?: response.trim()
    }
    
    private fun saveActualOutput(testId: String, dsl: String, result: ParseResult) {
        val status = if (result is ParseResult.Success) "OK" else "FAIL"
        val outputFile = File("$ACTUAL_DIR/$testId-$status.nanodsl")
        outputFile.writeText(dsl)
        logger.info { "Saved actual output: ${outputFile.path}" }
    }
}

