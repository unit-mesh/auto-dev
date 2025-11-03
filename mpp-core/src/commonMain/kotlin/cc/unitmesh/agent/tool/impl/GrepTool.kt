package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import kotlinx.serialization.Serializable

/**
 * Parameters for the Grep tool
 */
@Serializable
data class GrepParams(
    /**
     * The regular expression pattern to search for in file contents
     */
    val pattern: String,
    
    /**
     * The directory to search in (optional, defaults to project root)
     */
    val path: String? = null,
    
    /**
     * File pattern to include in the search (e.g. "*.kt", "*.{ts,js}")
     */
    val include: String? = null,
    
    /**
     * File pattern to exclude from the search
     */
    val exclude: String? = null,
    
    /**
     * Whether the search should be case-sensitive
     */
    val caseSensitive: Boolean = false,
    
    /**
     * Maximum number of matches to return
     */
    val maxMatches: Int = 100,
    
    /**
     * Number of context lines to show before and after each match
     */
    val contextLines: Int = 0,
    
    /**
     * Whether to search recursively in subdirectories
     */
    val recursive: Boolean = true
)

/**
 * Result object for a single grep match
 */
@Serializable
data class GrepMatch(
    val file: String,
    val lineNumber: Int,
    val line: String,
    val matchStart: Int,
    val matchEnd: Int,
    val contextBefore: List<String> = emptyList(),
    val contextAfter: List<String> = emptyList()
)

/**
 * Tool invocation for grep search
 */
class GrepInvocation(
    params: GrepParams,
    tool: GrepTool,
    private val fileSystem: ToolFileSystem
) : BaseToolInvocation<GrepParams, ToolResult>(params, tool) {
    
    override fun getDescription(): String {
        val searchPath = params.path ?: "project root"
        val includeDesc = params.include?.let { " (include: $it)" } ?: ""
        val excludeDesc = params.exclude?.let { " (exclude: $it)" } ?: ""
        return "Search for pattern '${params.pattern}' in $searchPath$includeDesc$excludeDesc"
    }
    
    override fun getToolLocations(): List<ToolLocation> {
        val searchPath = params.path ?: fileSystem.getProjectPath() ?: "."
        return listOf(ToolLocation(searchPath, LocationType.DIRECTORY))
    }
    
    override suspend fun execute(context: ToolExecutionContext): ToolResult {
        return ToolErrorUtils.safeExecute(ToolErrorType.INVALID_PATTERN) {
            val searchPath = params.path ?: fileSystem.getProjectPath() ?: "."
            
            // Validate search path exists
            if (!fileSystem.exists(searchPath)) {
                throw ToolException("Search path not found: $searchPath", ToolErrorType.DIRECTORY_NOT_FOUND)
            }
            
            // Create regex pattern
            val regex = try {
                if (params.caseSensitive) {
                    Regex(params.pattern)
                } else {
                    Regex(params.pattern, RegexOption.IGNORE_CASE)
                }
            } catch (e: Exception) {
                throw ToolException("Invalid regex pattern: ${params.pattern}", ToolErrorType.INVALID_PATTERN)
            }
            
            // Find files to search
            val filesToSearch = findFilesToSearch(searchPath)
            
            // Search for matches
            val matches = mutableListOf<GrepMatch>()
            var totalMatches = 0
            
            for (file in filesToSearch) {
                if (totalMatches >= params.maxMatches) break
                
                val fileMatches = searchInFile(file, regex)
                matches.addAll(fileMatches.take(params.maxMatches - totalMatches))
                totalMatches += fileMatches.size
            }
            
            // Format results
            val resultText = formatResults(matches, filesToSearch.size)
            
            val metadata = mapOf(
                "pattern" to params.pattern,
                "search_path" to searchPath,
                "files_searched" to filesToSearch.size.toString(),
                "total_matches" to matches.size.toString(),
                "case_sensitive" to params.caseSensitive.toString(),
                "recursive" to params.recursive.toString(),
                "include_pattern" to (params.include ?: ""),
                "exclude_pattern" to (params.exclude ?: "")
            )
            
            ToolResult.Success(resultText, metadata)
        }
    }
    
    private fun findFilesToSearch(searchPath: String): List<String> {
        val files = mutableListOf<String>()
        
        fun collectFiles(path: String) {
            val pathFiles = fileSystem.listFiles(path)
            
            for (file in pathFiles) {
                val fileInfo = fileSystem.getFileInfo(file)
                
                if (fileInfo?.isDirectory == true && params.recursive) {
                    collectFiles(file)
                } else if (fileInfo?.isDirectory == false) {
                    // Check include/exclude patterns
                    val fileName = file.substringAfterLast('/')
                    
                    val shouldInclude = params.include?.let { pattern ->
                        matchesGlobPattern(fileName, pattern)
                    } ?: true
                    
                    val shouldExclude = params.exclude?.let { pattern ->
                        matchesGlobPattern(fileName, pattern)
                    } ?: false
                    
                    if (shouldInclude && !shouldExclude) {
                        files.add(file)
                    }
                }
            }
        }
        
        collectFiles(searchPath)
        return files.sorted()
    }
    
    private suspend fun searchInFile(filePath: String, regex: Regex): List<GrepMatch> {
        val matches = mutableListOf<GrepMatch>()
        
        try {
            val content = fileSystem.readFile(filePath) ?: return emptyList()
            val lines = content.lines()
            
            lines.forEachIndexed { index, line ->
                val matchResults = regex.findAll(line)
                
                for (matchResult in matchResults) {
                    val contextBefore = if (params.contextLines > 0) {
                        val startIndex = (index - params.contextLines).coerceAtLeast(0)
                        lines.subList(startIndex, index)
                    } else emptyList()
                    
                    val contextAfter = if (params.contextLines > 0) {
                        val endIndex = (index + params.contextLines + 1).coerceAtMost(lines.size)
                        lines.subList(index + 1, endIndex)
                    } else emptyList()
                    
                    matches.add(
                        GrepMatch(
                            file = filePath,
                            lineNumber = index + 1, // 1-based line numbers
                            line = line,
                            matchStart = matchResult.range.first,
                            matchEnd = matchResult.range.last + 1,
                            contextBefore = contextBefore,
                            contextAfter = contextAfter
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Skip files that can't be read
        }
        
        return matches
    }
    
    private fun formatResults(matches: List<GrepMatch>, filesSearched: Int): String {
        if (matches.isEmpty()) {
            return "No matches found for pattern '${params.pattern}' in $filesSearched files."
        }
        
        val result = StringBuilder()
        result.appendLine("Found ${matches.size} matches for pattern '${params.pattern}' in $filesSearched files:")
        result.appendLine()
        
        var currentFile = ""
        
        for (match in matches) {
            if (match.file != currentFile) {
                if (currentFile.isNotEmpty()) result.appendLine()
                result.appendLine("File: ${match.file}")
                currentFile = match.file
            }
            
            // Show context before
            match.contextBefore.forEachIndexed { index, contextLine ->
                val lineNum = match.lineNumber - match.contextBefore.size + index
                result.appendLine("  $lineNum: $contextLine")
            }
            
            // Show the match line with highlighting
            val line = match.line
            val beforeMatch = line.substring(0, match.matchStart)
            val matchText = line.substring(match.matchStart, match.matchEnd)
            val afterMatch = line.substring(match.matchEnd)
            
            result.appendLine("→ ${match.lineNumber}: $beforeMatch**$matchText**$afterMatch")
            
            // Show context after
            match.contextAfter.forEachIndexed { index, contextLine ->
                val lineNum = match.lineNumber + index + 1
                result.appendLine("  $lineNum: $contextLine")
            }
            
            if (params.contextLines > 0) result.appendLine()
        }
        
        return result.toString().trim()
    }
    
    private fun matchesGlobPattern(fileName: String, pattern: String): Boolean {
        // Simple glob pattern matching
        val regexPattern = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
            .replace("{", "(")
            .replace("}", ")")
            .replace(",", "|")
        
        return fileName.matches(Regex(regexPattern))
    }
}

/**
 * Tool for searching file contents using regular expressions
 */
class GrepTool(
    private val fileSystem: ToolFileSystem
) : BaseExecutableTool<GrepParams, ToolResult>() {
    
    override val name: String = ToolType.Grep.name
    override val description: String = """
        Search for patterns in file contents using regular expressions.
        Supports file filtering with include/exclude patterns, case-sensitive/insensitive search,
        context lines around matches, and recursive directory traversal.
        Essential for finding specific code patterns, text, or analyzing codebase content.
    """.trimIndent()
    
    override fun getParameterClass(): String = GrepParams::class.simpleName ?: "GrepParams"
    
    override fun createToolInvocation(params: GrepParams): ToolInvocation<GrepParams, ToolResult> {
        validateParameters(params)
        return GrepInvocation(params, this, fileSystem)
    }
    
    private fun validateParameters(params: GrepParams) {
        if (params.pattern.isBlank()) {
            throw ToolException("Search pattern cannot be empty", ToolErrorType.MISSING_REQUIRED_PARAMETER)
        }

        if (params.maxMatches <= 0) {
            throw ToolException("Max matches must be positive", ToolErrorType.PARAMETER_OUT_OF_RANGE)
        }

        if (params.contextLines < 0) {
            throw ToolException("Context lines cannot be negative", ToolErrorType.PARAMETER_OUT_OF_RANGE)
        }

        // Test regex pattern validity
        try {
            Regex(params.pattern)
        } catch (e: Exception) {
            throw ToolException("Invalid regex pattern: ${params.pattern}", ToolErrorType.INVALID_PATTERN)
        }
    }
}
