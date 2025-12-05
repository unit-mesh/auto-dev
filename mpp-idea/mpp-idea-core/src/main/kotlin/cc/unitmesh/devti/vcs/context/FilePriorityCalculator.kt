package cc.unitmesh.devti.vcs.context

import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import java.nio.file.Path
import java.nio.file.PathMatcher

/**
 * Calculates file priority based on file type, size, and other characteristics.
 */
class FilePriorityCalculator(
    private val ignoreFilePatterns: List<PathMatcher> = emptyList()
) {
    
    companion object {
        /**
         * Critical source code file extensions
         */
        private val CRITICAL_EXTENSIONS = setOf(
            "kt", "java", "scala", "groovy",  // JVM languages
            "ts", "tsx", "js", "jsx", "vue",  // JavaScript/TypeScript
            "py", "pyi",                       // Python
            "go",                              // Go
            "rs",                              // Rust
            "c", "cpp", "cc", "h", "hpp",     // C/C++
            "cs",                              // C#
            "rb",                              // Ruby
            "php",                             // PHP
            "swift",                           // Swift
        )

        /**
         * High priority file extensions
         */
        private val HIGH_EXTENSIONS = setOf(
            "yaml", "yml", "toml", "properties", "conf", "config",  // Configuration
            "gradle", "kts", "xml", "pom",                          // Build files
            "sql", "graphql", "proto",                              // Schema/Query files
        )

        /**
         * Medium priority file extensions
         */
        private val MEDIUM_EXTENSIONS = setOf(
            "md", "adoc", "rst", "txt",        // Documentation
            "sh", "bash", "zsh", "fish",       // Shell scripts
            "Dockerfile", "docker-compose",    // Docker
            "Makefile",                        // Make
        )

        /**
         * Low priority file extensions
         */
        private val LOW_EXTENSIONS = setOf(
            "json", "jsonl",                   // Data files
            "csv", "tsv",                      // Tabular data
            "html", "htm", "css", "scss",      // Web assets
            "svg", "png", "jpg", "jpeg", "gif", "ico",  // Images
            "lock",                            // Lock files
        )

        /**
         * File patterns that should be excluded
         */
        private val EXCLUDED_PATTERNS = setOf(
            "node_modules", "target", "build", "dist", "out",  // Build outputs
            ".git", ".svn", ".hg",                             // VCS directories
            "vendor", "venv", ".venv",                         // Dependencies
        )
    }

    /**
     * Calculate priority for a single change
     */
    fun calculate(change: Change): PrioritizedChange? {
        val revision = change.afterRevision ?: change.beforeRevision ?: return null
        val filePath = revision.file.path
        
        // Check if file should be ignored
        if (shouldIgnore(filePath)) {
            return null
        }

        val fileExtension = getFileExtension(filePath)
        val fileSize = getFileSize(revision)
        
        // Determine base priority from extension
        val basePriority = when (fileExtension) {
            in CRITICAL_EXTENSIONS -> FilePriority.CRITICAL
            in HIGH_EXTENSIONS -> FilePriority.HIGH
            in MEDIUM_EXTENSIONS -> FilePriority.MEDIUM
            in LOW_EXTENSIONS -> FilePriority.LOW
            else -> FilePriority.MEDIUM  // Default to medium for unknown types
        }

        // Adjust priority based on file size
        val adjustedPriority = adjustPriorityBySize(basePriority, fileSize)

        return PrioritizedChange(
            change = change,
            priority = adjustedPriority,
            filePath = filePath,
            fileSize = fileSize,
            fileExtension = fileExtension
        )
    }

    /**
     * Calculate priorities for multiple changes and sort them
     */
    fun calculateAndSort(changes: List<Change>): List<PrioritizedChange> {
        return changes
            .mapNotNull { calculate(it) }
            .sorted()  // Uses PrioritizedChange.compareTo
    }

    /**
     * Check if file should be ignored based on patterns
     */
    private fun shouldIgnore(filePath: String): Boolean {
        // Check against ignore patterns
        if (ignoreFilePatterns.any { it.matches(Path.of(filePath)) }) {
            return true
        }

        // Check against excluded path patterns
        return EXCLUDED_PATTERNS.any { pattern ->
            filePath.contains("/$pattern/") || filePath.contains("\\$pattern\\")
        }
    }

    /**
     * Adjust priority based on file size
     */
    private fun adjustPriorityBySize(basePriority: FilePriority, fileSize: Long): FilePriority {
        if (fileSize > FilePriority.MAX_FILE_SIZE) {
            return FilePriority.EXCLUDED
        }

        return when (basePriority) {
            FilePriority.CRITICAL -> {
                if (fileSize > FilePriority.MAX_CRITICAL_SIZE) FilePriority.HIGH
                else FilePriority.CRITICAL
            }
            FilePriority.HIGH -> {
                if (fileSize > FilePriority.MAX_HIGH_SIZE) FilePriority.MEDIUM
                else FilePriority.HIGH
            }
            FilePriority.MEDIUM -> {
                if (fileSize > FilePriority.MAX_MEDIUM_SIZE) FilePriority.LOW
                else FilePriority.MEDIUM
            }
            else -> basePriority
        }
    }

    /**
     * Get file extension from path
     */
    private fun getFileExtension(filePath: String): String {
        val fileName = filePath.substringAfterLast('/')
        return if (fileName.contains('.')) {
            fileName.substringAfterLast('.')
        } else {
            fileName  // For files like Dockerfile, Makefile
        }
    }

    /**
     * Get file size from revision
     */
    private fun getFileSize(revision: com.intellij.openapi.vcs.changes.ContentRevision): Long {
        return try {
            val virtualFile = (revision as? CurrentContentRevision)?.virtualFile
            virtualFile?.length ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}

