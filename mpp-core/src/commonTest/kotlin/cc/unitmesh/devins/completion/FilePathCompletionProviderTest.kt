package cc.unitmesh.devins.completion

import cc.unitmesh.devins.completion.providers.FilePathCompletionProvider
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.workspace.DefaultWorkspace
import cc.unitmesh.devins.workspace.WorkspaceManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class FilePathCompletionProviderTest {

    private class MockFileSystem(private val searchResults: List<String> = emptyList()) : ProjectFileSystem {
        override fun getProjectPath(): String = "/test/project"
        override fun readFile(path: String): String? = null
        override fun exists(path: String): Boolean = false
        override fun isDirectory(path: String): Boolean = false
        override fun listFiles(path: String, pattern: String?): List<String> = emptyList()
        override fun searchFiles(pattern: String, maxDepth: Int, maxResults: Int): List<String> {
            return searchResults
        }
        override fun resolvePath(relativePath: String): String = relativePath
    }

    @Test
    fun testStaticCompletionsWhenQueryIsEmpty() = runTest {
        // 创建一个临时的 workspace 用于测试
        val originalWorkspace = WorkspaceManager.currentWorkspace
        try {
            WorkspaceManager.openWorkspace("TestProject", "/test/project")

            val provider = FilePathCompletionProvider()
            val context = CompletionContext(
                fullText = "/file:",
                cursorPosition = 6,
                triggerType = CompletionTriggerType.COMMAND_VALUE,
                triggerOffset = 6,
                queryText = ""
            )

            val completions = provider.getCompletions(context)

            // 应该包含静态常用文件（不包含目录）
            assertTrue(completions.any { it.text == "README.md" || it.text == "build.gradle.kts" }, 
                "Should contain static common files like README.md or build.gradle.kts")
        } finally {
            // 恢复原来的 workspace
            WorkspaceManager.closeCurrentWorkspace()
            if (originalWorkspace != null) {
                WorkspaceManager.openWorkspace(originalWorkspace.name, originalWorkspace.rootPath ?: "")
            }
        }
    }

    @Test
    fun testFileSearchWithQuery() = runTest {
        // 由于我们使用的是真实的文件系统，这个测试在没有实际文件时会返回空结果
        // 这里我们只测试代码不会崩溃，并且返回结果的格式正确
        val originalWorkspace = WorkspaceManager.currentWorkspace
        try {
            WorkspaceManager.openWorkspace("TestProject", "/test/project")

            val provider = FilePathCompletionProvider()
            val context = CompletionContext(
                fullText = "/file:controller",
                cursorPosition = 16,
                triggerType = CompletionTriggerType.COMMAND_VALUE,
                triggerOffset = 6,
                queryText = "controller"
            )

            val completions = provider.getCompletions(context)

            // 验证补全不包含目录（所有项都应该是文件）
            assertTrue(completions.all { it.description?.startsWith("File:") ?: true }, 
                "Should only contain files, not directories")
        } finally {
            WorkspaceManager.closeCurrentWorkspace()
            if (originalWorkspace != null) {
                WorkspaceManager.openWorkspace(originalWorkspace.name, originalWorkspace.rootPath ?: "")
            }
        }
    }

    @Test
    fun testCompletionItemFormat() = runTest {
        val originalWorkspace = WorkspaceManager.currentWorkspace
        try {
            WorkspaceManager.openWorkspace("TestProject", "/test/project")

            val provider = FilePathCompletionProvider()
            val context = CompletionContext(
                fullText = "/file:read",
                cursorPosition = 11,
                triggerType = CompletionTriggerType.COMMAND_VALUE,
                triggerOffset = 6,
                queryText = "read"
            )

            val completions = provider.getCompletions(context)

            // 验证所有补全项都有正确的格式
            completions.forEach { completion ->
                assertTrue(completion.text.isNotEmpty(), "Completion text should not be empty")
                assertTrue(completion.displayText.isNotEmpty(), "Display text should not be empty")
                assertTrue(completion.insertHandler != null, "Insert handler should not be null")
            }
        } finally {
            WorkspaceManager.closeCurrentWorkspace()
            if (originalWorkspace != null) {
                WorkspaceManager.openWorkspace(originalWorkspace.name, originalWorkspace.rootPath ?: "")
            }
        }
    }

    @Test
    fun testShortQueryReturnsStaticPaths() = runTest {
        val originalWorkspace = WorkspaceManager.currentWorkspace
        try {
            WorkspaceManager.openWorkspace("TestProject", "/test/project")

            val provider = FilePathCompletionProvider()
            val context = CompletionContext(
                fullText = "/file:s",
                cursorPosition = 7,
                triggerType = CompletionTriggerType.COMMAND_VALUE,
                triggerOffset = 6,
                queryText = "s"
            )

            val completions = provider.getCompletions(context)

            // 短查询应该包含匹配的静态文件（如 settings.gradle.kts）
            assertTrue(completions.any { it.text.contains("settings") }, 
                "Short query 's' should match static files like settings.gradle.kts")
        } finally {
            WorkspaceManager.closeCurrentWorkspace()
            if (originalWorkspace != null) {
                WorkspaceManager.openWorkspace(originalWorkspace.name, originalWorkspace.rootPath ?: "")
            }
        }
    }
}

