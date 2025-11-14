package cc.unitmesh.devins.ui.compose.agent.codereview

fun detectLanguage(path: String): String? {
    val extension = path.substringAfterLast('.', "")
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
