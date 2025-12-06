package cc.unitmesh.xuiper.model

import kotlinx.serialization.Serializable

/**
 * A collection of test cases that can be run together.
 * 
 * TestSuite supports:
 * - Grouping related test cases
 * - Shared configuration (prompt templates, model settings)
 * - Aggregate reporting
 */
@Serializable
data class TestSuite(
    /** Unique identifier */
    val id: String,
    
    /** Suite name */
    val name: String,
    
    /** Suite description */
    val description: String,
    
    /** List of test cases in this suite */
    val testCases: List<TestCase>,
    
    /** Default prompt template ID to use */
    val defaultPromptTemplate: String = "default",
    
    /** Model configurations to test against */
    val models: List<ModelConfig> = listOf(ModelConfig.DEFAULT),
    
    /** Suite-level tags */
    val tags: List<String> = emptyList()
)

/**
 * Model configuration for testing
 */
@Serializable
data class ModelConfig(
    val name: String,
    val provider: String,
    val modelId: String,
    val temperature: Float = 0.0f,
    val maxTokens: Int = 2048
) {
    companion object {
        val DEFAULT = ModelConfig(
            name = "deepseek-chat",
            provider = "deepseek",
            modelId = "deepseek-chat",
            temperature = 0.0f
        )
        
        val GPT4 = ModelConfig(
            name = "gpt-4o",
            provider = "openai",
            modelId = "gpt-4o",
            temperature = 0.0f
        )
        
        val CLAUDE = ModelConfig(
            name = "claude-3.5-sonnet",
            provider = "anthropic",
            modelId = "claude-3-5-sonnet-20241022",
            temperature = 0.0f
        )
    }
}

/**
 * Aggregate results for a test suite run
 */
@Serializable
data class SuiteResult(
    val suiteId: String,
    val results: List<TestResult>,
    val summary: SuiteSummary,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Summary statistics for a suite run
 */
@Serializable
data class SuiteSummary(
    val totalTests: Int,
    val passed: Int,
    val failed: Int,
    val averageScore: Float,
    val averageExecutionTimeMs: Long,
    val totalTokens: Int,
    val scoresByCategory: Map<TestCategory, Float> = emptyMap(),
    val scoresByCriterion: Map<EvaluationCriterion, Float> = emptyMap()
) {
    val passRate: Float get() = if (totalTests > 0) passed.toFloat() / totalTests else 0f
}

/**
 * Loader for test suites from various sources
 */
interface TestSuiteLoader {
    fun loadSuite(path: String): TestSuite
    fun loadAllSuites(directory: String): List<TestSuite>
}

/**
 * Reporter for test results
 */
interface TestReporter {
    fun reportResult(result: TestResult)
    fun reportSuite(suiteResult: SuiteResult)
    fun generateReport(results: List<SuiteResult>): String
}

