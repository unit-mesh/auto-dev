package cc.unitmesh.indexer

import cc.unitmesh.devins.filesystem.EmptyFileSystem
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.indexer.model.DomainDictionary
import cc.unitmesh.indexer.model.SemanticName
import cc.unitmesh.indexer.model.ElementType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test for DomainDictService
 */
class DomainDictServiceTest {
    
    @Test
    fun testCollectSemanticNames_withEmptyFileSystem() = runTest {
        val fileSystem = EmptyFileSystem()
        val service = DomainDictService(fileSystem)
        
        val result = service.collectSemanticNames(1000)
        
        assertNotNull(result)
        assertTrue(result.level1.isEmpty())
        assertTrue(result.level2.isEmpty())
    }
    
    @Test
    fun testCollectSemanticNames_withMockFileSystem() = runTest {
        val mockFileSystem = MockFileSystem(mutableMapOf(
            "src/main/java/com/example/UserController.java" to """
                package com.example;
                public class UserController {
                    public User getUserById(Long id) { return null; }
                    public void createUser(User user) { }
                }
            """.trimIndent(),
            "src/main/java/com/example/BlogService.java" to """
                package com.example;
                public class BlogService {
                    public Blog createBlog(String title) { return null; }
                    public List<Blog> getAllBlogs() { return null; }
                }
            """.trimIndent()
        ))
        
        val service = DomainDictService(mockFileSystem)
        val result = service.collectSemanticNames(2000)
        
        assertNotNull(result)
        assertTrue(result.level1.isNotEmpty(), "Should have file-level semantic names")
        
        // Check that technical suffixes are removed
        val allNames = result.getAllNames()
        assertTrue(allNames.any { it.contains("User") }, "Should contain User")
        assertTrue(allNames.any { it.contains("Blog") }, "Should contain Blog")
    }
    
    @Test
    fun testSaveAndLoadContent() = runTest {
        val mockFileSystem = MockFileSystem()
        val service = DomainDictService(mockFileSystem)
        
        val testContent = "中文,代码翻译,描述\n用户,User,用户实体\n博客,Blog,博客实体"
        
        val saved = service.saveContent(testContent)
        assertTrue(saved, "Should save content successfully")
        
        val loaded = service.loadContent()
        assertEquals(testContent, loaded, "Loaded content should match saved content")
    }
}

/**
 * Mock file system for testing
 */
class MockFileSystem(
    private val files: MutableMap<String, String> = mutableMapOf(),
    private val directories: MutableSet<String> = mutableSetOf()
) : ProjectFileSystem {
    
    override fun getProjectPath(): String? = "/mock/project"
    
    override fun readFile(path: String): String? = files[path]
    
    override fun writeFile(path: String, content: String): Boolean {
        files[path] = content
        return true
    }
    
    override fun exists(path: String): Boolean = files.containsKey(path) || directories.contains(path)
    
    override fun isDirectory(path: String): Boolean = directories.contains(path)
    
    override fun listFiles(path: String, pattern: String?): List<String> {
        return files.keys.filter { it.startsWith("$path/") }
            .map { it.substringAfter("$path/") }
            .filter { !it.contains("/") } // Only direct children
    }
    
    override fun searchFiles(pattern: String, maxDepth: Int, maxResults: Int): List<String> {
        val regex = pattern.replace("*", ".*").toRegex()
        return files.keys.filter { regex.matches(it.substringAfterLast("/")) }
            .take(maxResults)
    }
    
    override fun resolvePath(relativePath: String): String = "/mock/project/$relativePath"
    
    override fun createDirectory(path: String): Boolean {
        directories.add(path)
        return true
    }
}
