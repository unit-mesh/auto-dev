package cc.unitmesh.xuiper.model

import kotlinx.serialization.Serializable

/**
 * A single test case for evaluating NanoDSL generation by AI.
 *
 * Design principles:
 * - Each test case has a clear description of what UI to generate
 * - Expected output provides the ground truth for comparison
 * - Metadata enables filtering and categorization
 * - Evaluation criteria define how to score AI output
 */
@Serializable
data class TestCase(
    /** Unique identifier for the test case */
    val id: String,
    
    /** Human-readable name */
    val name: String,
    
    /** Detailed description of the UI requirement */
    val description: String,
    
    /** The user prompt that will be sent to the LLM */
    val userPrompt: String,
    
    /** Expected NanoDSL output (ground truth) */
    val expectedDsl: String,
    
    /** Category for grouping related tests */
    val category: TestCategory = TestCategory.BASIC,
    
    /** Difficulty level */
    val difficulty: Difficulty = Difficulty.EASY,
    
    /** Evaluation criteria for this test */
    val criteria: List<EvaluationCriterion> = defaultCriteria(),
    
    /** Tags for filtering */
    val tags: List<String> = emptyList(),
    
    /** Optional context data (e.g., schema definitions) */
    val context: Map<String, String> = emptyMap()
) {
    companion object {
        fun defaultCriteria() = listOf(
            EvaluationCriterion.SYNTAX_VALID,
            EvaluationCriterion.COMPONENT_MATCH,
            EvaluationCriterion.STRUCTURE_SIMILAR
        )
    }
}

/**
 * Test case categories
 */
@Serializable
enum class TestCategory {
    /** Basic component rendering */
    BASIC,
    /** Layout components (VStack, HStack) */
    LAYOUT,
    /** State management with bindings */
    STATE,
    /** Conditional rendering (if/else) */
    CONDITIONAL,
    /** List rendering (for loops) */
    LIST,
    /** User interactions (buttons, inputs) */
    INTERACTION,
    /** Complex composite components */
    COMPOSITE,
    /** HTTP requests (fetch, api calls) */
    HTTP,
    /** Edge cases and error handling */
    EDGE_CASE
}

/**
 * Test difficulty levels
 */
@Serializable
enum class Difficulty {
    EASY,
    MEDIUM,
    HARD,
    EXPERT
}

/**
 * Evaluation criteria for scoring AI output
 */
@Serializable
enum class EvaluationCriterion {
    /** DSL syntax is valid and parseable */
    SYNTAX_VALID,
    /** Required components are present */
    COMPONENT_MATCH,
    /** Overall structure is similar to expected */
    STRUCTURE_SIMILAR,
    /** Properties and attributes are correct */
    PROPS_CORRECT,
    /** State bindings are correctly used */
    STATE_BINDINGS,
    /** Actions are properly defined */
    ACTIONS_CORRECT,
    /** Indentation and formatting are correct */
    FORMATTING,
    /** No extra/unnecessary components */
    NO_REDUNDANCY
}

/**
 * Result of running a single test case
 */
@Serializable
data class TestResult(
    val testCaseId: String,
    val modelName: String,
    val actualDsl: String,
    val scores: Map<EvaluationCriterion, Float>,
    val overallScore: Float,
    val executionTimeMs: Long,
    val tokenUsage: TokenUsage? = null,
    val errors: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Token usage information
 */
@Serializable
data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int
)

