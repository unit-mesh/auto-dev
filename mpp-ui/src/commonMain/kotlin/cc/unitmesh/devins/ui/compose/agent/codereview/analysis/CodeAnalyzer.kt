package cc.unitmesh.devins.ui.compose.agent.codereview.analysis

import cc.unitmesh.agent.diff.DiffLineType
import cc.unitmesh.agent.logging.AutoDevLogger
import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.codegraph.CodeGraphFactory
import cc.unitmesh.codegraph.model.CodeElementType
import cc.unitmesh.codegraph.parser.Language
import cc.unitmesh.devins.ui.compose.agent.codereview.DiffFileInfo
import cc.unitmesh.devins.ui.compose.agent.codereview.ModifiedCodeRange
import cc.unitmesh.devins.workspace.Workspace

/**
 * Analyzes code structure to identify modified functions, classes, and other code elements.
 * This is a pure non-AI component that uses CodeGraph for parsing and diff information
 * to determine what code was changed.
 */
class CodeAnalyzer(private val workspace: Workspace) {
    private val parser = CodeGraphFactory.createParser()

    /**
     * Analyze modified code to find which functions/classes were changed.
     * This uses CodeGraph to parse files and map diff changes to code elements.
     *
     * @param diffFiles List of changed files with their diffs
     * @param projectPath Root path of the project
     * @param progressCallback Optional callback for progress updates
     * @return Map of file paths to lists of modified code ranges
     */
    suspend fun analyzeModifiedCode(
        diffFiles: List<DiffFileInfo>,
        projectPath: String,
        progressCallback: ((String) -> Unit)? = null
    ): Map<String, List<ModifiedCodeRange>> {
        val modifiedRanges = mutableMapOf<String, MutableList<ModifiedCodeRange>>()

        try {
            progressCallback?.invoke("🔍 Analyzing modified code structure...\n")

            for (diffFile in diffFiles) {
                // Skip deleted files
                if (diffFile.changeType == ChangeType.DELETE) continue

                val filePath = diffFile.path

                // Determine language from file extension
                val language = detectLanguageFromPath(filePath)
                if (language == Language.UNKNOWN) {
                    AutoDevLogger.info("CodeAnalyzer") {
                        "Skipping $filePath - unknown language"
                    }
                    continue
                }

                // Read file content
                val fileContent = try {
                    workspace.fileSystem.readFile(filePath)
                } catch (e: Exception) {
                    AutoDevLogger.warn("CodeAnalyzer") {
                        "Failed to read file $filePath: ${e.message}"
                    }
                    continue
                }

                if (fileContent == null) {
                    AutoDevLogger.warn("CodeAnalyzer") {
                        "File not found: $filePath"
                    }
                    continue
                }

                // Get modified line numbers from hunks
                val modifiedLines = mutableSetOf<Int>()
                diffFile.hunks.forEach { hunk ->
                    hunk.lines.forEach { line ->
                        if (line.type == DiffLineType.ADDED) {
                            line.newLineNumber?.let { modifiedLines.add(it) }
                        }
                    }
                }

                if (modifiedLines.isEmpty()) continue

                // Parse the file to get code structure
                val codeNodes = try {
                    parser.parseNodes(fileContent, filePath, language)
                } catch (e: Exception) {
                    AutoDevLogger.error("CodeAnalyzer") {
                        "Failed to parse $filePath: ${e.message}"
                    }
                    continue
                }

                // Find which functions/classes contain modified lines
                val affectedNodes = codeNodes.filter { node ->
                    // Only consider methods, functions, and classes
                    when (node.type) {
                        CodeElementType.METHOD,
                        CodeElementType.FUNCTION,
                        CodeElementType.CLASS,
                        CodeElementType.INTERFACE -> {
                            // Check if any modified line falls within this node's range
                            modifiedLines.any { line ->
                                line >= node.startLine && line <= node.endLine
                            }
                        }

                        else -> false
                    }
                }

                // Create ModifiedCodeRange for each affected node
                val ranges = affectedNodes.map { node ->
                    val affectedLines = modifiedLines.filter { line ->
                        line >= node.startLine && line <= node.endLine
                    }.sorted()

                    ModifiedCodeRange(
                        filePath = filePath,
                        elementName = node.name,
                        elementType = node.type.name,
                        startLine = node.startLine,
                        endLine = node.endLine,
                        modifiedLines = affectedLines
                    )
                }

                if (ranges.isNotEmpty()) {
                    modifiedRanges.getOrPut(filePath) { mutableListOf() }.addAll(ranges)
                }

                progressCallback?.invoke("  ✓ $filePath: Found ${ranges.size} modified code element(s)\n")
            }

            val totalRanges = modifiedRanges.values.sumOf { it.size }
            progressCallback?.invoke("\n✅ Code analysis complete. Found $totalRanges modified code elements.\n\n")
        } catch (e: Exception) {
            AutoDevLogger.error("CodeAnalyzer") {
                "Failed to analyze modified code: ${e.message}"
            }
            progressCallback?.invoke("\n⚠️ Failed to analyze code structure: ${e.message}\n\n")
        }

        println(modifiedRanges)
        return modifiedRanges
    }

    /**
     * Detect programming language from file path extension.
     *
     * @param filePath The file path to analyze
     * @return The detected Language enum value
     */
    fun detectLanguageFromPath(filePath: String): Language {
        return when (filePath.substringAfterLast('.', "").lowercase()) {
            "java" -> Language.JAVA
            "kt", "kts" -> Language.KOTLIN
            "cs" -> Language.CSHARP
            "js", "jsx" -> Language.JAVASCRIPT
            "ts", "tsx" -> Language.TYPESCRIPT
            "py" -> Language.PYTHON
            "go" -> Language.GO
            "rs" -> Language.RUST
            else -> Language.UNKNOWN
        }
    }
}
