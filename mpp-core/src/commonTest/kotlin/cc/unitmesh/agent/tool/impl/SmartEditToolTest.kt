package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.LocationType
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.filesystem.FileInfo
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmartEditToolTest {

    private class MockFileSystem : ToolFileSystem {
        private val files = mutableMapOf<String, String>()

        fun addFile(path: String, content: String) {
            files[path] = content
        }

        override fun getProjectPath(): String = "/project"

        override suspend fun readFile(path: String): String? = files[path]

        override suspend fun writeFile(path: String, content: String, createDirectories: Boolean) {
            files[path] = content
        }

        override fun exists(path: String): Boolean = path in files

        override fun listFiles(path: String, pattern: String?): List<String> = emptyList()

        override fun resolvePath(relativePath: String): String =
            if (relativePath.startsWith("/")) relativePath else "/project/$relativePath"

        override fun getFileInfo(path: String): FileInfo? {
            val content = files[path] ?: return null
            return FileInfo(
                path = path,
                isDirectory = false,
                size = content.length.toLong(),
                lastModified = Clock.System.now().toEpochMilliseconds()
            )
        }

        override fun createDirectory(path: String, createParents: Boolean) {}
        override fun delete(path: String, recursive: Boolean) {}
    }

    private lateinit var fileSystem: MockFileSystem
    private lateinit var mockLLMService: KoogLLMService
    private lateinit var tool: SmartEditTool

    @BeforeTest
    fun setup() {
        fileSystem = MockFileSystem()
        val mockConfig = cc.unitmesh.llm.ModelConfig(
            provider = cc.unitmesh.llm.LLMProviderType.OPENAI,
            modelName = "gpt-3.5-turbo",
            apiKey = "test-key",
            baseUrl = "",
            temperature = 0.7,
            maxTokens = 4000
        )
        mockLLMService = KoogLLMService(mockConfig)
        tool = SmartEditTool(fileSystem, mockLLMService)
    }

    @Test
    fun testExactReplacement() = runTest {
        fileSystem.addFile("/test.kt", "val x = 1\nval y = 2")
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = "val x = 1",
            newString = "val x = 10",
            instruction = "Update x to 10"
        )
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isSuccess())
        assertEquals("val x = 10\nval y = 2", fileSystem.readFile("/test.kt"))
    }

    @Test
    fun testFlexibleReplacement() = runTest {
        fileSystem.addFile("/test.kt", "    val x = 1\n    val y = 2")
        // oldString has different indentation but matches content
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = "val x = 1",
            newString = "val x = 10",
            instruction = "Update x to 10"
        )
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isSuccess())
        // Should preserve indentation
        assertEquals("    val x = 10\n    val y = 2", fileSystem.readFile("/test.kt"))
    }

    @Test
    fun testRegexReplacement() = runTest {
        fileSystem.addFile("/test.kt", "    val x = 1")
        // Regex replacement logic allows for flexible whitespace
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = "val   x=  1", // Messy whitespace
            newString = "val x = 10",
            instruction = "Update x to 10"
        )
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isSuccess())
        assertEquals("    val x = 10", fileSystem.readFile("/test.kt"))
    }

    @Test
    fun testCreateNewFile() = runTest {
        val params = SmartEditParams(
            filePath = "/new.kt",
            oldString = "",
            newString = "val x = 1",
            instruction = "Create new file"
        )
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isSuccess())
        assertEquals("val x = 1", fileSystem.readFile("/new.kt"))
    }

    @Test
    fun testFileNotFound() = runTest {
        val params = SmartEditParams(
            filePath = "/nonexistent.kt",
            oldString = "foo",
            newString = "bar",
            instruction = "Update foo"
        )
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isError())
        assertTrue((result as ToolResult.Error).message.contains("File not found"))
    }

    @Test
    fun testNoOccurrenceFound() = runTest {
        fileSystem.addFile("/test.kt", "val x = 1")
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = "val y = 2",
            newString = "val y = 3",
            instruction = "Update y"
        )
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isError())
        assertTrue((result as ToolResult.Error).message.contains("0 occurrences found"))
    }
}
