package cc.unitmesh.agent.language

/**
 * Language detection utility
 */
object LanguageDetector {
    fun detectLanguage(filePath: String): String? {
        val extension = filePath.substringAfterLast('.', "")
        return when (extension.lowercase()) {
            "kt", "kts" -> "Kotlin"
            "java" -> "Java"
            "js", "jsx" -> "JavaScript"
            "ts", "tsx" -> "TypeScript"
            "py" -> "Python"
            "rs" -> "Rust"
            "go" -> "Go"
            "swift" -> "Swift"
            "c", "h" -> "C"
            "cpp", "cc", "cxx", "hpp" -> "C++"
            "cs" -> "C#"
            "rb" -> "Ruby"
            "php" -> "PHP"
            "html", "htm" -> "HTML"
            "css", "scss", "sass" -> "CSS"
            "json" -> "JSON"
            "xml" -> "XML"
            "yaml", "yml" -> "YAML"
            "md" -> "Markdown"
            "sh", "bash" -> "Shell"
            "sql" -> "SQL"
            else -> null
        }
    }

    fun getLinterNamesForLanguage(language: String): List<String> {
        return when (language) {
            "Kotlin" -> listOf("detekt")
            "Java" -> listOf("pmd")
            "JavaScript", "TypeScript" -> listOf("biome", "oxlint")
            "Python" -> listOf("ruff", "pylint", "flake8")
            "Rust" -> listOf("clippy")
            "Go" -> listOf("golangci-lint")
            "Ruby" -> listOf("rubocop", "brakeman")
            "PHP" -> listOf("phpstan", "phpmd", "phpcs")
            "Shell" -> listOf("shellcheck")
            "Markdown" -> listOf("markdownlint")
            "YAML" -> listOf("yamllint")
            "Docker" -> listOf("hadolint")
            "SQL" -> listOf("sqlfluff")
            "Swift" -> listOf("swiftlint")
            "HTML" -> listOf("htmlhint")
            "CSS" -> listOf("biome")
            else -> emptyList()
        }
    }
}