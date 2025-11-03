package cc.unitmesh.codegraph.parser

import cc.unitmesh.codegraph.model.CodeGraph
import cc.unitmesh.codegraph.model.CodeNode
import cc.unitmesh.codegraph.model.CodeRelationship

/**
 * Common interface for code parsers across different platforms.
 * Implementations will use platform-specific TreeSitter bindings.
 */
interface CodeParser {
    /**
     * Parse source code and extract nodes
     * 
     * @param sourceCode The source code to parse
     * @param filePath The file path (for reference)
     * @param language The programming language
     * @return List of code nodes
     */
    suspend fun parseNodes(
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<CodeNode>
    
    /**
     * Parse source code and extract both nodes and relationships
     * 
     * @param sourceCode The source code to parse
     * @param filePath The file path (for reference)
     * @param language The programming language
     * @return Pair of nodes and relationships
     */
    suspend fun parseNodesAndRelationships(
        sourceCode: String,
        filePath: String,
        language: Language
    ): Pair<List<CodeNode>, List<CodeRelationship>>
    
    /**
     * Parse multiple files and build a complete code graph
     * 
     * @param files Map of file paths to source code
     * @param language The programming language
     * @return Complete code graph
     */
    suspend fun parseCodeGraph(
        files: Map<String, String>,
        language: Language
    ): CodeGraph
}

/**
 * Supported programming languages
 */
enum class Language {
    JAVA,
    KOTLIN,
    CSHARP,
    JAVASCRIPT,
    TYPESCRIPT,
    PYTHON,
    GO,
    RUST,
    UNKNOWN
}

