package cc.unitmesh.devins.completion

import cc.unitmesh.devins.filesystem.ProjectFileSystem
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileSearchTest {
    
    private class MockFileSystem(private val files: List<String>) : ProjectFileSystem {
        override fun getProjectPath(): String = "/test/project"
        override fun readFile(path: String): String? = null
        override fun readFileAsBytes(path: String): ByteArray? {
            return null
        }

        override fun writeFile(path: String, content: String): Boolean {
            return false
        }

        override fun exists(path: String): Boolean = files.contains(path)
        override fun isDirectory(path: String): Boolean = path.endsWith("/")
        override fun listFiles(path: String, pattern: String?): List<String> = files
        override fun searchFiles(pattern: String, maxDepth: Int, maxResults: Int): List<String> {
            return files.take(maxResults)
        }
        override fun resolvePath(relativePath: String): String = relativePath
    }
    
    @Test
    fun testRecursiveFileSearchInitialization() = runTest {
        val files = listOf(
            "README.md",
            "src/main/kotlin/App.kt",
            "src/main/kotlin/Controller.kt",
            "src/test/kotlin/AppTest.kt"
        )
        
        val fileSystem = MockFileSystem(files)
        val options = FileSearchOptions(
            projectRoot = "/test/project",
            enableRecursiveSearch = true,
            enableFuzzyMatch = true
        )
        
        val search = FileSearchFactory.create(fileSystem, options)
        search.initialize()
        
        // 搜索应该能返回结果
        val results = search.search("Controller", maxResults = 10)
        assertTrue(results.isNotEmpty(), "Should find files")
    }
    
    @Test
    fun testWildcardSearch() = runTest {
        val files = listOf(
            "Controller.kt",
            "UserController.kt",
            "ApiController.kt",
            "Service.kt"
        )
        
        val fileSystem = MockFileSystem(files)
        val options = FileSearchOptions(
            projectRoot = "/test/project",
            enableRecursiveSearch = true
        )
        
        val search = FileSearchFactory.create(fileSystem, options) as RecursiveFileSearch
        search.initialize()
        
        val results = search.search("*Controller*", maxResults = 10)
        assertEquals(3, results.size, "Should find all Controller files")
        assertTrue(results.all { it.contains("Controller") })
    }
    
    @Test
    fun testFuzzyMatch() = runTest {
        val files = listOf(
            "UserController.kt",
            "UserService.kt",
            "ApiController.kt"
        )
        
        val fileSystem = MockFileSystem(files)
        val options = FileSearchOptions(
            projectRoot = "/test/project",
            enableRecursiveSearch = true,
            enableFuzzyMatch = true
        )
        
        val search = FileSearchFactory.create(fileSystem, options) as RecursiveFileSearch
        search.initialize()
        
        // 模糊匹配 "usrctl" 应该找到 "UserController"
        val results = search.search("usrctl", maxResults = 10)
        assertTrue(results.any { it.contains("UserController") }, 
            "Fuzzy match should find UserController")
    }
    
    @Test
    fun testEmptyPatternReturnsAllFiles() = runTest {
        val files = listOf(
            "README.md",
            "App.kt",
            "Test.kt"
        )
        
        val fileSystem = MockFileSystem(files)
        val options = FileSearchOptions(
            projectRoot = "/test/project",
            enableRecursiveSearch = true
        )
        
        val search = FileSearchFactory.create(fileSystem, options)
        search.initialize()
        
        val results = search.search("", maxResults = 10)
        assertTrue(results.isNotEmpty(), "Empty pattern should return files")
    }
    
    @Test
    fun testMaxResultsLimit() = runTest {
        val files = (1..100).map { "File$it.kt" }
        
        val fileSystem = MockFileSystem(files)
        val options = FileSearchOptions(
            projectRoot = "/test/project",
            enableRecursiveSearch = true
        )
        
        val search = FileSearchFactory.create(fileSystem, options)
        search.initialize()
        
        val results = search.search("File", maxResults = 10)
        assertTrue(results.size <= 10, "Should respect maxResults limit")
    }
}

