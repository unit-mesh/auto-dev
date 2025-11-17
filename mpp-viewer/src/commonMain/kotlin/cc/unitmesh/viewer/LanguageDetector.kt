package cc.unitmesh.viewer

/**
 * Utility for detecting programming languages from file extensions
 */
object LanguageDetector {
    /**
     * Detect the programming language from a file path
     *
     * @param filePath The file path
     * @return The language identifier (monaco editor compatible), or "plaintext" if unknown
     */
    fun detectLanguage(filePath: String): String {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        val fileName = filePath.substringAfterLast('/').lowercase()

        return when {
            // Programming languages
            extension == "java" -> "java"
            extension in setOf("kt", "kts") -> "kotlin"
            extension == "js" -> "javascript"
            extension in setOf("ts", "tsx") -> "typescript"
            extension in setOf("jsx") -> "javascript"
            extension == "py" -> "python"
            extension == "rb" -> "ruby"
            extension == "php" -> "php"
            extension == "cs" -> "csharp"
            extension == "go" -> "go"
            extension == "rs" -> "rust"
            extension == "cpp" || extension == "cc" || extension == "cxx" || extension == "hpp" -> "cpp"
            extension == "c" || extension == "h" -> "c"
            extension == "swift" -> "swift"
            extension == "scala" -> "scala"
            extension == "r" -> "r"
            extension == "lua" -> "lua"
            extension == "pl" -> "perl"

            // Markup and data
            extension == "xml" -> "xml"
            extension in setOf("html", "htm") -> "html"
            extension == "css" || extension == "scss" || extension == "sass" || extension == "less" -> "css"
            extension == "json" -> "json"
            extension in setOf("yaml", "yml") -> "yaml"
            extension in setOf("md", "markdown") -> "markdown"
            extension == "toml" -> "toml"
            extension == "ini" || extension == "conf" -> "ini"
            extension == "properties" -> "properties"

            // Shell and scripts
            extension in setOf("sh", "bash", "zsh") -> "shell"
            extension == "ps1" -> "powershell"
            extension == "bat" || extension == "cmd" -> "bat"

            // Database
            extension == "sql" -> "sql"

            // Build and config
            extension == "gradle" -> "groovy"
            extension == "groovy" -> "groovy"
            fileName == "dockerfile" -> "dockerfile"
            fileName == "makefile" || fileName == "rakefile" -> "makefile"
            fileName == "cmakelists.txt" -> "cmake"

            // Version control
            fileName == ".gitignore" || fileName == ".gitattributes" -> "ignore"

            else -> "plaintext"
        }
    }

    /**
     * Check if a file is likely a binary file based on extension
     *
     * @param filePath The file path
     * @return true if the file is likely binary
     */
    fun isBinaryFile(filePath: String): Boolean {
        val extension = filePath.substringAfterLast('.', "").lowercase()

        val binaryExtensions = setOf(
            // Compiled/Archive
            "class", "jar", "war", "ear", "zip", "tar", "gz", "bz2", "7z", "rar",
            // Executables
            "exe", "dll", "so", "dylib", "bin", "app",
            // Images
            "png", "jpg", "jpeg", "gif", "bmp", "ico", "webp", "tiff", "svg",
            // Video
            "mp4", "avi", "mov", "mkv", "wmv", "flv", "webm",
            // Audio
            "mp3", "wav", "ogg", "flac", "aac", "wma",
            // Fonts
            "ttf", "otf", "woff", "woff2", "eot",
            // Database
            "db", "sqlite", "sqlite3",
            // Documents
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
        )

        return extension in binaryExtensions
    }
}

