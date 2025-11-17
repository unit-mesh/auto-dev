package cc.unitmesh.agent.tool.filesystem

import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolException
import kotlinx.datetime.Clock

/**
 * WASM-JS implementation of ToolFileSystem using in-memory file system
 * 
 * This implementation provides a Linux-like in-memory file system for WASM-JS environments.
 * All files and directories are stored in memory and will be lost when the page reloads.
 * 
 * Features:
 * - Full directory tree support with parent/child relationships
 * - File metadata (size, timestamps, permissions)
 * - Path normalization and resolution
 * - Pattern matching for file listing
 * - Recursive directory operations
 */
class WasmJsToolFileSystem(
    private val projectPath: String? = null
) : ToolFileSystem {
    
    // Root of the in-memory file system
    private val root = MemoryFSNode.Directory("/", mutableMapOf())
    
    init {
        // Create project directory if specified
        projectPath?.let {
            val normalizedPath = normalizePath(it)
            if (normalizedPath != "/") {
                try {
                    createDirectory(normalizedPath, createParents = true)
                } catch (e: Exception) {
                    println("Failed to create project directory: ${e.message}")
                }
            }
        }
    }
    
    override fun getProjectPath(): String? = projectPath
    
    override suspend fun readFile(path: String): String? {
        return try {
            val resolvedPath = resolvePath(path)
            val normalizedPath = normalizePath(resolvedPath)
            val node = findNode(normalizedPath)
            
            when (node) {
                is MemoryFSNode.File -> node.content
                is MemoryFSNode.Directory -> throw ToolException(
                    "Path is a directory: $path",
                    ToolErrorType.FILE_NOT_FOUND
                )
                null -> null
            }
        } catch (e: ToolException) {
            throw e
        } catch (e: Exception) {
            throw ToolException("Failed to read file: $path - ${e.message}", ToolErrorType.FILE_NOT_FOUND, e)
        }
    }
    
    override suspend fun writeFile(path: String, content: String, createDirectories: Boolean) {
        try {
            val resolvedPath = resolvePath(path)
            val normalizedPath = normalizePath(resolvedPath)
            
            if (createDirectories) {
                val parentPath = getParentPath(normalizedPath)
                if (parentPath != null && !exists(parentPath)) {
                    createDirectory(parentPath, createParents = true)
                }
            }
            
            val parentPath = getParentPath(normalizedPath) ?: "/"
            val fileName = getFileName(normalizedPath)
            val parentNode = findNode(parentPath) as? MemoryFSNode.Directory
                ?: throw ToolException("Parent directory not found: $parentPath", ToolErrorType.FILE_ACCESS_DENIED)
            
            val now = Clock.System.now().toEpochMilliseconds()
            val fileNode = MemoryFSNode.File(
                name = fileName,
                content = content,
                size = content.length.toLong(),
                lastModified = now,
                created = now
            )
            
            parentNode.children[fileName] = fileNode
        } catch (e: ToolException) {
            throw e
        } catch (e: Exception) {
            throw ToolException("Failed to write file: $path - ${e.message}", ToolErrorType.FILE_ACCESS_DENIED, e)
        }
    }
    
    override fun exists(path: String): Boolean {
        return try {
            val resolvedPath = resolvePath(path)
            val normalizedPath = normalizePath(resolvedPath)
            findNode(normalizedPath) != null
        } catch (e: Exception) {
            false
        }
    }
    
    override fun listFiles(path: String, pattern: String?): List<String> {
        return try {
            val resolvedPath = resolvePath(path)
            val normalizedPath = normalizePath(resolvedPath)
            val node = findNode(normalizedPath) as? MemoryFSNode.Directory ?: return emptyList()
            
            val files = node.children.filter { (_, child) -> child is MemoryFSNode.File }
            
            val result = if (pattern != null) {
                val regex = convertGlobToRegex(pattern)
                files.filter { (name, _) -> regex.matches(name) }
            } else {
                files
            }
            
            result.map { (name, _) -> 
                if (normalizedPath == "/") "/$name" else "$normalizedPath/$name"
            }.sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override fun resolvePath(relativePath: String): String {
        if (relativePath.startsWith("/")) {
            return relativePath
        }
        return if (projectPath != null) {
            "$projectPath/$relativePath"
        } else {
            "/$relativePath"
        }
    }
    
    override fun getFileInfo(path: String): FileInfo? {
        return try {
            val resolvedPath = resolvePath(path)
            val normalizedPath = normalizePath(resolvedPath)
            val node = findNode(normalizedPath) ?: return null
            
            when (node) {
                is MemoryFSNode.File -> FileInfo(
                    path = normalizedPath,
                    isDirectory = false,
                    size = node.size,
                    lastModified = node.lastModified,
                    isReadable = true,
                    isWritable = true
                )
                is MemoryFSNode.Directory -> FileInfo(
                    path = normalizedPath,
                    isDirectory = true,
                    size = 0L,
                    lastModified = node.lastModified,
                    isReadable = true,
                    isWritable = true
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun createDirectory(path: String, createParents: Boolean) {
        try {
            val resolvedPath = resolvePath(path)
            val normalizedPath = normalizePath(resolvedPath)
            
            if (normalizedPath == "/") return // Root already exists
            
            val segments = normalizedPath.split("/").filter { it.isNotEmpty() }
            var currentPath = ""
            var currentNode = root
            
            for (segment in segments) {
                currentPath = if (currentPath.isEmpty()) "/$segment" else "$currentPath/$segment"
                
                val existing = currentNode.children[segment]
                when {
                    existing is MemoryFSNode.Directory -> {
                        currentNode = existing
                    }
                    existing is MemoryFSNode.File -> {
                        throw ToolException(
                            "Cannot create directory: file exists at $currentPath",
                            ToolErrorType.FILE_ACCESS_DENIED
                        )
                    }
                    existing == null -> {
                        if (!createParents && currentPath != normalizedPath) {
                            throw ToolException(
                                "Parent directory does not exist: $currentPath",
                                ToolErrorType.DIRECTORY_NOT_FOUND
                            )
                        }
                        val newDir = MemoryFSNode.Directory(
                            name = segment,
                            children = mutableMapOf(),
                            lastModified = Clock.System.now().toEpochMilliseconds()
                        )
                        currentNode.children[segment] = newDir
                        currentNode = newDir
                    }
                }
            }
        } catch (e: ToolException) {
            throw e
        } catch (e: Exception) {
            throw ToolException("Failed to create directory: $path - ${e.message}", ToolErrorType.FILE_ACCESS_DENIED, e)
        }
    }
    
    override fun delete(path: String, recursive: Boolean) {
        try {
            val resolvedPath = resolvePath(path)
            val normalizedPath = normalizePath(resolvedPath)
            
            if (normalizedPath == "/") {
                throw ToolException("Cannot delete root directory", ToolErrorType.FILE_ACCESS_DENIED)
            }
            
            val parentPath = getParentPath(normalizedPath) ?: "/"
            val fileName = getFileName(normalizedPath)
            val parentNode = findNode(parentPath) as? MemoryFSNode.Directory
                ?: throw ToolException("Parent directory not found: $parentPath", ToolErrorType.DIRECTORY_NOT_FOUND)
            
            val node = parentNode.children[fileName]
                ?: throw ToolException("File or directory not found: $path", ToolErrorType.FILE_NOT_FOUND)
            
            if (node is MemoryFSNode.Directory && node.children.isNotEmpty() && !recursive) {
                throw ToolException("Directory not empty: $path", ToolErrorType.FILE_ACCESS_DENIED)
            }
            
            parentNode.children.remove(fileName)
        } catch (e: ToolException) {
            throw e
        } catch (e: Exception) {
            throw ToolException("Failed to delete: $path - ${e.message}", ToolErrorType.FILE_ACCESS_DENIED, e)
        }
    }
    
    // Private helper methods
    
    private fun findNode(path: String): MemoryFSNode? {
        val normalizedPath = normalizePath(path)
        if (normalizedPath == "/") return root
        
        val segments = normalizedPath.split("/").filter { it.isNotEmpty() }
        var currentNode: MemoryFSNode = root
        
        for (segment in segments) {
            currentNode = when (currentNode) {
                is MemoryFSNode.Directory -> currentNode.children[segment] ?: return null
                is MemoryFSNode.File -> return null
            }
        }
        
        return currentNode
    }
    
    private fun normalizePath(path: String): String {
        if (path.isEmpty()) return "/"
        
        val segments = mutableListOf<String>()
        for (segment in path.split("/")) {
            when (segment) {
                "", "." -> continue
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.size - 1)
                else -> segments.add(segment)
            }
        }
        
        return if (segments.isEmpty()) "/" else "/${segments.joinToString("/")}"
    }
    
    private fun getParentPath(path: String): String? {
        val normalizedPath = normalizePath(path)
        if (normalizedPath == "/") return null
        
        val lastSlash = normalizedPath.lastIndexOf('/')
        return if (lastSlash == 0) "/" else normalizedPath.substring(0, lastSlash)
    }
    
    private fun getFileName(path: String): String {
        val normalizedPath = normalizePath(path)
        return normalizedPath.substringAfterLast('/')
    }
    
    private fun convertGlobToRegex(pattern: String): Regex {
        val regexPattern = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return Regex("^$regexPattern$")
    }
}

/**
 * In-memory file system node (file or directory)
 */
private sealed class MemoryFSNode {
    abstract val name: String
    abstract val lastModified: Long
    
    data class File(
        override val name: String,
        val content: String,
        val size: Long,
        override val lastModified: Long,
        val created: Long
    ) : MemoryFSNode()
    
    data class Directory(
        override val name: String,
        val children: MutableMap<String, MemoryFSNode>,
        override val lastModified: Long = Clock.System.now().toEpochMilliseconds()
    ) : MemoryFSNode()
}
