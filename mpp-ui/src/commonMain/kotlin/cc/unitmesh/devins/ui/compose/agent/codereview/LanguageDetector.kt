package cc.unitmesh.devins.ui.compose.agent.codereview

fun detectLanguage(filePath: String): String? {
    return when (filePath.substringAfterLast('.', "")) {
        "kt" -> "kotlin"
        "java" -> "java"
        "js", "ts" -> "javascript"
        "py" -> "python"
        "go" -> "go"
        "rs" -> "rust"
        else -> null
    }
}
