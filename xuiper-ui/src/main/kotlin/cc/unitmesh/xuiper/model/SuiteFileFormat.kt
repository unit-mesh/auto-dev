package cc.unitmesh.xuiper.model

import kotlinx.serialization.Serializable

/**
 * File format for loading test suites from JSON.
 * This is separate from TestSuite to handle file path references.
 */
@Serializable
data class SuiteFileFormat(
    val id: String,
    val name: String,
    val description: String,
    val defaultPromptTemplate: String = "default",
    val models: List<ModelConfig> = listOf(ModelConfig.DEFAULT),
    val testCases: List<TestCaseFile>,
    val tags: List<String> = emptyList()
)

/**
 * Test case format for file loading.
 * Uses expectedDslFile instead of inline DSL.
 */
@Serializable
data class TestCaseFile(
    val id: String,
    val name: String,
    val description: String,
    val userPrompt: String,
    val expectedDslFile: String? = null,
    val category: TestCategory = TestCategory.BASIC,
    val difficulty: Difficulty = Difficulty.EASY,
    val tags: List<String> = emptyList()
)

