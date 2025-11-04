package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.filesystem.FileInfo
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WriteFileToolTest {
    
    private class MockFileSystem : ToolFileSystem {
        private val files = mutableMapOf<String, String>()
        private val directories = mutableSetOf<String>()
        
        fun getFileContent(path: String): String? = files[path]
        fun hasFile(path: String): Boolean = path in files
        fun getFileCount(): Int = files.size
        
        override fun getProjectPath(): String = "/project"
        
        override suspend fun readFile(path: String): String? = files[path]
        
        override suspend fun writeFile(path: String, content: String, createDirectories: Boolean) {
            if (createDirectories) {
                val parentPath = path.substringBeforeLast('/')
                if (parentPath.isNotEmpty() && parentPath != path) {
                    createDirectory(parentPath, true)
                }
            }
            files[path] = content
        }
        
        override fun exists(path: String): Boolean = path in files || path in directories
        
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
        
        override fun createDirectory(path: String, createParents: Boolean) {
            directories.add(path)
            if (createParents) {
                var parent = path.substringBeforeLast('/')
                while (parent.isNotEmpty() && parent != path) {
                    directories.add(parent)
                    parent = parent.substringBeforeLast('/')
                }
            }
        }
        
        override fun delete(path: String, recursive: Boolean) {
            files.remove(path)
            directories.remove(path)
        }
    }
    
    @Test
    fun testWriteSimpleFile() = runTest {
        val fileSystem = MockFileSystem()
        val tool = WriteFileTool(fileSystem)
        
        val params = WriteFileParams(
            path = "test.txt",
            content = "Hello, World!"
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()
        
        assertTrue(result.isSuccess(), "Should succeed")
        assertEquals("Hello, World!", fileSystem.getFileContent("test.txt"))
        assertTrue(result.getOutput().contains("Successfully"))
    }
    
    @Test
    fun testWriteMultilineFile() = runTest {
        val fileSystem = MockFileSystem()
        val tool = WriteFileTool(fileSystem)
        
        val multilineContent = """
            package com.example.test
            
            import kotlinx.coroutines.*
            import kotlinx.serialization.Serializable
            
            /**
             * Test data class for multi-line content verification
             * This class demonstrates WriteFileTool's ability to handle
             * complex multi-line Kotlin code with proper formatting.
             */
            @Serializable
            data class TestData(
                val id: String,
                val name: String,
                val description: String,
                val tags: List<String> = emptyList()
            ) {
                /**
                 * Validates the test data
                 */
                fun isValid(): Boolean {
                    return id.isNotBlank() && 
                           name.isNotBlank() && 
                           description.isNotBlank()
                }
                
                /**
                 * Converts to JSON string
                 */
                fun toJson(): String {
                    return "{\n    \"id\": \"" + id + "\",\n    \"name\": \"" + name + "\"\n}"
                }
            }
        """.trimIndent()
        
        val params = WriteFileParams(
            path = "src/test/TestData.kt",
            content = multilineContent,
            createDirectories = true
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()
        
        assertTrue(result.isSuccess(), "Should succeed")
        
        val writtenContent = fileSystem.getFileContent("src/test/TestData.kt")
        assertEquals(multilineContent, writtenContent, "Content should match exactly")
        
        // Verify line count
        val expectedLines = multilineContent.lines().size
        val actualLines = writtenContent?.lines()?.size ?: 0
        assertEquals(expectedLines, actualLines, "Line count should match")
        
        // Verify it contains key Kotlin elements
        assertTrue(writtenContent!!.contains("package com.example.test"))
        assertTrue(writtenContent.contains("data class TestData"))
        assertTrue(writtenContent.contains("fun isValid()"))
        assertTrue(writtenContent.contains("@Serializable"))
    }
    
    @Test
    fun testWriteFileWithSpecialCharacters() = runTest {
        val fileSystem = MockFileSystem()
        val tool = WriteFileTool(fileSystem)
        
        val contentWithSpecialChars = """
            // Unicode and special characters test
            val greeting = "Hello, 世界! 🌍"
            val multilineString = ""${'"'}
                This is a multi-line string with:
                - Tabs:	here
                - Quotes: "double" and 'single'
                - Backslashes: \ and \\
                - Unicode: 你好, مرحبا, Здравствуйте
                - Emojis: 🚀 🎉 ✨
            ""${'"'}.trimIndent()
        """.trimIndent()
        
        val params = WriteFileParams(
            path = "special_chars.kt",
            content = contentWithSpecialChars
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()
        
        assertTrue(result.isSuccess(), "Should succeed")
        assertEquals(contentWithSpecialChars, fileSystem.getFileContent("special_chars.kt"))
    }
    
    @Test
    fun testToolMetadata() {
        val fileSystem = MockFileSystem()
        val tool = WriteFileTool(fileSystem)
        
        assertEquals("write-file", tool.name)
        assertTrue(tool.description.contains("Create new files"))
        assertTrue(tool.description.contains("write content"))
    }
}
