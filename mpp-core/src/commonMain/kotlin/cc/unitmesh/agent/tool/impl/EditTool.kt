package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.integer
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.agent.tool.tracking.ChangeType
import cc.unitmesh.agent.tool.tracking.FileChange
import cc.unitmesh.agent.tool.tracking.FileChangeTracker
import kotlinx.serialization.Serializable

/**
 * Parameters for the Edit tool (search and replace in files)
 */
@Serializable
data class EditFileParams(
    /**
     * The file path to edit (relative to project root or absolute)
     */
    val filePath: String,
    
    /**
     * The exact text to search for and replace
     */
    val oldString: String,
    
    /**
     * The new text to replace with
     */
    val newString: String,
    
    /**
     * Number of replacements expected (defaults to 1)
     * Use when you want to replace multiple occurrences
     */
    val expectedReplacements: Int = 1
)

object EditFileSchema : DeclarativeToolSchema(
    description = """Replaces text within a file. By default, replaces a single occurrence, but can replace multiple occurrences when expectedReplacements is specified. 
This tool requires providing significant context around the change to ensure precise targeting. 
Always use the read-file tool to examine the file's current content before attempting a text replacement.

Expectation for required parameters:
1. filePath MUST be a valid path; otherwise an error will be thrown.
2. oldString MUST be the exact literal text to replace (including all whitespace, indentation, newlines, and surrounding code etc.).
3. newString MUST be the exact literal text to replace oldString with (also including all whitespace, indentation, newlines, and surrounding code etc.). Ensure the resulting code is correct and idiomatic.
4. NEVER escape oldString or newString, that would break the exact literal text requirement.

**Important:** If ANY of the above are not satisfied, the tool will fail. 
CRITICAL for oldString: Must uniquely identify the instance to change. Include at least 3 lines of context BEFORE and AFTER the target text, matching whitespace and indentation precisely. 
If this string matches multiple locations and expectedReplacements is 1, or does not match exactly, the tool will fail.

**Multiple replacements:** Set expectedReplacements to the number of occurrences you want to replace. The tool will replace ALL occurrences that match oldString exactly. Ensure the number of replacements matches your expectation.""",
    properties = mapOf(
        "filePath" to string(
            description = "The file path to modify (relative to project root or absolute)",
            required = true
        ),
        "oldString" to string(
            description = "The exact literal text to replace. For single replacements (default), include at least 3 lines of context BEFORE and AFTER the target text, matching whitespace and indentation precisely. For multiple replacements, specify expectedReplacements parameter. If this string is not the exact literal text or does not match exactly, the tool will fail.",
            required = true
        ),
        "newString" to string(
            description = "The exact literal text to replace oldString with. Provide the EXACT text. Ensure the resulting code is correct and idiomatic.",
            required = true
        ),
        "expectedReplacements" to integer(
            description = "Number of replacements expected. Defaults to 1 if not specified. Use when you want to replace multiple occurrences.",
            required = false,
            default = 1,
            minimum = 1
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return """/$toolName filePath="src/main.kt" oldString="val x = 1" newString="val x = 2" expectedReplacements=1"""
    }
}

/**
 * Tool invocation for editing files (search and replace)
 */
class EditFileInvocation(
    params: EditFileParams,
    tool: EditFileTool,
    private val fileSystem: ToolFileSystem
) : BaseToolInvocation<EditFileParams, ToolResult>(params, tool) {
    
    override fun getDescription(): String {
        val oldSnippet = params.oldString.lines().first().take(30) + 
            if (params.oldString.length > 30) "..." else ""
        val newSnippet = params.newString.lines().first().take(30) + 
            if (params.newString.length > 30) "..." else ""
        
        return "Edit file: ${params.filePath} - Replace '$oldSnippet' with '$newSnippet' (${params.expectedReplacements} occurrence(s))"
    }
    
    override fun getToolLocations(): List<ToolLocation> = 
        listOf(ToolLocation(params.filePath, LocationType.FILE))
    
    override suspend fun execute(context: ToolExecutionContext): ToolResult {
        return ToolErrorUtils.safeExecute(ToolErrorType.FILE_NOT_FOUND) {
            // Check if file exists
            if (!fileSystem.exists(params.filePath)) {
                throw ToolException(
                    "File not found: ${params.filePath}. Cannot apply edit.",
                    ToolErrorType.FILE_NOT_FOUND
                )
            }
            
            // Read current file content
            val currentContent = fileSystem.readFile(params.filePath)
                ?: throw ToolException(
                    "Could not read file: ${params.filePath}",
                    ToolErrorType.FILE_NOT_FOUND
                )
            
            // Handle new file creation (oldString is empty)
            if (params.oldString.isEmpty()) {
                if (currentContent.isEmpty()) {
                    // Create new file
                    fileSystem.writeFile(params.filePath, params.newString, createDirectories = true)
                    return@safeExecute ToolResult.Success(
                        "Created new file: ${params.filePath} with provided content.",
                        mapOf(
                            "file_path" to params.filePath,
                            "operation" to "create",
                            "content_length" to params.newString.length.toString(),
                            "content_lines" to params.newString.lines().size.toString()
                        )
                    )
                } else {
                    throw ToolException(
                        "Failed to edit. Attempted to create a file that already exists: ${params.filePath}",
                        ToolErrorType.FILE_ACCESS_DENIED
                    )
                }
            }
            
            // Count occurrences
            val occurrences = countOccurrences(currentContent, params.oldString)
            
            // Validate occurrence count
            if (occurrences == 0) {
                throw ToolException(
                    "Failed to edit, 0 occurrences found for oldString in ${params.filePath}. No edits made. The exact text in oldString was not found. Ensure you're not escaping content incorrectly and check whitespace, indentation, and context. Use read-file tool to verify.",
                    ToolErrorType.INVALID_PARAMETERS
                )
            }
            
            if (occurrences != params.expectedReplacements) {
                val occurrenceTerm = if (params.expectedReplacements == 1) "occurrence" else "occurrences"
                throw ToolException(
                    "Failed to edit, expected ${params.expectedReplacements} $occurrenceTerm but found $occurrences for oldString in file: ${params.filePath}",
                    ToolErrorType.INVALID_PARAMETERS
                )
            }
            
            // Check if old and new strings are identical
            if (params.oldString == params.newString) {
                throw ToolException(
                    "No changes to apply. The oldString and newString are identical in file: ${params.filePath}",
                    ToolErrorType.INVALID_PARAMETERS
                )
            }
            
            // Perform replacement
            val newContent = currentContent.replace(params.oldString, params.newString)
            
            // Check if content actually changed
            if (currentContent == newContent) {
                throw ToolException(
                    "No changes to apply. The new content is identical to the current content in file: ${params.filePath}",
                    ToolErrorType.INVALID_PARAMETERS
                )
            }
            
            // Write the modified content back
            fileSystem.writeFile(params.filePath, newContent, createDirectories = true)
            
            // Track the file change
            FileChangeTracker.recordChange(
                FileChange(
                    filePath = params.filePath,
                    changeType = ChangeType.EDIT,
                    originalContent = currentContent,
                    newContent = newContent,
                    metadata = mapOf(
                        "tool" to "edit-file",
                        "replacements" to occurrences.toString()
                    )
                )
            )
            
            val metadata = mapOf(
                "file_path" to params.filePath,
                "operation" to "edit",
                "replacements" to occurrences.toString(),
                "old_content_length" to currentContent.length.toString(),
                "new_content_length" to newContent.length.toString(),
                "old_lines" to currentContent.lines().size.toString(),
                "new_lines" to newContent.lines().size.toString()
            )
            
            ToolResult.Success(
                "Successfully modified file: ${params.filePath} ($occurrences replacement(s)).",
                metadata
            )
        }
    }
    
    /**
     * Count occurrences of search string in content
     */
    private fun countOccurrences(content: String, searchString: String): Int {
        if (searchString.isEmpty()) return 0
        
        var count = 0
        var index = 0
        while (index < content.length) {
            val foundIndex = content.indexOf(searchString, index)
            if (foundIndex == -1) break
            count++
            index = foundIndex + searchString.length
        }
        return count
    }
}

/**
 * Tool for editing files using search and replace
 * Similar to Gemini CLI's Edit tool
 */
class EditFileTool(
    private val fileSystem: ToolFileSystem
) : BaseExecutableTool<EditFileParams, ToolResult>() {
    override val name: String = "edit-file"
    override val description: String = """
Replaces text within a file. By default, replaces a single occurrence, but can replace multiple occurrences when \`expected_replacements\` is specified. This tool requires providing significant context around the change to ensure precise targeting. Always use the ${'$'}{READ_FILE_TOOL_NAME} tool to examine the file's current content before attempting a text replacement.

      The user has the ability to modify the \`new_string\` content. If modified, this will be stated in the response.

Expectation for required parameters:
1. \`file_path\` MUST be an absolute path; otherwise an error will be thrown.
2. \`old_string\` MUST be the exact literal text to replace (including all whitespace, indentation, newlines, and surrounding code etc.).
3. \`new_string\` MUST be the exact literal text to replace \`old_string\` with (also including all whitespace, indentation, newlines, and surrounding code etc.). Ensure the resulting code is correct and idiomatic.
4. NEVER escape \`old_string\` or \`new_string\`, that would break the exact literal text requirement.
**Important:** If ANY of the above are not satisfied, the tool will fail. CRITICAL for \`old_string\`: Must uniquely identify the single instance to change. Include at least 3 lines of context BEFORE and AFTER the target text, matching whitespace and indentation precisely. If this string matches multiple locations, or does not match exactly, the tool will fail.
**Multiple replacements:** Set \`expected_replacements\` to the number of occurrences you want to replace. The tool will replace ALL occurrences that match \`old_string\` exactly. Ensure the number of replacements matches your expectation.
    """.trimIndent()
    
    override val metadata: ToolMetadata = ToolMetadata(
        displayName = "Edit File",
        tuiEmoji = "🔧",
        composeIcon = "edit_note",
        category = ToolCategory.FileSystem,
        schema = EditFileSchema
    )
    
    override fun getParameterClass(): String = EditFileParams::class.simpleName ?: "EditFileParams"
    
    override fun createToolInvocation(params: EditFileParams): ToolInvocation<EditFileParams, ToolResult> {
        validateParameters(params)
        return EditFileInvocation(params, this, fileSystem)
    }
    
    private fun validateParameters(params: EditFileParams) {
        if (params.filePath.isBlank()) {
            throw ToolException("File path cannot be empty", ToolErrorType.MISSING_REQUIRED_PARAMETER)
        }
        
        // Check for potentially dangerous paths
        if (params.filePath.contains("..")) {
            throw ToolException("Path traversal not allowed: ${params.filePath}", ToolErrorType.PATH_INVALID)
        }
        
        if (params.expectedReplacements < 1) {
            throw ToolException(
                "expectedReplacements must be at least 1, got: ${params.expectedReplacements}",
                ToolErrorType.PARAMETER_OUT_OF_RANGE
            )
        }
        
        // Allow empty oldString for file creation, but both oldString and newString can't be empty for edit
        if (params.oldString.isNotEmpty() && params.newString.isEmpty()) {
            // This is technically allowed (deletion), so no error
        }
    }
}

