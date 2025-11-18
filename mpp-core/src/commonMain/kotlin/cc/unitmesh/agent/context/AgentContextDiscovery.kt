package cc.unitmesh.agent.context

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import kotlinx.io.files.Path

/**
 * Discovers and loads AGENTS.md files for providing custom context and instructions
 * 
 * Follows the AGENTS.md standard (https://agents.md/) initiated by OpenAI:
 * - Searches from git repository root to current working directory
 * - Supports multiple filename variants (AGENTS.md, CLAUDE.md, etc.)
 * - Concatenates files in hierarchical order (root → leaf)
 * - Respects byte limits to avoid context overflow
 * 
 * Implementation inspired by:
 * - Codex (https://github.com/anthropics/codex) - Rust implementation
 * - Gemini-CLI (https://github.com/google/gemini-cli) - TypeScript implementation
 */
class AgentContextDiscovery(
    private val fileSystem: ToolFileSystem,
    private val maxBytes: Int = DEFAULT_MAX_BYTES
) {
    private val logger = getLogger("AgentContextDiscovery")
    
    /**
     * Discover and load AGENTS.md files from project hierarchy
     * 
     * @param projectPath Working directory path
     * @param fallbackFilenames Additional filenames to search (e.g., CLAUDE.md)
     * @return Combined instructions from all discovered files, or empty string if none found
     */
    suspend fun loadAgentContext(
        projectPath: String,
        fallbackFilenames: List<String> = DEFAULT_FALLBACK_FILENAMES
    ): String {
        if (maxBytes == 0) {
            logger.debug { "AgentContext loading disabled (maxBytes = 0)" }
            return ""
        }
        
        val filePaths = discoverContextFiles(projectPath, fallbackFilenames)
        if (filePaths.isEmpty()) {
            logger.debug { "No agent context files found in project hierarchy" }
            return ""
        }
        
        logger.info { "Found ${filePaths.size} agent context files" }
        return readAndConcatenate(filePaths, projectPath)
    }
    
    /**
     * Discover context file paths in hierarchical order (git root → cwd)
     */
    private suspend fun discoverContextFiles(
        workingDir: String,
        fallbackFilenames: List<String>
    ): List<String> {
        val normalizedPath = normalizePath(workingDir)
        
        // Build directory chain from cwd upwards
        val dirChain = mutableListOf(normalizedPath)
        var gitRoot: String? = null
        var cursor = normalizedPath
        
        while (true) {
            val parent = getParentPath(cursor) ?: break
            if (parent == cursor) break // Reached root
            
            // Check for .git marker
            val gitMarker = joinPath(cursor, ".git")
            if (fileSystem.exists(gitMarker)) {
                gitRoot = cursor
                break
            }
            
            dirChain.add(parent)
            cursor = parent
        }
        
        // Determine search directories (git root → cwd)
        val searchDirs = if (gitRoot != null) {
            dirChain.reversed().dropWhile { it != gitRoot }
        } else {
            listOf(normalizedPath) // No git root found, only search cwd
        }
        
        logger.debug { "Searching ${searchDirs.size} directories for context files" }
        
        // Search for context files in each directory
        val candidateFilenames = buildCandidateFilenames(fallbackFilenames)
        val foundPaths = mutableListOf<String>()
        
        for (dir in searchDirs) {
            // Try each candidate filename in priority order
            for (filename in candidateFilenames) {
                val candidate = joinPath(dir, filename)
                if (fileSystem.exists(candidate)) {
                    val fileInfo = fileSystem.getFileInfo(candidate)
                    if (fileInfo != null && !fileInfo.isDirectory) {
                        foundPaths.add(candidate)
                        logger.debug { "Found context file: $candidate" }
                        break // Only one file per directory
                    }
                }
            }
        }
        
        return foundPaths
    }
    
    /**
     * Read and concatenate context files with byte limit
     */
    private suspend fun readAndConcatenate(
        filePaths: List<String>,
        workingDir: String
    ): String {
        var remainingBytes = maxBytes
        val parts = mutableListOf<String>()
        
        for (path in filePaths) {
            if (remainingBytes <= 0) {
                logger.warn { "Reached byte limit ($maxBytes), truncating remaining files" }
                break
            }
            
            try {
                val content = fileSystem.readFile(path)
                if (content == null) {
                    logger.warn { "File not found or unreadable: $path" }
                    continue
                }
                
                // Truncate if exceeds remaining bytes
                val actualContent = if (content.length > remainingBytes) {
                    content.substring(0, remainingBytes)
                } else {
                    content
                }
                
                val trimmed = actualContent.trim()
                
                if (trimmed.isNotEmpty()) {
                    // Format with context markers (similar to gemini-cli)
                    val relativePath = getRelativePath(workingDir, path)
                    val formatted = formatContextBlock(relativePath, trimmed)
                    parts.add(formatted)
                    
                    remainingBytes -= actualContent.length
                    logger.debug { "Loaded ${actualContent.length} bytes from $relativePath (remaining: $remainingBytes)" }
                }
            } catch (e: Exception) {
                logger.warn { "Failed to read context file $path: ${e.message}" }
            }
        }
        
        return if (parts.isEmpty()) {
            ""
        } else {
            parts.joinToString("\n\n")
        }
    }
    
    /**
     * Format context block with markers (similar to gemini-cli style)
     */
    private fun formatContextBlock(relativePath: String, content: String): String {
        return """
            |--- AGENTS.md from: $relativePath ---
            |$content
            |--- End of AGENTS.md from: $relativePath ---
        """.trimMargin()
    }
    
    /**
     * Build candidate filename list with priority:
     * 1. AGENTS.override.md (local override)
     * 2. AGENTS.md (standard)
     * 3. Fallback filenames (CLAUDE.md, etc.)
     */
    private fun buildCandidateFilenames(fallbackFilenames: List<String>): List<String> {
        val candidates = mutableListOf<String>()
        candidates.add(LOCAL_OVERRIDE_FILENAME)
        candidates.add(DEFAULT_FILENAME)
        
        // Add fallbacks, avoiding duplicates
        for (fallback in fallbackFilenames) {
            if (fallback.isNotBlank() && fallback !in candidates) {
                candidates.add(fallback)
            }
        }
        
        return candidates
    }
    
    // Path manipulation utilities using kotlinx.io.files.Path
    
    private fun normalizePath(path: String): String {
        return Path(path).toString()
    }
    
    private fun getParentPath(path: String): String? {
        val parent = Path(path).parent
        return parent?.toString()
    }
    
    private fun joinPath(parent: String, child: String): String {
        return Path(parent, child).toString()
    }
    
    private fun getRelativePath(base: String, target: String): String {
        return try {
            val basePath = Path(base)
            val targetPath = Path(target)
            
            // Simple relative path calculation
            val baseStr = basePath.toString().trimEnd('/')
            val targetStr = targetPath.toString()
            
            if (targetStr.startsWith(baseStr)) {
                targetStr.substring(baseStr.length).trimStart('/')
            } else {
                targetStr
            }
        } catch (e: Exception) {
            target
        }
    }
    
    companion object {
        // Default filename following AGENTS.md standard
        const val DEFAULT_FILENAME = "AGENTS.md"
        
        // Local override (takes precedence, not committed to git)
        const val LOCAL_OVERRIDE_FILENAME = "AGENTS.override.md"
        
        // Default max bytes (32KB like codex)
        const val DEFAULT_MAX_BYTES = 32 * 1024
        
        // Default fallback filenames for compatibility
        val DEFAULT_FALLBACK_FILENAMES = listOf(
            "CLAUDE.md",      // Claude Code compatibility
            ".agents.md",     // Hidden variant
            "GEMINI.md",      // Gemini CLI compatibility
        )
    }
}

