package cc.unitmesh.agent.vcs.context

import cc.unitmesh.agent.logging.getLogger

/**
 * Compresses diff context to fit within token limits.
 * Implements smart strategies for prioritizing and truncating file diffs.
 */
class DiffContextCompressor(
    private val maxLinesPerFile: Int = FilePriority.MAX_LINES_PER_FILE,
    private val maxTotalLines: Int = 10000
) {
    private val logger = getLogger("DiffContextCompressor")

    companion object {
        /**
         * Critical source code file extensions (highest priority)
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
     * Compress a git diff patch to fit within context limits.
     * 
     * Strategy:
     * 1. Format the diff using DiffFormatter
     * 2. Split into individual file diffs
     * 3. Prioritize files by extension and change type
     * 4. Truncate large file diffs to maxLinesPerFile
     * 5. Include as many files as possible within maxTotalLines
     * 
     * @param patch The raw git diff patch
     * @return Compressed diff string
     */
    fun compress(patch: String): String {
        // First, format the diff to simplify it
        val formatted = DiffFormatter.postProcess(patch)
        
        // Split into file diffs
        val fileDiffs = splitIntoFileDiffs(formatted)
        
        logger.info { "Compressing ${fileDiffs.size} file diffs" }
        
        // Prioritize and compress
        val prioritizedDiffs = fileDiffs
            .map { PrioritizedFileDiff.from(it) }
            .sortedByDescending { it.priority.level }
        
        val result = StringBuilder()
        var totalLines = 0
        var includedFiles = 0
        var truncatedFiles = 0
        
        for (diff in prioritizedDiffs) {
            val lines = diff.content.lines()
            val lineCount = lines.size
            
            if (totalLines >= maxTotalLines) {
                logger.info { "Reached max total lines ($maxTotalLines), stopping" }
                break
            }
            
            val remainingLines = maxTotalLines - totalLines
            
            if (lineCount <= maxLinesPerFile && lineCount <= remainingLines) {
                // Include full diff
                result.appendLine(diff.content)
                result.appendLine()
                totalLines += lineCount
                includedFiles++
            } else if (remainingLines > 10) {
                // Truncate diff
                val allowedLines = minOf(maxLinesPerFile, remainingLines)
                val truncated = truncateFileDiff(diff, allowedLines)
                result.appendLine(truncated)
                result.appendLine()
                totalLines += allowedLines
                includedFiles++
                truncatedFiles++
            } else {
                // Skip this file, not enough room
                logger.info { "Skipping ${diff.filePath} (not enough room)" }
            }
        }
        
        logger.info {
            "Compressed diff: included $includedFiles files ($truncatedFiles truncated), $totalLines total lines"
        }
        
        // Add summary if files were truncated or excluded
        if (truncatedFiles > 0 || includedFiles < prioritizedDiffs.size) {
            result.appendLine()
            result.appendLine("<!-- Context Compression Summary -->")
            result.appendLine("<!-- Total files in diff: ${prioritizedDiffs.size} -->")
            result.appendLine("<!-- Files included: $includedFiles -->")
            result.appendLine("<!-- Files truncated: $truncatedFiles -->")
            result.appendLine("<!-- Files excluded: ${prioritizedDiffs.size - includedFiles} -->")
            result.appendLine("<!-- Total lines: $totalLines -->")
        }
        
        return result.toString().trim()
    }

    /**
     * Split a formatted diff into individual file diffs.
     */
    private fun splitIntoFileDiffs(formatted: String): List<String> {
        val fileDiffs = mutableListOf<String>()
        val lines = formatted.lines()
        var currentDiff = StringBuilder()
        
        for (line in lines) {
            // Check if this is a file boundary marker
            if (isFileBoundary(line) && currentDiff.isNotEmpty()) {
                fileDiffs.add(currentDiff.toString().trim())
                currentDiff = StringBuilder()
            }
            currentDiff.appendLine(line)
        }
        
        // Add last diff
        if (currentDiff.isNotEmpty()) {
            fileDiffs.add(currentDiff.toString().trim())
        }
        
        return fileDiffs
    }

    /**
     * Check if a line marks a file boundary.
     */
    private fun isFileBoundary(line: String): Boolean {
        return line.startsWith("---") || 
               line.startsWith("new file ") ||
               line.startsWith("delete file ") ||
               line.startsWith("rename file ") ||
               line.startsWith("modify file ")
    }

    /**
     * Truncate a file diff to the specified number of lines.
     * Preserves the file header and adds a truncation notice.
     */
    private fun truncateFileDiff(diff: PrioritizedFileDiff, maxLines: Int): String {
        val lines = diff.content.lines()
        
        if (lines.size <= maxLines) {
            return diff.content
        }
        
        // Keep header lines (first few lines with file info)
        val headerLines = lines.takeWhile { 
            it.startsWith("---") || it.startsWith("+++") || 
            it.startsWith("new file") || it.startsWith("delete file") ||
            it.startsWith("rename file") || it.startsWith("modify file") ||
            it.startsWith("@@")
        }
        
        val headerCount = headerLines.size
        val contentLines = maxLines - headerCount - 1 // -1 for truncation notice
        
        val truncated = StringBuilder()
        truncated.appendLine(headerLines.joinToString("\n"))
        
        // Add content lines
        val remainingLines = lines.drop(headerCount).take(contentLines)
        truncated.appendLine(remainingLines.joinToString("\n"))
        
        // Add truncation notice
        truncated.appendLine("... [truncated ${lines.size - maxLines} lines] ...")
        
        return truncated.toString().trim()
    }

    /**
     * Represents a file diff with priority metadata.
     */
    private data class PrioritizedFileDiff(
        val content: String,
        val filePath: String,
        val priority: FilePriority
    ) {
        companion object {
            fun from(diffContent: String): PrioritizedFileDiff {
                val filePath = extractFilePath(diffContent)
                val priority = calculatePriority(filePath)
                return PrioritizedFileDiff(diffContent, filePath, priority)
            }

            private fun extractFilePath(diffContent: String): String {
                // Try to extract file path from various markers
                val lines = diffContent.lines()
                for (line in lines) {
                    when {
                        line.startsWith("--- ") -> {
                            val path = line.substring(4).trim()
                            if (path.startsWith("a/")) {
                                return path.substring(2)
                            }
                            return path
                        }
                        line.startsWith("+++ ") -> {
                            val path = line.substring(4).trim()
                            if (path.startsWith("b/")) {
                                return path.substring(2)
                            }
                            return path
                        }
                        line.startsWith("new file ") -> {
                            return line.substring("new file ".length).trim()
                        }
                        line.startsWith("delete file ") -> {
                            return line.substring("delete file ".length).trim()
                        }
                        line.startsWith("modify file ") -> {
                            return line.substring("modify file ".length).trim()
                        }
                        line.startsWith("rename file from") -> {
                            // Extract the "to" part
                            val toIndex = line.indexOf(" to ")
                            if (toIndex > 0) {
                                return line.substring(toIndex + 4).trim()
                            }
                        }
                    }
                }
                return "unknown"
            }

            private fun calculatePriority(filePath: String): FilePriority {
                // Check if should be excluded
                if (shouldExclude(filePath)) {
                    return FilePriority.EXCLUDED
                }

                val extension = getFileExtension(filePath)
                
                return when (extension) {
                    in CRITICAL_EXTENSIONS -> FilePriority.CRITICAL
                    in HIGH_EXTENSIONS -> FilePriority.HIGH
                    in MEDIUM_EXTENSIONS -> FilePriority.MEDIUM
                    in LOW_EXTENSIONS -> FilePriority.LOW
                    else -> FilePriority.MEDIUM  // Default to medium for unknown types
                }
            }

            private fun shouldExclude(filePath: String): Boolean {
                return EXCLUDED_PATTERNS.any { pattern ->
                    filePath.contains("/$pattern/") || filePath.contains("\\$pattern\\")
                }
            }

            private fun getFileExtension(filePath: String): String {
                val fileName = filePath.substringAfterLast('/')
                return if (fileName.contains('.')) {
                    fileName.substringAfterLast('.')
                } else {
                    fileName  // For files like Dockerfile, Makefile
                }
            }
        }
    }
}
