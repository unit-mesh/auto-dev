package cc.unitmesh.agent.tool.filesystem

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for WasmJsToolFileSystem (in-memory file system)
 */
class WasmJsToolFileSystemTest {
    
    @Test
    fun testCreateAndReadFile() = runTest {
        val fs = WasmJsToolFileSystem("/project")
        
        // Write file
        fs.writeFile("test.txt", "Hello, World!", createDirectories = true)
        
        // Read file
        val content = fs.readFile("test.txt")
        assertEquals("Hello, World!", content)
    }
    
    @Test
    fun testFileExists() = runTest {
        val fs = WasmJsToolFileSystem("/project")
        
        assertFalse(fs.exists("nonexistent.txt"))
        
        fs.writeFile("exists.txt", "content", createDirectories = true)
        assertTrue(fs.exists("exists.txt"))
    }
    
    @Test
    fun testCreateDirectory() {
        val fs = WasmJsToolFileSystem("/project")
        
        fs.createDirectory("subdir", createParents = true)
        assertTrue(fs.exists("subdir"))
        
        val info = fs.getFileInfo("subdir")
        assertNotNull(info)
        assertTrue(info.isDirectory)
    }
    
    @Test
    fun testNestedDirectories() = runTest {
        val fs = WasmJsToolFileSystem("/project")
        
        // Create nested directory structure
        fs.createDirectory("a/b/c", createParents = true)
        
        // Write file in nested directory
        fs.writeFile("a/b/c/test.txt", "nested content", createDirectories = true)
        
        // Read file
        val content = fs.readFile("a/b/c/test.txt")
        assertEquals("nested content", content)
    }
    
    @Test
    fun testListFiles() = runTest {
        val fs = WasmJsToolFileSystem("/project")
        
        // Create multiple files
        fs.writeFile("file1.txt", "content1", createDirectories = true)
        fs.writeFile("file2.txt", "content2", createDirectories = true)
        fs.writeFile("file3.md", "content3", createDirectories = true)
        
        // List all files
        val allFiles = fs.listFiles("/project")
        assertEquals(3, allFiles.size)
        
        // List with pattern
        val txtFiles = fs.listFiles("/project", "*.txt")
        assertEquals(2, txtFiles.size)
    }
    
    @Test
    fun testDeleteFile() = runTest {
        val fs = WasmJsToolFileSystem("/project")
        
        fs.writeFile("delete-me.txt", "temporary", createDirectories = true)
        assertTrue(fs.exists("delete-me.txt"))
        
        fs.delete("delete-me.txt", recursive = false)
        assertFalse(fs.exists("delete-me.txt"))
    }
    
    @Test
    fun testPathNormalization() = runTest {
        val fs = WasmJsToolFileSystem("/project")
        
        // Test various path formats
        fs.writeFile("./test.txt", "content1", createDirectories = true)
        fs.writeFile("test.txt", "content2", createDirectories = true) // overwrite
        
        val content = fs.readFile("/project/test.txt")
        assertEquals("content2", content)
    }
    
    @Test
    fun testFileInfo() = runTest {
        val fs = WasmJsToolFileSystem("/project")
        
        fs.writeFile("info-test.txt", "Hello", createDirectories = true)
        
        val info = fs.getFileInfo("info-test.txt")
        assertNotNull(info)
        assertFalse(info.isDirectory)
        assertEquals(5L, info.size) // "Hello" has 5 characters
        assertTrue(info.isReadable)
        assertTrue(info.isWritable)
    }
    
    @Test
    fun testUpdateFile() = runTest {
        val fs = WasmJsToolFileSystem("/project")
        
        fs.writeFile("update.txt", "original", createDirectories = true)
        assertEquals("original", fs.readFile("update.txt"))
        
        fs.writeFile("update.txt", "updated", createDirectories = false)
        assertEquals("updated", fs.readFile("update.txt"))
    }
    
    @Test
    fun testReadNonExistentFile() = runTest {
        val fs = WasmJsToolFileSystem("/project")
        
        val content = fs.readFile("nonexistent.txt")
        assertNull(content)
    }
    
    @Test
    fun testAbsolutePaths() = runTest {
        val fs = WasmJsToolFileSystem("/project")
        
        fs.writeFile("/project/absolute.txt", "absolute content", createDirectories = true)
        
        val content = fs.readFile("/project/absolute.txt")
        assertEquals("absolute content", content)
    }
}
