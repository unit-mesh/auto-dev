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
import kotlin.test.assertFalse

/**
 * Comprehensive test suite for SmartEditTool covering edge cases and error conditions.
 */
class SmartEditToolComprehensiveTest {

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

    // ===== Exact Replacement Tests =====

    @Test
    fun testExactReplacementWithMultipleLines() = runTest {
        fileSystem.addFile("/test.kt", """
            fun hello() {
                println("Hello, World!")
                println("Goodbye!")
            }
        """.trimIndent())
        
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = """println("Hello, World!")
                println("Goodbye!")""".trimIndent(),
            newString = """println("Hi!")
                println("See you!")""".trimIndent(),
            instruction = "Update greetings"
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isSuccess())
        val newContent = fileSystem.readFile("/test.kt")!!
        assertTrue(newContent.contains("println(\"Hi!\")"))
        assertTrue(newContent.contains("println(\"See you!\")"))
    }

    @Test
    fun testExactReplacementWithSpecialCharacters() = runTest {
        fileSystem.addFile("/test.kt", """val price = $100""")
        
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = """val price = $100""",
            newString = """val price = $200""",
            instruction = "Update price"
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isSuccess())
        assertEquals("val price = \$200", fileSystem.readFile("/test.kt"))
    }

    // ===== Flexible Replacement Tests =====

    @Test
    fun testFlexibleReplacementPreservesIndentation() = runTest {
        fileSystem.addFile("/test.kt", """
            class Foo {
                fun bar() {
                    val x = 1
                }
            }
        """.trimIndent())
        
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = "val x = 1",
            newString = "val x = 2",
            instruction = "Update x"
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isSuccess())
        val newContent = fileSystem.readFile("/test.kt")!!
        assertTrue(newContent.contains("        val x = 2"))
    }

    @Test
    fun testFlexibleReplacementWithDifferentWhitespace() = runTest {
        fileSystem.addFile("/test.kt", """
            fun   hello(  ) {
                println("Hi")
            }
        """.trimIndent())
        
        // oldString has normalized whitespace
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = "fun hello() {",
            newString = "fun goodbye() {",
            instruction = "Rename function"
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isSuccess())
        val newContent = fileSystem.readFile("/test.kt")!!
        // Should use regex replacement and preserve indentation
        assertTrue(newContent.contains("goodbye"))
    }

    // ===== Error Condition Tests =====

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
        val error = result as ToolResult.Error
        assertTrue(error.message.contains("File not found"))
    }

    @Test
    fun testAttemptToCreateExistingFile() = runTest {
        fileSystem.addFile("/test.kt", "existing content")
        
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = "", // Empty oldString means create file
            newString = "new content",
            instruction = "Create file"
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isError())
        val error = result as ToolResult.Error
        assertTrue(error.message.contains("already exists"))
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
        val error = result as ToolResult.Error
        assertTrue(error.message.contains("0 occurrences found"))
    }

    @Test
    fun testExpectedOccurrenceMismatch() = runTest {
        fileSystem.addFile("/test.kt", "val x = 1\nval x = 1\nval y = 2")
        
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = "val x = 1", // Appears twice
            newString = "val x = 2",
            instruction = "Update x"
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isError())
        val error = result as ToolResult.Error
        // Should fail because we expect 1 but found 2
        assertTrue(error.message.contains("expected 1") || error.message.contains("found 2"))
    }

    @Test
    fun testOldAndNewStringsIdentical() = runTest {
        fileSystem.addFile("/test.kt", "val x = 1")
        
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = "val x = 1",
            newString = "val x = 1", // Same as oldString
            instruction = "No change"
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isError())
        val error = result as ToolResult.Error
        assertTrue(error.message.contains("identical"))
    }

    // ===== Line Ending Tests =====

    @Test
    fun testPreserveCRLFLineEndings() = runTest {
        fileSystem.addFile("/test.kt", "line one\r\nline two\r\n")
        
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = "line two",
            newString = "line three",
            instruction = "Update line"
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isSuccess())
        val newContent = fileSystem.readFile("/test.kt")!!
        assertEquals("line one\r\nline three\r\n", newContent)
    }

    @Test
    fun testPreserveLFLineEndings() = runTest {
        fileSystem.addFile("/test.kt", "line one\nline two\n")
        
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = "line two",
            newString = "line three",
            instruction = "Update line"
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isSuccess())
        val newContent = fileSystem.readFile("/test.kt")!!
        assertEquals("line one\nline three\n", newContent)
    }

    @Test
    fun testPreserveTrailingNewline() = runTest {
        fileSystem.addFile("/test.kt", "val x = 1\n")
        
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = "val x = 1",
            newString = "val x = 2",
            instruction = "Update x"
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isSuccess())
        val newContent = fileSystem.readFile("/test.kt")!!
        assertEquals("val x = 2\n", newContent)
    }

    @Test
    fun testNoTrailingNewline() = runTest {
        fileSystem.addFile("/test.kt", "val x = 1")
        
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = "val x = 1",
            newString = "val x = 2",
            instruction = "Update x"
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isSuccess())
        val newContent = fileSystem.readFile("/test.kt")!!
        assertEquals("val x = 2", newContent)
    }

    // ===== Real-world Scenarios =====

    @Test
    fun testReplaceMethodBody() = runTest {
        fileSystem.addFile("/test.kt", """
            class Calculator {
                fun add(a: Int, b: Int): Int {
                    return a + b
                }
                
                fun subtract(a: Int, b: Int): Int {
                    return a - b
                }
            }
        """.trimIndent())
        
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = """    fun add(a: Int, b: Int): Int {
                    return a + b
                }""".trimIndent(),
            newString = """    fun add(a: Int, b: Int): Int {
                    println("Adding ${"$"}a and ${"$"}b")
                    return a + b
                }""".trimIndent(),
            instruction = "Add logging to add method"
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isSuccess())
        val newContent = fileSystem.readFile("/test.kt")!!
        assertTrue(newContent.contains("println"))
    }

    @Test
    fun testReplaceWithUnicodeContent() = runTest {
        fileSystem.addFile("/test.kt", """val greeting = "Hello 世界"""")
        
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = """val greeting = "Hello 世界"""",
            newString = """val greeting = "你好 World"""",
            instruction = "Update greeting"
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isSuccess())
        val newContent = fileSystem.readFile("/test.kt")!!
        assertEquals("""val greeting = "你好 World"""", newContent)
    }

    // ===== Tool Metadata Tests =====

    @Test
    fun testToolLocations() {
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = "old",
            newString = "new",
            instruction = "test"
        )
        
        val invocation = tool.createInvocation(params)
        val locations = invocation.getToolLocations()
        
        assertEquals(1, locations.size)
        assertEquals("/test.kt", locations[0].path)
        assertEquals(LocationType.FILE, locations[0].type)
    }

    @Test
    fun testGetDescription() {
        val params = SmartEditParams(
            filePath = "/test.kt",
            oldString = "old",
            newString = "new",
            instruction = "test"
        )
        
        val invocation = tool.createInvocation(params)
        val description = invocation.getDescription()
        
        assertTrue(description.contains("/test.kt"))
        assertTrue(description.contains("old"))
        assertTrue(description.contains("new"))
    }

    @Test
    fun testPathTraversalRejection() {
        val params = SmartEditParams(
            filePath = "../../../etc/passwd",
            oldString = "root",
            newString = "hacker",
            instruction = "evil"
        )
        
        try {
            tool.createInvocation(params)
            assertTrue(false, "Should have thrown exception for path traversal")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("Path traversal") == true)
        }
    }
}
