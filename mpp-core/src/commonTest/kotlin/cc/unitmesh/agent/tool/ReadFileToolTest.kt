package cc.unitmesh.agent.tool

import cc.unitmesh.agent.tool.filesystem.FileInfo
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.impl.ReadFileParams
import cc.unitmesh.agent.tool.impl.ReadFileTool
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ReadFileToolTest {
    
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
                lastModified = System.currentTimeMillis()
            )
        }
        
        override fun createDirectory(path: String, createParents: Boolean) {}
        override fun delete(path: String, recursive: Boolean) {}
    }
    
    @Test
    fun testReadFileSuccess() = runTest {
        val fileSystem = MockFileSystem().apply {
            addFile("test.txt", "Hello, World!\nSecond line\nThird line")
        }
        val tool = ReadFileTool(fileSystem)

        val params = ReadFileParams(path = "test.txt")
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()

        assertTrue(result.isSuccess(), "Should succeed")
        assertEquals("Hello, World!\nSecond line\nThird line", result.getOutput())
    }
    
    @Test
    fun testReadFileWithLineRange() = runTest {
        val fileSystem = MockFileSystem().apply {
            addFile("test.txt", "Line 1\nLine 2\nLine 3\nLine 4\nLine 5")
        }
        val tool = ReadFileTool(fileSystem)
        
        val params = ReadFileParams(path = "test.txt", startLine = 2, endLine = 4)
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()
        
        assertTrue(result.isSuccess(), "Should succeed")
        assertEquals("Line 2\nLine 3\nLine 4", result.getOutput())
    }
    
    @Test
    fun testReadFileWithMaxLines() = runTest {
        val fileSystem = MockFileSystem().apply {
            addFile("test.txt", "Line 1\nLine 2\nLine 3\nLine 4\nLine 5")
        }
        val tool = ReadFileTool(fileSystem)
        
        val params = ReadFileParams(path = "test.txt", startLine = 2, maxLines = 2)
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()
        
        assertTrue(result.isSuccess(), "Should succeed")
        assertEquals("Line 2\nLine 3", result.getOutput())
    }
    
    @Test
    fun testReadFileNotFound() = runTest {
        val fileSystem = MockFileSystem()
        val tool = ReadFileTool(fileSystem)
        
        val params = ReadFileParams(path = "nonexistent.txt")
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()
        
        assertTrue(result.isError(), "Should fail")
        assertTrue(result.getError().contains("not found"), "Should indicate file not found")
    }
    
    @Test
    fun testInvalidLineRange() = runTest {
        val fileSystem = MockFileSystem().apply {
            addFile("test.txt", "Line 1\nLine 2\nLine 3")
        }
        val tool = ReadFileTool(fileSystem)
        
        val params = ReadFileParams(path = "test.txt", startLine = 5, endLine = 10)
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()
        
        assertTrue(result.isError(), "Should fail")
        assertTrue(result.getError().contains("beyond file length"), "Should indicate line out of range")
    }
    
    @Test
    fun testGetDescription() {
        val fileSystem = MockFileSystem()
        val tool = ReadFileTool(fileSystem)
        
        val params = ReadFileParams(path = "test.txt", startLine = 1, endLine = 10)
        val invocation = tool.createInvocation(params)
        
        val description = invocation.getDescription()
        assertTrue(description.contains("test.txt"), "Should contain file path")
        assertTrue(description.contains("lines 1-10"), "Should contain line range")
    }
    
    @Test
    fun testGetToolLocations() {
        val fileSystem = MockFileSystem()
        val tool = ReadFileTool(fileSystem)
        
        val params = ReadFileParams(path = "test.txt")
        val invocation = tool.createInvocation(params)
        
        val locations = invocation.getToolLocations()
        assertEquals(1, locations.size)
        assertEquals("test.txt", locations[0].path)
        assertEquals(LocationType.FILE, locations[0].type)
    }
}
