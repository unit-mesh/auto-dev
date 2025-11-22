package cc.unitmesh.devins.ui.compose.agent.codereview

import kotlinx.serialization.Serializable

/**
 * Information about a test file and its test cases
 */
@Serializable
data class TestFileInfo(
    /**
     * Path to the test file (relative to project root)
     */
    val filePath: String,
    
    /**
     * Programming language of the test file
     */
    val language: String,
    
    /**
     * Test cases in this file organized as a tree structure
     */
    val testCases: List<TestCaseNode>,
    
    /**
     * Whether the test file exists (vs. expected but not found)
     */
    val exists: Boolean = true,
    
    /**
     * Error message if the test file couldn't be parsed
     */
    val parseError: String? = null
)

/**
 * Represents a node in the test case tree (class or method)
 */
@Serializable
data class TestCaseNode(
    /**
     * Name of the test class or method
     */
    val name: String,
    
    /**
     * Type of the node: CLASS or METHOD
     */
    val type: TestNodeType,
    
    /**
     * Children nodes (methods within a class, or nested classes)
     */
    val children: List<TestCaseNode> = emptyList(),
    
    /**
     * Start line number (1-based) in the file
     */
    val startLine: Int = 0,
    
    /**
     * End line number (1-based) in the file
     */
    val endLine: Int = 0,
    
    /**
     * Full qualified name (for classes)
     */
    val qualifiedName: String? = null
)

/**
 * Type of test node
 */
@Serializable
enum class TestNodeType {
    CLASS,
    METHOD
}

/**
 * Information about a test method
 */
@Serializable
data class TestMethodInfo(
    /**
     * Name of the test method
     */
    val name: String,
    
    /**
     * Start line number (1-based)
     */
    val startLine: Int,
    
    /**
     * End line number (1-based)
     */
    val endLine: Int,
    
    /**
     * Test annotations (e.g., @Test, @ParameterizedTest)
     */
    val annotations: List<String> = emptyList()
)
