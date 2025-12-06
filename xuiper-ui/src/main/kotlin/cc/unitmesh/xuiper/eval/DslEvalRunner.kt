package cc.unitmesh.xuiper.eval

import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.xuiper.config.ConfigLoader
import cc.unitmesh.xuiper.eval.evaluator.EvaluatorRegistry
import cc.unitmesh.xuiper.model.*
import cc.unitmesh.xuiper.prompt.PromptTemplateRegistry
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

/**
 * Main runner for NanoDSL evaluation tests.
 * 
 * Usage:
 *   ./gradlew :xuiper-ui:runDslEval
 * 
 * Or with custom config:
 *   ./gradlew :xuiper-ui:runDslEval -DmodelProvider=openai -DmodelName=gpt-4o
 */
fun main(args: Array<String>) {
    val runner = DslEvalRunner()
    runBlocking {
        runner.runAllTests()
    }
}

/**
 * Extensible DSL evaluation runner.
 *
 * Design principles:
 * 1. Pluggable model configurations
 * 2. Customizable prompt templates
 * 3. Detailed result reporting
 * 4. Easy to extend with new test cases
 */
class DslEvalRunner(
    private val testCasesDir: String = "testcases",
    private val outputDir: String = "testcases/actual"
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // Cached config from ~/.autodev/config.yaml
    private var cachedConfigApiKey: String? = null
    private var cachedConfigProvider: String? = null

    /**
     * Run all test suites found in testcases directory
     */
    suspend fun runAllTests(): List<SuiteResult> {
        val suiteFiles = File(testCasesDir).listFiles { file -> 
            file.extension == "json" && file.name.contains("suite")
        } ?: emptyArray()
        
        logger.info { "Found ${suiteFiles.size} test suite(s)" }
        
        return suiteFiles.map { file ->
            val suite = loadSuite(file)
            runSuite(suite)
        }
    }

    /**
     * Run a single test suite
     */
    suspend fun runSuite(suite: TestSuite): SuiteResult {
        logger.info { "Running suite: ${suite.name} (${suite.testCases.size} tests)" }
        
        val results = mutableListOf<TestResult>()
        
        for (modelConfig in suite.models) {
            val llmService = createLLMService(modelConfig)
            
            for (testCase in suite.testCases) {
                val result = runTest(testCase, modelConfig, llmService, suite.defaultPromptTemplate)
                results.add(result)
                saveActualOutput(testCase, result)
            }
        }
        
        val summary = calculateSummary(results, suite)
        val suiteResult = SuiteResult(suite.id, results, summary)
        
        saveReport(suite, suiteResult)
        printSummary(suiteResult)
        
        return suiteResult
    }

    /**
     * Run a single test case
     */
    suspend fun runTest(
        testCase: TestCase,
        modelConfig: cc.unitmesh.xuiper.model.ModelConfig,
        llmService: KoogLLMService,
        promptTemplateId: String
    ): TestResult {
        logger.info { "  Running: ${testCase.name}" }
        
        val template = PromptTemplateRegistry.get(promptTemplateId)
        val rendered = template.render(mapOf("user_prompt" to testCase.userPrompt))
        
        var actualDsl = ""
        var tokenUsage: TokenUsage? = null
        val errors = mutableListOf<String>()
        
        val executionTimeMs = measureTimeMillis {
            try {
                val messages = listOf(
                    Message(role = MessageRole.SYSTEM, content = rendered.systemPrompt)
                )
                
                val responseFlow = llmService.streamPrompt(
                    userPrompt = rendered.userPrompt,
                    historyMessages = messages,
                    compileDevIns = false
                )
                
                actualDsl = responseFlow.toList().joinToString("")
                
                // Extract DSL from code fence if present
                actualDsl = extractDslFromResponse(actualDsl)
                
                val lastToken = llmService.getLastTokenInfo()
                tokenUsage = TokenUsage(
                    inputTokens = lastToken.inputTokens,
                    outputTokens = lastToken.outputTokens,
                    totalTokens = lastToken.totalTokens
                )
            } catch (e: Exception) {
                errors.add("LLM Error: ${e.message}")
                logger.error(e) { "Test failed: ${testCase.name}" }
            }
        }
        
        val scores = evaluateOutput(testCase, actualDsl)
        val overallScore = scores.values.average().toFloat()
        
        return TestResult(
            testCaseId = testCase.id,
            modelName = modelConfig.name,
            actualDsl = actualDsl,
            scores = scores,
            overallScore = overallScore,
            executionTimeMs = executionTimeMs,
            tokenUsage = tokenUsage,
            errors = errors
        )
    }

    private fun loadSuite(file: File): TestSuite {
        val content = file.readText()
        val parsed = json.decodeFromString<SuiteFileFormat>(content)
        
        // Load expected DSL from files
        val testCases = parsed.testCases.map { tc ->
            val expectedDsl = tc.expectedDslFile?.let { 
                File("$testCasesDir/$it").readText() 
            } ?: ""
            
            TestCase(
                id = tc.id,
                name = tc.name,
                description = tc.description,
                userPrompt = tc.userPrompt,
                expectedDsl = expectedDsl,
                category = tc.category,
                difficulty = tc.difficulty,
                tags = tc.tags
            )
        }
        
        return TestSuite(
            id = parsed.id,
            name = parsed.name,
            description = parsed.description,
            testCases = testCases,
            defaultPromptTemplate = parsed.defaultPromptTemplate,
            models = parsed.models,
            tags = parsed.tags
        )
    }
    
    private fun createLLMService(config: cc.unitmesh.xuiper.model.ModelConfig): KoogLLMService {
        val provider = LLMProviderType.valueOf(config.provider.uppercase())
        val apiKey = getApiKey(config.provider)

        if (apiKey.isBlank()) {
            val envName = getEnvName(config.provider)
            throw IllegalStateException(
                """
                |Missing API key for ${config.provider}.
                |
                |Please set the API key in one of the following ways:
                |1. ~/.autodev/config.yaml (recommended)
                |2. Environment variable: $envName
                |
                |Example for config.yaml:
                |  active: default
                |  configs:
                |    - name: default
                |      provider: ${config.provider}
                |      apiKey: your_api_key_here
                |      model: ${config.modelId}
                |
                |Example for environment variable:
                |  export $envName=your_api_key_here
                |  ./gradlew :xuiper-ui:runDslEval
                """.trimMargin()
            )
        }

        val modelConfig = ModelConfig(
            provider = provider,
            modelName = config.modelId,
            apiKey = apiKey,
            temperature = config.temperature.toDouble(),
            maxTokens = config.maxTokens
        )

        return KoogLLMService.create(modelConfig)
    }

    private fun getEnvName(provider: String): String = when (provider.lowercase()) {
        "openai" -> "OPENAI_API_KEY"
        "anthropic" -> "ANTHROPIC_API_KEY"
        "deepseek" -> "DEEPSEEK_API_KEY"
        "google" -> "GOOGLE_API_KEY"
        else -> "${provider.uppercase()}_API_KEY"
    }

    /**
     * Get API key from multiple sources:
     * 1. First try ~/.autodev/config.yaml (active config)
     * 2. Fall back to environment variable
     */
    private fun getApiKey(provider: String): String {
        // Try to get from ConfigLoader first
        if (cachedConfigApiKey == null) {
            try {
                val activeConfig = ConfigLoader.loadActiveConfig()
                if (activeConfig != null && activeConfig.apiKey.isNotBlank()) {
                    cachedConfigApiKey = activeConfig.apiKey
                    cachedConfigProvider = activeConfig.provider
                    logger.info { "Loaded API key from ~/.autodev/config.yaml (provider: ${activeConfig.provider})" }
                }
            } catch (e: Exception) {
                logger.debug { "Could not load config: ${e.message}" }
            }
        }

        // If cached config matches the requested provider, use it
        if (cachedConfigApiKey != null && cachedConfigProvider.equals(provider, ignoreCase = true)) {
            return cachedConfigApiKey!!
        }

        // Fall back to environment variable
        return System.getenv(getEnvName(provider)) ?: ""
    }

    private fun extractDslFromResponse(response: String): String {
        // Match ```nanodsl ... ``` or ``` ... ```
        val fencePattern = Regex("```(?:nanodsl)?\\s*\\n([\\s\\S]*?)\\n```")
        val match = fencePattern.find(response)
        return match?.groupValues?.get(1)?.trim() ?: response.trim()
    }

    private fun evaluateOutput(
        testCase: TestCase,
        actualDsl: String
    ): Map<EvaluationCriterion, Float> {
        // Use pluggable evaluator system
        return EvaluatorRegistry.evaluateAll(testCase, actualDsl)
    }

    private fun calculateSummary(results: List<TestResult>, suite: TestSuite): SuiteSummary {
        val passed = results.count { it.overallScore >= 0.7f }
        val averageScore = results.map { it.overallScore }.average().toFloat()
        val averageTime = results.map { it.executionTimeMs }.average().toLong()
        val totalTokens = results.mapNotNull { it.tokenUsage?.totalTokens }.sum()

        return SuiteSummary(
            totalTests = results.size,
            passed = passed,
            failed = results.size - passed,
            averageScore = averageScore,
            averageExecutionTimeMs = averageTime,
            totalTokens = totalTokens
        )
    }

    private fun saveActualOutput(testCase: TestCase, result: TestResult) {
        File(outputDir).mkdirs()
        val outputFile = File("$outputDir/${testCase.id}-${result.modelName}.nanodsl")
        outputFile.writeText(result.actualDsl)
        logger.debug { "    Saved: ${outputFile.path}" }
    }

    private fun saveReport(suite: TestSuite, result: SuiteResult) {
        val reportFile = File("$outputDir/${suite.id}-report.json")
        reportFile.writeText(json.encodeToString(SuiteResult.serializer(), result))
        logger.info { "Report saved: ${reportFile.path}" }
    }

    private fun printSummary(result: SuiteResult) {
        val summary = result.summary
        println("\n" + "=".repeat(60))
        println("EVALUATION RESULTS")
        println("=".repeat(60))
        println("Total: ${summary.totalTests} | Passed: ${summary.passed} | Failed: ${summary.failed}")
        println("Pass Rate: ${"%.1f".format(summary.passRate * 100)}%")
        println("Average Score: ${"%.2f".format(summary.averageScore)}")
        println("Average Time: ${summary.averageExecutionTimeMs}ms")
        println("Total Tokens: ${summary.totalTokens}")
        println("=".repeat(60) + "\n")
    }
}

