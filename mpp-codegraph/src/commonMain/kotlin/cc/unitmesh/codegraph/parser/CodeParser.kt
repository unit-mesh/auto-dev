package cc.unitmesh.codegraph.parser

import cc.unitmesh.codegraph.model.CodeGraph
import cc.unitmesh.codegraph.model.CodeNode
import cc.unitmesh.codegraph.model.CodeRelationship
import cc.unitmesh.codegraph.model.FileImports
import cc.unitmesh.codegraph.model.ImportInfo

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
    
    /**
     * Parse import statements from source code using TreeSitter AST
     * 
     * @param sourceCode The source code to parse
     * @param filePath The file path (for reference)
     * @param language The programming language
     * @return List of import info
     */
    suspend fun parseImports(
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<ImportInfo>
    
    /**
     * Parse imports from multiple files
     * 
     * @param files Map of file paths to source code
     * @param language The programming language
     * @return Map of file path to file imports
     */
    suspend fun parseAllImports(
        files: Map<String, String>,
        language: Language
    ): Map<String, FileImports> {
        return files.mapValues { (filePath, sourceCode) ->
            val imports = parseImports(sourceCode, filePath, language)
            FileImports(
                filePath = filePath,
                imports = imports,
                packageName = extractPackageName(sourceCode, language)
            )
        }
    }
    
    /**
     * Extract package/module name from source code
     */
    fun extractPackageName(sourceCode: String, language: Language): String {
        // Default implementation using regex (can be overridden for AST-based)
        return when (language) {
            Language.JAVA, Language.KOTLIN -> {
                val regex = Regex("""package\s+([\w.]+)""")
                regex.find(sourceCode)?.groupValues?.get(1) ?: ""
            }
            Language.PYTHON -> {
                // Python uses directory structure, no explicit package
                ""
            }
            Language.GO -> {
                val regex = Regex("""package\s+(\w+)""")
                regex.find(sourceCode)?.groupValues?.get(1) ?: ""
            }
            else -> ""
        }
    }
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

