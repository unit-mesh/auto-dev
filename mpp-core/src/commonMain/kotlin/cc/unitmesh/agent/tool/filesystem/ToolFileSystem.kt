package cc.unitmesh.agent.tool.filesystem

import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolException
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.io.writeString

/**
 * Cross-platform file system interface for tools using kotlinx-io
 */
interface ToolFileSystem {
    /**
     * Get the project root path
     */
    fun getProjectPath(): String?
    
    /**
     * Read file content
     * @param path File path (relative to project root or absolute path)
     * @return File content, null if file doesn't exist
     */
    suspend fun readFile(path: String): String?
    
    /**
     * Write content to file
     * @param path File path
     * @param content Content to write
     * @param createDirectories Whether to create parent directories if they don't exist
     */
    suspend fun writeFile(path: String, content: String, createDirectories: Boolean = true)
    
    /**
     * Check if file or directory exists
     * @param path File or directory path
     */
    fun exists(path: String): Boolean
    
    /**
     * List files in directory
     * @param path Directory path
     * @param pattern File name pattern (supports wildcards like "*.md")
     * @return List of matching file paths
     */
    fun listFiles(path: String, pattern: String? = null): List<String>
    
    /**
     * Resolve relative path to absolute path
     * @param relativePath Path relative to project root
     * @return Absolute path
     */
    fun resolvePath(relativePath: String): String
    
    /**
     * Get file metadata
     */
    fun getFileInfo(path: String): FileInfo?
    
    /**
     * Create directory
     */
    fun createDirectory(path: String, createParents: Boolean = true)
    
    /**
     * Delete file or directory
     */
    fun delete(path: String, recursive: Boolean = false)
}

/**
 * File metadata information
 */
data class FileInfo(
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long? = null,
    val isReadable: Boolean = true,
    val isWritable: Boolean = true
)

/**
 * Default implementation using kotlinx-io
 */
class DefaultToolFileSystem(
    private val projectPath: String? = null,
    private val fileSystem: FileSystem = SystemFileSystem
) : ToolFileSystem {
    
    override fun getProjectPath(): String? = projectPath
    
    override suspend fun readFile(path: String): String? {
        return try {
            val resolvedPath = Path(resolvePath(path))
            if (!fileSystem.exists(resolvedPath)) {
                return null
            }
            fileSystem.source(resolvedPath).buffered().use { source ->
                source.readString()
            }
        } catch (e: Exception) {
            throw ToolException("Failed to read file: ${e.message}", ToolErrorType.FILE_NOT_FOUND, e)
        }
    }
    
    override suspend fun writeFile(path: String, content: String, createDirectories: Boolean) {
        try {
            val resolvedPath = Path(resolvePath(path))

            if (createDirectories) {
                val parentPath = resolvedPath.parent
                if (parentPath != null && !fileSystem.exists(parentPath)) {
                    fileSystem.createDirectories(parentPath)
                }
            }

            fileSystem.sink(resolvedPath).buffered().use { sink ->
                sink.writeString(content)
            }
        } catch (e: Exception) {
            throw ToolException("Failed to write file: ${e.message}", ToolErrorType.FILE_ACCESS_DENIED, e)
        }
    }
    
    override fun exists(path: String): Boolean {
        return try {
            val resolvedPath = Path(resolvePath(path))
            fileSystem.exists(resolvedPath)
        } catch (e: Exception) {
            false
        }
    }
    
    override fun listFiles(path: String, pattern: String?): List<String> {
        return try {
            val resolvedPath = Path(resolvePath(path))
            if (!fileSystem.exists(resolvedPath)) {
                return emptyList()
            }
            
            val files = mutableListOf<String>()
            fileSystem.list(resolvedPath).forEach { childPath ->
                val fileName = childPath.name
                if (pattern == null || matchesPattern(fileName, pattern)) {
                    files.add(childPath.toString())
                }
            }
            files.sorted()
        } catch (e: Exception) {
            throw ToolException("Failed to list files: ${e.message}", ToolErrorType.DIRECTORY_NOT_FOUND, e)
        }
    }
    
    override fun resolvePath(relativePath: String): String {
        return if (relativePath.startsWith("/") || relativePath.contains(":")) {
            // Already absolute path
            relativePath
        } else {
            // Relative path
            projectPath?.let { 
                Path(it, relativePath).toString()
            } ?: relativePath
        }
    }
    
    override fun getFileInfo(path: String): FileInfo? {
        return try {
            val resolvedPath = Path(resolvePath(path))
            if (!fileSystem.exists(resolvedPath)) {
                return null
            }
            
            val metadata = fileSystem.metadataOrNull(resolvedPath) ?: return null
            
            FileInfo(
                path = resolvedPath.toString(),
                isDirectory = metadata.isDirectory,
                size = metadata.size ?: 0L,
                lastModified = null, // kotlinx-io doesn't provide modification time yet
                isReadable = true, // Assume readable for now
                isWritable = true  // Assume writable for now
            )
        } catch (e: Exception) {
            null
        }
    }
    
    override fun createDirectory(path: String, createParents: Boolean) {
        try {
            val resolvedPath = Path(resolvePath(path))
            if (createParents) {
                fileSystem.createDirectories(resolvedPath)
            } else {
                fileSystem.createDirectories(resolvedPath, mustCreate = true)
            }
        } catch (e: Exception) {
            throw ToolException("Failed to create directory: ${e.message}", ToolErrorType.FILE_ACCESS_DENIED, e)
        }
    }

    override fun delete(path: String, recursive: Boolean) {
        try {
            val resolvedPath = Path(resolvePath(path))
            // kotlinx-io doesn't have deleteRecursively, so we just use delete
            fileSystem.delete(resolvedPath)
        } catch (e: Exception) {
            throw ToolException("Failed to delete file: ${e.message}", ToolErrorType.FILE_ACCESS_DENIED, e)
        }
    }
    
    /**
     * Simple pattern matching for file names
     * Supports basic wildcards: * and ?
     */
    private fun matchesPattern(fileName: String, pattern: String): Boolean {
        if (pattern == "*") return true
        
        // Convert glob pattern to regex
        val regexPattern = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        
        return fileName.matches(Regex(regexPattern))
    }
}

/**
 * Empty file system implementation for testing
 */
class EmptyToolFileSystem : ToolFileSystem {
    override fun getProjectPath(): String? = null
    override suspend fun readFile(path: String): String? = null
    override suspend fun writeFile(path: String, content: String, createDirectories: Boolean) {}
    override fun exists(path: String): Boolean = false
    override fun listFiles(path: String, pattern: String?): List<String> = emptyList()
    override fun resolvePath(relativePath: String): String = relativePath
    override fun getFileInfo(path: String): FileInfo? = null
    override fun createDirectory(path: String, createParents: Boolean) {}
    override fun delete(path: String, recursive: Boolean) {}
}
