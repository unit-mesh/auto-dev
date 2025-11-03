package cc.unitmesh.agent.tool.gitignore

/**
 * Cross-platform GitIgnore parser
 * Loads and parses .gitignore files from the file system
 */
expect class GitIgnoreParser(projectRoot: String) {
    /**
     * Check if a file path should be ignored
     * @param filePath Relative file path from project root
     * @return true if the file should be ignored
     */
    fun isIgnored(filePath: String): Boolean
    
    /**
     * Reload gitignore files from the file system
     */
    fun reload()
    
    /**
     * Get all loaded patterns
     */
    fun getPatterns(): List<String>
}

/**
 * GitIgnore file loader interface
 * Platform-specific implementations load .gitignore files
 */
interface GitIgnoreLoader {
    /**
     * Load .gitignore file content from a directory
     * @param dirPath Directory path to search for .gitignore
     * @return Content of .gitignore file, or null if not found
     */
    fun loadGitIgnoreFile(dirPath: String): String?
    
    /**
     * Check if a path is a directory
     */
    fun isDirectory(path: String): Boolean
    
    /**
     * List all directories in a path (for recursive .gitignore loading)
     */
    fun listDirectories(path: String): List<String>
    
    /**
     * Join path components
     */
    fun joinPath(vararg components: String): String
    
    /**
     * Get relative path from base to target
     */
    fun getRelativePath(base: String, target: String): String
}

/**
 * Base GitIgnore parser implementation
 * Uses GitIgnoreLoader for platform-specific file operations
 */
class BaseGitIgnoreParser(
    private val projectRoot: String,
    private val loader: GitIgnoreLoader
) {
    private val filters = mutableMapOf<String, GitIgnoreFilter>()
    private val globalFilter = DefaultGitIgnoreFilter()
    
    init {
        // Always ignore .git directory
        globalFilter.addPattern(".git")
        globalFilter.addPattern(".git/")
        reload()
    }
    
    fun reload() {
        filters.clear()
        loadGitIgnoreFiles(projectRoot)
    }
    
    private fun loadGitIgnoreFiles(dirPath: String) {
        // Load .gitignore in current directory
        val gitignoreContent = loader.loadGitIgnoreFile(dirPath)
        if (gitignoreContent != null) {
            val filter = parseGitIgnoreContent(gitignoreContent)
            filters[dirPath] = filter
        }
        
        // Load .git/info/exclude if at project root
        if (dirPath == projectRoot) {
            val excludePath = loader.joinPath(dirPath, ".git", "info", "exclude")
            val excludeContent = loader.loadGitIgnoreFile(excludePath.substringBeforeLast("/"))
            if (excludeContent != null) {
                excludeContent.lines().forEach { line ->
                    globalFilter.addPattern(line)
                }
            }
        }
        
        // Recursively load .gitignore files in subdirectories
        try {
            val subdirs = loader.listDirectories(dirPath)
            for (subdir in subdirs) {
                // Skip .git directory
                if (subdir.endsWith("/.git") || subdir.endsWith("\\.git")) {
                    continue
                }
                loadGitIgnoreFiles(subdir)
            }
        } catch (e: Exception) {
            // Ignore errors when listing directories
        }
    }
    
    fun isIgnored(filePath: String): Boolean {
        val normalizedPath = GitIgnorePatternMatcher.normalizePath(filePath)
        
        if (normalizedPath.isEmpty()) {
            return false
        }
        
        // Check global patterns first
        if (globalFilter.isIgnored(normalizedPath)) {
            return true
        }
        
        // Check patterns from .gitignore files in parent directories
        // Process from root to file location
        val pathParts = normalizedPath.split("/")
        var currentPath = projectRoot
        var relativePath = ""
        
        for (i in pathParts.indices) {
            if (i > 0) {
                relativePath = if (relativePath.isEmpty()) {
                    pathParts[i - 1]
                } else {
                    "$relativePath/${pathParts[i - 1]}"
                }
                currentPath = loader.joinPath(currentPath, pathParts[i - 1])
            }
            
            // Check if current directory has .gitignore
            val filter = filters[currentPath]
            if (filter != null) {
                // Build path relative to this .gitignore location
                val pathFromHere = if (i == 0) {
                    normalizedPath
                } else {
                    pathParts.subList(i, pathParts.size).joinToString("/")
                }
                
                if (filter.isIgnored(pathFromHere)) {
                    return true
                }
            }
        }
        
        return false
    }
    
    fun getPatterns(): List<String> {
        val allPatterns = mutableListOf<String>()
        allPatterns.addAll(globalFilter.getPatterns())
        filters.values.forEach { filter ->
            allPatterns.addAll(filter.getPatterns())
        }
        return allPatterns
    }
}

