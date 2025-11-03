package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.filesystem.FileInfo
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.gitignore.GitIgnoreParser
import kotlinx.serialization.Serializable

@Serializable
data class GlobParams(
    val pattern: String,
    val path: String? = null,
    val includeDirectories: Boolean = false,
    val includeHidden: Boolean = false,
    val maxResults: Int = 1000,
    val sortByTime: Boolean = false,
    val includeFileInfo: Boolean = false,
    val respectGitIgnore: Boolean = true
)

@Serializable
data class GlobFileResult(
    val path: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val size: Long? = null,
    val lastModified: Long? = null
)

class GlobInvocation(
    params: GlobParams,
    tool: GlobTool,
    private val fileSystem: ToolFileSystem,
    private val gitIgnoreParser: GitIgnoreParser? = null
) : BaseToolInvocation<GlobParams, ToolResult>(params, tool) {
    
    override fun getDescription(): String {
        val searchPath = params.path ?: "project root"
        val typeDesc = if (params.includeDirectories) "files and directories" else "files"
        return "Find $typeDesc matching pattern '${params.pattern}' in $searchPath"
    }
    
    override fun getToolLocations(): List<ToolLocation> {
        val searchPath = params.path ?: fileSystem.getProjectPath() ?: "."
        return listOf(ToolLocation(searchPath, LocationType.DIRECTORY))
    }
    
    override suspend fun execute(context: ToolExecutionContext): ToolResult {
        return ToolErrorUtils.safeExecute(ToolErrorType.INVALID_PATTERN) {
            val searchPath = params.path ?: fileSystem.getProjectPath() ?: "."
            
            if (!fileSystem.exists(searchPath)) {
                throw ToolException("Search path not found: $searchPath", ToolErrorType.DIRECTORY_NOT_FOUND)
            }
            
            val matches = findMatches(searchPath)
            val sortedMatches = if (params.sortByTime) {
                matches.sortedByDescending { it.lastModified ?: 0L }
            } else {
                matches.sortedBy { it.path }
            }
            
            val limitedMatches = sortedMatches.take(params.maxResults)
            val resultText = formatResults(limitedMatches, matches.size)
            
            val metadata = mapOf(
                "pattern" to params.pattern,
                "search_path" to searchPath,
                "total_matches" to matches.size.toString(),
                "returned_matches" to limitedMatches.size.toString(),
                "include_directories" to params.includeDirectories.toString(),
                "include_hidden" to params.includeHidden.toString(),
                "sort_by_time" to params.sortByTime.toString(),
                "respect_gitignore" to params.respectGitIgnore.toString()
            )
            
            ToolResult.Success(resultText, metadata)
        }
    }
    
    private fun findMatches(searchPath: String): List<GlobFileResult> {
        val matches = mutableListOf<GlobFileResult>()
        val projectPath = fileSystem.getProjectPath()

        fun collectMatches(currentPath: String) {
            try {
                val files = fileSystem.listFiles(currentPath)

                for (file in files) {
                    val fileInfo = fileSystem.getFileInfo(file)
                    if (fileInfo == null) continue

                    val fileName = file.substringAfterLast('/')

                    if (!params.includeHidden && fileName.startsWith('.')) {
                        continue
                    }

                    // Check gitignore if enabled
                    if (params.respectGitIgnore && gitIgnoreParser != null && projectPath != null) {
                        val relativePath = if (file.startsWith(projectPath)) {
                            file.removePrefix(projectPath).removePrefix("/")
                        } else {
                            file
                        }

                        if (gitIgnoreParser.isIgnored(relativePath)) {
                            continue
                        }
                    }

                    if (fileInfo.isDirectory) {
                        collectMatches(file)

                        if (params.includeDirectories && matchesPattern(file, params.pattern)) {
                            matches.add(createFileResult(file, fileInfo, projectPath))
                        }
                    } else {
                        if (matchesPattern(file, params.pattern)) {
                            matches.add(createFileResult(file, fileInfo, projectPath))
                        }
                    }
                }
            } catch (e: Exception) {
                // Skip directories that can't be read
            }
        }

        collectMatches(searchPath)
        return matches
    }
    
    private fun createFileResult(filePath: String, fileInfo: FileInfo, projectPath: String?): GlobFileResult {
        val relativePath = if (projectPath != null && filePath.startsWith(projectPath)) {
            filePath.removePrefix(projectPath).removePrefix("/")
        } else {
            filePath
        }
        
        return GlobFileResult(
            path = filePath,
            relativePath = relativePath,
            isDirectory = fileInfo.isDirectory,
            size = if (params.includeFileInfo) fileInfo.size else null,
            lastModified = if (params.includeFileInfo) fileInfo.lastModified else null
        )
    }
    
    private fun matchesPattern(filePath: String, pattern: String): Boolean {
        val fileName = filePath.substringAfterLast('/')
        
        return when {
            !pattern.contains('/') -> matchesGlobPattern(fileName, pattern)
            pattern.contains("**") -> matchesRecursivePattern(filePath, pattern)
            else -> matchesPathPattern(filePath, pattern)
        }
    }
    
    private fun matchesGlobPattern(text: String, pattern: String): Boolean {
        val regexPattern = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
            .replace("{", "(")
            .replace("}", ")")
            .replace(",", "|")
        
        return text.matches(Regex(regexPattern))
    }
    
    private fun matchesRecursivePattern(filePath: String, pattern: String): Boolean {
        val patternParts = pattern.split("**")
        
        if (patternParts.size == 1) {
            return matchesGlobPattern(filePath, pattern)
        }
        
        val prefix = patternParts[0].removeSuffix("/")
        val suffix = patternParts[1].removePrefix("/")
        
        val prefixMatches = prefix.isEmpty() || filePath.startsWith(prefix)
        val suffixMatches = suffix.isEmpty() || matchesGlobPattern(filePath.substringAfterLast('/'), suffix)
        
        return prefixMatches && suffixMatches
    }
    
    private fun matchesPathPattern(filePath: String, pattern: String): Boolean {
        return matchesGlobPattern(filePath, pattern)
    }
    
    private fun formatResults(matches: List<GlobFileResult>, totalMatches: Int): String {
        if (matches.isEmpty()) {
            return "No files found matching pattern '${params.pattern}'."
        }
        
        val result = StringBuilder()
        result.appendLine("Found $totalMatches files matching pattern '${params.pattern}':")
        
        if (totalMatches > matches.size) {
            result.appendLine("(Showing first ${matches.size} results)")
        }
        
        result.appendLine()
        
        for (match in matches) {
            val typeIndicator = if (match.isDirectory) "📁" else "📄"
            val path = match.relativePath.ifEmpty { match.path }
            
            result.append("$typeIndicator $path")
            
            if (params.includeFileInfo && !match.isDirectory) {
                match.size?.let { size ->
                    result.append(" (${formatFileSize(size)})")
                }
            }
            
            result.appendLine()
        }
        
        return result.toString().trim()
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            else -> "${bytes / (1024 * 1024 * 1024)}GB"
        }
    }
}

class GlobTool(
    private val fileSystem: ToolFileSystem
) : BaseExecutableTool<GlobParams, ToolResult>() {

    override val name: String = ToolNames.GLOB
    override val description: String = """
        Find files and directories using glob patterns with wildcard support.
        Supports recursive search (**), character classes ([abc]), alternatives ({a,b}),
        and standard wildcards (* and ?). Respects .gitignore rules by default.
        Essential for discovering files by pattern, analyzing project structure,
        or finding files for batch operations.
    """.trimIndent()

    override fun getParameterClass(): String = GlobParams::class.simpleName ?: "GlobParams"

    override fun createToolInvocation(params: GlobParams): ToolInvocation<GlobParams, ToolResult> {
        validateParameters(params)

        // Create GitIgnoreParser if respectGitIgnore is enabled and we have a project path
        val gitIgnoreParser = if (params.respectGitIgnore) {
            val projectPath = fileSystem.getProjectPath()
            if (projectPath != null) {
                try {
                    GitIgnoreParser(projectPath)
                } catch (e: Exception) {
                    // If GitIgnore parser fails to initialize, continue without it
                    null
                }
            } else {
                null
            }
        } else {
            null
        }

        return GlobInvocation(params, this, fileSystem, gitIgnoreParser)
    }
    
    private fun validateParameters(params: GlobParams) {
        if (params.pattern.isBlank()) {
            throw ToolException("Glob pattern cannot be empty", ToolErrorType.MISSING_REQUIRED_PARAMETER)
        }
        
        if (params.maxResults <= 0) {
            throw ToolException("Max results must be positive", ToolErrorType.PARAMETER_OUT_OF_RANGE)
        }
        
        if (params.pattern.contains("..")) {
            throw ToolException("Path traversal not allowed in pattern: ${params.pattern}", ToolErrorType.PATH_INVALID)
        }
    }
}
