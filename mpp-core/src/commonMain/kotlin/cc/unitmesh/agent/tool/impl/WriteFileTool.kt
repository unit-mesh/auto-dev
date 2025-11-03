package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import kotlinx.serialization.Serializable

/**
 * Parameters for the WriteFile tool
 */
@Serializable
data class WriteFileParams(
    /**
     * The file path to write to (relative to project root or absolute)
     */
    val path: String,
    
    /**
     * The content to write to the file
     */
    val content: String,
    
    /**
     * Whether to create parent directories if they don't exist
     */
    val createDirectories: Boolean = true,
    
    /**
     * Whether to overwrite existing file (default: true)
     */
    val overwrite: Boolean = true,
    
    /**
     * Whether to append to existing file instead of overwriting
     */
    val append: Boolean = false
)

/**
 * Tool invocation for writing files
 */
class WriteFileInvocation(
    params: WriteFileParams,
    tool: WriteFileTool,
    private val fileSystem: ToolFileSystem
) : BaseToolInvocation<WriteFileParams, ToolResult>(params, tool) {
    
    override fun getDescription(): String {
        val action = when {
            params.append -> "Append to"
            fileSystem.exists(params.path) -> "Overwrite"
            else -> "Create"
        }
        val size = params.content.length
        val lines = params.content.lines().size
        return "$action file: ${params.path} ($size chars, $lines lines)"
    }
    
    override fun getToolLocations(): List<ToolLocation> {
        return listOf(ToolLocation(params.path, LocationType.FILE))
    }
    
    override suspend fun execute(context: ToolExecutionContext): ToolResult {
        return ToolErrorUtils.safeExecute(ToolErrorType.FILE_ACCESS_DENIED) {
            val fileExists = fileSystem.exists(params.path)
            
            // Check if we should overwrite
            if (fileExists && !params.overwrite && !params.append) {
                throw ToolException(
                    "File already exists and overwrite is disabled: ${params.path}",
                    ToolErrorType.FILE_ACCESS_DENIED
                )
            }
            
            // Prepare content to write
            val contentToWrite = if (params.append && fileExists) {
                val existingContent = fileSystem.readFile(params.path) ?: ""
                existingContent + params.content
            } else {
                params.content
            }
            
            // Write the file
            fileSystem.writeFile(params.path, contentToWrite, params.createDirectories)
            
            // Get file info for metadata
            val fileInfo = fileSystem.getFileInfo(params.path)
            val metadata = mutableMapOf<String, String>().apply {
                put("file_path", params.path)
                put("content_length", params.content.length.toString())
                put("content_lines", params.content.lines().size.toString())
                put("operation", if (params.append) "append" else if (fileExists) "overwrite" else "create")
                put("created_directories", params.createDirectories.toString())
                
                fileInfo?.let { info ->
                    put("final_file_size", info.size.toString())
                    put("is_directory", info.isDirectory.toString())
                }
            }
            
            val resultMessage = buildString {
                append("Successfully ")
                append(if (params.append) "appended to" else if (fileExists) "overwrote" else "created")
                append(" file: ${params.path}")
                append(" (${params.content.length} chars, ${params.content.lines().size} lines)")
            }
            
            ToolResult.Success(resultMessage, metadata)
        }
    }
}

/**
 * Tool for writing content to files
 */
class WriteFileTool(
    private val fileSystem: ToolFileSystem
) : BaseExecutableTool<WriteFileParams, ToolResult>() {
    override val name: String = ToolNames.WRITE_FILE
    override val description: String = """
        Create new files or write content to existing files using the provided content.
        Supports creating parent directories automatically and can append to existing files.
        Use for new file creation, content replacement, or appending to existing files.
        Always verify file existence with read-file first if needed.
    """.trimIndent()
    
    override fun getParameterClass(): String = WriteFileParams::class.simpleName ?: "WriteFileParams"
    
    override fun createToolInvocation(params: WriteFileParams): ToolInvocation<WriteFileParams, ToolResult> {
        // Validate parameters
        validateParameters(params)
        return WriteFileInvocation(params, this, fileSystem)
    }
    
    private fun validateParameters(params: WriteFileParams) {
        if (params.path.isBlank()) {
            throw ToolException("File path cannot be empty", ToolErrorType.MISSING_REQUIRED_PARAMETER)
        }

        // Check for potentially dangerous paths
        if (params.path.contains("..")) {
            throw ToolException("Path traversal not allowed: ${params.path}", ToolErrorType.PATH_INVALID)
        }

        // Validate that append and overwrite are not both false when file exists
        if (fileSystem.exists(params.path) && !params.overwrite && !params.append) {
            throw ToolException(
                "File exists but both overwrite and append are disabled: ${params.path}",
                ToolErrorType.FILE_ACCESS_DENIED
            )
        }

        // Check content size (optional limit)
        val maxContentSize = 10 * 1024 * 1024 // 10MB limit
        if (params.content.length > maxContentSize) {
            throw ToolException(
                "Content too large: ${params.content.length} bytes (max: $maxContentSize bytes)",
                ToolErrorType.FILE_TOO_LARGE
            )
        }
    }
}
