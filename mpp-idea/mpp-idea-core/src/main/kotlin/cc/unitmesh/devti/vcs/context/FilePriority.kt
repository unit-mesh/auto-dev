package cc.unitmesh.devti.vcs.context

/**
 * File priority levels for context window management.
 * Higher priority files are more likely to be included in full detail.
 */
enum class FilePriority(val level: Int, val description: String) {
    /**
     * Critical files that should always be included with full diff.
     * Examples: Core source code files (kt, java, ts, js, py, go, rs)
     */
    CRITICAL(100, "Critical source code files"),

    /**
     * High priority files that are important but can be summarized if needed.
     * Examples: Configuration files (yaml, toml, gradle), test files
     */
    HIGH(75, "Important configuration and test files"),

    /**
     * Medium priority files that can be summarized.
     * Examples: Documentation (md), build scripts
     */
    MEDIUM(50, "Documentation and build files"),

    /**
     * Low priority files that should be summarized or excluded.
     * Examples: Data files (json, xml), generated files
     */
    LOW(25, "Data and generated files"),

    /**
     * Files that should be excluded from context.
     * Examples: Binary files, very large files, lock files
     */
    EXCLUDED(0, "Binary or excluded files");

    companion object {
        /**
         * Maximum file size in bytes for CRITICAL priority (500KB)
         */
        const val MAX_CRITICAL_SIZE = 500_000L

        /**
         * Maximum file size in bytes for HIGH priority (200KB)
         */
        const val MAX_HIGH_SIZE = 200_000L

        /**
         * Maximum file size in bytes for MEDIUM priority (100KB)
         */
        const val MAX_MEDIUM_SIZE = 100_000L

        /**
         * Maximum file size in bytes for any file (1MB)
         */
        const val MAX_FILE_SIZE = 1_000_000L
    }
}

