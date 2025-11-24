package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.agent.diff.FileChange
import cc.unitmesh.agent.diff.FileChangeTracker
import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.agent.tool.schema.ToolCategory
import cc.unitmesh.llm.KoogLLMService
import kotlinx.serialization.Serializable

/**
 * Parameters for the SmartEdit tool
 */
@Serializable
data class SmartEditParams(
    /**
     * The absolute path to the file to modify
     */
    val filePath: String,

    /**
     * The text to replace
     */
    val oldString: String,

    /**
     * The text to replace it with
     */
    val newString: String,

    /**
     * The instruction for what needs to be done.
     */
    val instruction: String
)

private const val SMART_EDIT_DESCRIPTION = """Replaces text within a file. Replaces a single occurrence. This tool requires providing significant context around the change to ensure precise targeting. Always use the read-file tool to examine the file's current content before attempting a text replacement.
      
      Expectation for required parameters:
      1. filePath MUST be an absolute path; otherwise an error will be thrown.
      2. oldString MUST be the exact literal text to replace (including all whitespace, indentation, newlines, and surrounding code etc.).
      3. newString MUST be the exact literal text to replace oldString with (also including all whitespace, indentation, newlines, and surrounding code etc.). Ensure the resulting code is correct and idiomatic and that oldString and newString are different.
      4. instruction is the detailed instruction of what needs to be changed. It is important to Make it specific and detailed so developers or large language models can understand what needs to be changed and perform the changes on their own if necessary. 
      5. NEVER escape oldString or newString, that would break the exact literal text requirement.
      **Important:** If ANY of the above are not satisfied, the tool will fail. CRITICAL for oldString: Must uniquely identify the single instance to change. Include at least 3 lines of context BEFORE and AFTER the target text, matching whitespace and indentation precisely. If this string matches multiple locations, or does not match exactly, the tool will fail.
      6. Prefer to break down complex and long changes into multiple smaller atomic calls to this tool. Always check the content of the file after changes or not finding a string to match.
      **Multiple replacements:** If there are multiple and ambiguous occurences of the oldString in the file, the tool will also fail."""

object SmartEditSchema : DeclarativeToolSchema(
    description = SMART_EDIT_DESCRIPTION,
    properties = mapOf(
        "filePath" to string(
            description = "The absolute path to the file to modify. Must start with '/'.",
            required = true
        ),
        "instruction" to string(
            description = "A clear, semantic instruction for the code change, acting as a high-quality prompt for an expert LLM assistant.",
            required = true
        ),
        "oldString" to string(
            description = "The exact literal text to replace, preferably unescaped. Include at least 3 lines of context BEFORE and AFTER the target text, matching whitespace and indentation precisely.",
            required = true
        ),
        "newString" to string(
            description = "The exact literal text to replace oldString with, preferably unescaped. Provide the EXACT text.",
            required = true
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return """/$toolName filePath="/path/to/file.kt" oldString="val x = 1" newString="val x = 2" instruction="Update x to 2""""
    }
}

class SmartEditInvocation(
    params: SmartEditParams,
    tool: SmartEditTool,
    private val fileSystem: ToolFileSystem,
    private val llmService: KoogLLMService?
) : BaseToolInvocation<SmartEditParams, ToolResult>(params, tool) {

    override fun getToolLocations(): List<ToolLocation> =
        listOf(ToolLocation(params.filePath, LocationType.FILE))

    override fun getDescription(): String {
        val oldSnippet = params.oldString.lines().first().take(30) +
                if (params.oldString.length > 30) "..." else ""
        val newSnippet = params.newString.lines().first().take(30) +
                if (params.newString.length > 30) "..." else ""

        if (params.oldString == params.newString) {
            return "No file changes to ${params.filePath}"
        }
        return "SmartEdit ${params.filePath}: '$oldSnippet' => '$newSnippet'"
    }

    override suspend fun execute(context: ToolExecutionContext): ToolResult {
        return try {
            val result = calculateEdit(params)
            
            if (result.error != null) {
                return ToolResult.Error(
                    message = result.error.raw,
                    errorType = result.error.type.name,
                    metadata = mapOf("display" to result.error.display)
                )
            }

            // Ensure parent directories exist
            // fileSystem.writeFile handles directory creation if createDirectories=true

            var finalContent = result.newContent
            if (!result.isNewFile && result.originalLineEnding == "\r\n") {
                finalContent = finalContent.replace("\n", "\r\n")
            }

            fileSystem.writeFile(params.filePath, finalContent, createDirectories = true)

            // Track change
            FileChangeTracker.recordChange(
                FileChange(
                    filePath = params.filePath,
                    changeType = if (result.isNewFile) ChangeType.CREATE else ChangeType.EDIT,
                    originalContent = result.currentContent ?: "",
                    newContent = finalContent,
                    metadata = mapOf(
                        "tool" to "smart-edit",
                        "replacements" to result.occurrences.toString()
                    )
                )
            )

            val successMessage = if (result.isNewFile) {
                "Created new file: ${params.filePath} with provided content."
            } else {
                "Successfully modified file: ${params.filePath} (${result.occurrences} replacements)."
            }

            ToolResult.Success(
                successMessage,
                mapOf(
                    "file_path" to params.filePath,
                    "occurrences" to result.occurrences.toString()
                )
            )

        } catch (e: Exception) {
            ToolResult.Error(
                message = "Error executing edit: ${e.message}",
                errorType = ToolErrorType.INTERNAL_ERROR.name
            )
        }
    }

    private data class ReplacementResult(
        val newContent: String,
        val occurrences: Int,
        val finalOldString: String,
        val finalNewString: String
    )

    private data class ToolError(
        val display: String,
        val raw: String,
        val type: ToolErrorType
    )

    private data class CalculatedEdit(
        val currentContent: String?,
        val newContent: String,
        val occurrences: Int,
        val error: ToolError? = null,
        val isNewFile: Boolean = false,
        val originalLineEnding: String = "\n"
    )

    private suspend fun calculateEdit(params: SmartEditParams): CalculatedEdit {
        val expectedReplacements = 1
        var currentContent: String? = null
        var fileExists = false
        var originalLineEnding = "\n"

        if (fileSystem.exists(params.filePath)) {
            currentContent = fileSystem.readFile(params.filePath)
            if (currentContent != null) {
                originalLineEnding = if (currentContent.contains("\r\n")) "\r\n" else "\n"
                currentContent = currentContent.replace("\r\n", "\n")
                fileExists = true
            }
        }

        val isNewFile = params.oldString.isEmpty() && !fileExists

        if (isNewFile) {
            return CalculatedEdit(
                currentContent = currentContent,
                newContent = params.newString,
                occurrences = 1,
                isNewFile = true,
                originalLineEnding = originalLineEnding
            )
        }

        if (!fileExists) {
            return CalculatedEdit(
                currentContent = currentContent,
                newContent = "",
                occurrences = 0,
                error = ToolError(
                    display = "File not found. Cannot apply edit. Use an empty oldString to create a new file.",
                    raw = "File not found: ${params.filePath}",
                    type = ToolErrorType.FILE_NOT_FOUND
                ),
                originalLineEnding = originalLineEnding
            )
        }

        if (currentContent == null) {
             return CalculatedEdit(
                currentContent = currentContent,
                newContent = "",
                occurrences = 0,
                error = ToolError(
                    display = "Failed to read content of file.",
                    raw = "Failed to read content of existing file: ${params.filePath}",
                    type = ToolErrorType.FILE_NOT_FOUND // Mapping READ_CONTENT_FAILURE to FILE_NOT_FOUND or similar
                ),
                originalLineEnding = originalLineEnding
            )
        }

        if (params.oldString.isEmpty()) {
            return CalculatedEdit(
                currentContent = currentContent,
                newContent = currentContent,
                occurrences = 0,
                error = ToolError(
                    display = "Failed to edit. Attempted to create a file that already exists.",
                    raw = "File already exists, cannot create: ${params.filePath}",
                    type = ToolErrorType.FILE_ACCESS_DENIED // Mapping ATTEMPT_TO_CREATE_EXISTING_FILE
                ),
                originalLineEnding = originalLineEnding
            )
        }

        val replacementResult = calculateReplacement(currentContent, params)
        
        val initialError = getErrorReplaceResult(
            params,
            replacementResult.occurrences,
            expectedReplacements,
            replacementResult.finalOldString,
            replacementResult.finalNewString
        )

        if (initialError == null) {
            return CalculatedEdit(
                currentContent = currentContent,
                newContent = replacementResult.newContent,
                occurrences = replacementResult.occurrences,
                isNewFile = false,
                originalLineEnding = originalLineEnding
            )
        }

        // Attempt self-correction
        // For now, we will skip the actual LLM call as it requires complex setup and prompt engineering porting
        // In a real implementation, we would call llmService here.
        // Returning the initial error for now.
        
        return CalculatedEdit(
            currentContent = currentContent,
            newContent = currentContent,
            occurrences = 0,
            error = initialError,
            isNewFile = false,
            originalLineEnding = originalLineEnding
        )
    }

    private fun calculateReplacement(currentContent: String, params: SmartEditParams): ReplacementResult {
        val normalizedSearch = params.oldString.replace("\r\n", "\n")
        val normalizedReplace = params.newString.replace("\r\n", "\n")

        if (normalizedSearch.isEmpty()) {
            return ReplacementResult(currentContent, 0, normalizedSearch, normalizedReplace)
        }

        val exactResult = calculateExactReplacement(currentContent, normalizedSearch, normalizedReplace)
        if (exactResult != null) return exactResult

        val flexibleResult = calculateFlexibleReplacement(currentContent, normalizedSearch, normalizedReplace)
        if (flexibleResult != null) return flexibleResult

        val regexResult = calculateRegexReplacement(currentContent, normalizedSearch, normalizedReplace)
        if (regexResult != null) return regexResult

        return ReplacementResult(currentContent, 0, normalizedSearch, normalizedReplace)
    }

    private fun calculateExactReplacement(content: String, search: String, replace: String): ReplacementResult? {
        val occurrences = content.split(search).size - 1
        if (occurrences > 0) {
            var modifiedCode = content.replace(search, replace)
            modifiedCode = restoreTrailingNewline(content, modifiedCode)
            return ReplacementResult(modifiedCode, occurrences, search, replace)
        }
        return null
    }

    private fun calculateFlexibleReplacement(content: String, search: String, replace: String): ReplacementResult? {
        val sourceLines = content.lines().toMutableList()
        if (content.endsWith("\n")) {
             // lines() splits "a\n" into ["a", ""], we want to ignore the last empty string if it's just a trailing newline effect
             // But actually lines() behavior: "a\n" -> ["a", ""]. 
             // TypeScript: content.match(/.*(?:\n|$)/g)?.slice(0, -1) ?? []
             // Let's stick to simple line splitting and re-joining.
             if (sourceLines.last().isEmpty()) sourceLines.removeAt(sourceLines.lastIndex)
        }
        
        val searchLines = search.lines().map { it.trim() }
        val replaceLines = replace.lines()

        var flexibleOccurrences = 0
        var i = 0
        while (i <= sourceLines.size - searchLines.size) {
            val window = sourceLines.subList(i, i + searchLines.size)
            val windowStripped = window.map { it.trim() }
            
            val isMatch = windowStripped.zip(searchLines).all { (w, s) -> w == s }

            if (isMatch) {
                flexibleOccurrences++
                val firstLineInMatch = window[0]
                val indentation = firstLineInMatch.takeWhile { it.isWhitespace() }
                
                val newBlockWithIndent = replaceLines.map { "$indentation$it" }
                
                // Replace lines in sourceLines
                // We need to remove searchLines.size lines and insert newBlockWithIndent lines
                // But since we are iterating, modifying the list is tricky.
                // However, we only need to do this once for single replacement usually, but loop supports multiple.
                // Let's assume we build a new list or modify in place carefully.
                // Since we are modifying, we should probably restart or adjust index.
                
                // For simplicity and safety (and since we expect 1 replacement), let's just do it.
                // But wait, removing multiple lines and inserting multiple lines changes indices.
                
                // Actually, let's just return the first match for now as per "single occurrence" requirement mostly.
                // But the loop implies multiple.
                
                // To support multiple, we should probably rebuild the list.
                
                // Re-implementation for correct list modification:
                // It's easier to find all matches first, then apply from end to start to avoid index shift issues.
                // But here we can just do one replacement and return if we only expect 1.
                
                // Let's stick to finding one match for now as SmartEdit usually targets one.
                // If we want to support multiple, we need to be careful.
                
                // Let's just do the first one and return.
                val newLines = ArrayList(sourceLines)
                for (k in 0 until searchLines.size) {
                    newLines.removeAt(i)
                }
                newLines.addAll(i, newBlockWithIndent)
                
                val modifiedCode = newLines.joinToString("\n")
                val finalCode = restoreTrailingNewline(content, modifiedCode)
                return ReplacementResult(finalCode, 1, search, replace)
            } else {
                i++
            }
        }
        return null
    }

    private fun calculateRegexReplacement(content: String, search: String, replace: String): ReplacementResult? {
        // Porting the regex logic
        val delimiters = listOf("(", ")", ":", "[", "]", "{", "}", ">", "<", "=")
        var processedString = search
        for (delim in delimiters) {
            processedString = processedString.replace(delim, " $delim ")
        }
        
        val tokens = processedString.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        
        val escapedTokens = tokens.map { Regex.escape(it) }
        val pattern = escapedTokens.joinToString("\\s*")
        val finalPattern = "^(\\s*)$pattern"
        
        val regex = Regex(finalPattern, RegexOption.MULTILINE)
        val match = regex.find(content) ?: return null
        
        val indentation = match.groupValues[1]
        val newLines = replace.lines()
        val newBlockWithIndent = newLines.joinToString("\n") { "$indentation$it" }
        
        // Replace only the first occurrence
        val modifiedCode = content.replaceFirst(regex.pattern, newBlockWithIndent) // This might not work directly with Regex object pattern string if it has flags
        
        // Kotlin's replaceFirst with Regex
        val modifiedCode2 = regex.replaceFirst(content, newBlockWithIndent)
        
        return ReplacementResult(restoreTrailingNewline(content, modifiedCode2), 1, search, replace)
    }

    private fun restoreTrailingNewline(original: String, modified: String): String {
        val hadTrailing = original.endsWith("\n")
        val hasTrailing = modified.endsWith("\n")
        return if (hadTrailing && !hasTrailing) {
            modified + "\n"
        } else if (!hadTrailing && hasTrailing) {
            modified.dropLast(1)
        } else {
            modified
        }
    }

    private fun getErrorReplaceResult(
        params: SmartEditParams,
        occurrences: Int,
        expectedReplacements: Int,
        finalOldString: String,
        finalNewString: String
    ): ToolError? {
        if (occurrences == 0) {
            return ToolError(
                display = "Failed to edit, could not find the string to replace.",
                raw = "Failed to edit, 0 occurrences found for oldString ($finalOldString). Original oldString was (${params.oldString}) in ${params.filePath}. No edits made. The exact text in oldString was not found. Ensure you're not escaping content incorrectly and check whitespace, indentation, and context. Use read-file tool to verify.",
                type = ToolErrorType.INVALID_PARAMETERS // Mapping EDIT_NO_OCCURRENCE_FOUND
            )
        } else if (occurrences != expectedReplacements) {
            val occurrenceTerm = if (expectedReplacements == 1) "occurrence" else "occurrences"
            return ToolError(
                display = "Failed to edit, expected $expectedReplacements $occurrenceTerm but found $occurrences.",
                raw = "Failed to edit, Expected $expectedReplacements $occurrenceTerm but found $occurrences for oldString in file: ${params.filePath}",
                type = ToolErrorType.INVALID_PARAMETERS // Mapping EDIT_EXPECTED_OCCURRENCE_MISMATCH
            )
        } else if (finalOldString == finalNewString) {
            return ToolError(
                display = "No changes to apply. The oldString and newString are identical.",
                raw = "No changes to apply. The oldString and newString are identical in file: ${params.filePath}",
                type = ToolErrorType.INVALID_PARAMETERS // Mapping EDIT_NO_CHANGE
            )
        }
        return null
    }
}

class SmartEditTool(
    private val fileSystem: ToolFileSystem,
    private val llmService: KoogLLMService?
) : BaseExecutableTool<SmartEditParams, ToolResult>() {
    override val name: String = "smart-edit"
    override val description: String = SMART_EDIT_DESCRIPTION
    override val metadata: ToolMetadata = ToolMetadata(
        displayName = "Smart Edit",
        tuiEmoji = "🧠",
        composeIcon = "auto_fix",
        category = ToolCategory.FileSystem,
        schema = SmartEditSchema
    )

    override fun getParameterClass(): String = SmartEditParams::class.simpleName ?: "SmartEditParams"

    override fun createToolInvocation(params: SmartEditParams): ToolInvocation<SmartEditParams, ToolResult> {
        validateParameters(params)
        return SmartEditInvocation(params, this, fileSystem, llmService)
    }

    private fun validateParameters(params: SmartEditParams) {
        if (params.filePath.isBlank()) {
            throw ToolException("File path cannot be empty", ToolErrorType.MISSING_REQUIRED_PARAMETER)
        }
        if (params.filePath.contains("..")) {
            throw ToolException("Path traversal not allowed: ${params.filePath}", ToolErrorType.PATH_INVALID)
        }
    }
}
