package cc.unitmesh.devins.completion.providers

import cc.unitmesh.devins.completion.CompletionContext
import cc.unitmesh.devins.completion.CompletionItem
import cc.unitmesh.devins.completion.CompletionProvider
import cc.unitmesh.devins.completion.defaultInsertHandler

/**
 * 文件路径补全提供者（用于 /file:, /write: 等命令之后）
 */
class FilePathCompletionProvider : CompletionProvider {
    private val commonPaths = listOf(
        CompletionItem(
            text = "src/main/kotlin/",
            displayText = "src/main/kotlin/",
            description = "Kotlin source directory",
            icon = "📁",
            insertHandler = defaultInsertHandler("src/main/kotlin/")
        ),
        CompletionItem(
            text = "src/main/java/",
            displayText = "src/main/java/",
            description = "Java source directory",
            icon = "📁",
            insertHandler = defaultInsertHandler("src/main/java/")
        ),
        CompletionItem(
            text = "src/test/kotlin/",
            displayText = "src/test/kotlin/",
            description = "Kotlin test directory",
            icon = "📁",
            insertHandler = defaultInsertHandler("src/test/kotlin/")
        ),
        CompletionItem(
            text = "README.md",
            displayText = "README.md",
            description = "Project README",
            icon = "📝",
            insertHandler = defaultInsertHandler("README.md")
        ),
        CompletionItem(
            text = "build.gradle.kts",
            displayText = "build.gradle.kts",
            description = "Gradle build file",
            icon = "🔨",
            insertHandler = defaultInsertHandler("build.gradle.kts")
        )
    )

    override fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val query = context.queryText
        return commonPaths
            .filter { it.matchScore(query) > 0 }
            .sortedByDescending { it.matchScore(query) }
    }
}