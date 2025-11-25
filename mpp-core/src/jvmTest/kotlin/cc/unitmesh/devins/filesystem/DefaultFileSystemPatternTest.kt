package cc.unitmesh.devins.filesystem

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.BeforeTest

/**
 * Test for file pattern matching with braces syntax like *.{md,txt}
 */
class DefaultFileSystemPatternTest {
    
    private lateinit var tempDir: Path
    
    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("test")
    }
    
    @Test
    fun `should match files with brace pattern`() {
        // Setup: create test files
        val testFiles = listOf(
            "README.md",
            "CHANGELOG.md",
            "notes.txt",
            "config.json",
            "script.sh",
            "doc.markdown"
        )
        
        testFiles.forEach { fileName ->
            tempDir.resolve(fileName).writeText("test content")
        }
        
        // Create a subdirectory with more files
        val subDir = tempDir.resolve("docs")
        subDir.createDirectories()
        subDir.resolve("guide.md").writeText("guide content")
        subDir.resolve("api.md").writeText("api content")
        
        // Test
        val fileSystem = DefaultFileSystem(tempDir.toString())
        
        // Test 1: Pattern with braces - should match md, txt, markdown
        val pattern1 = "*.{md,txt,markdown}"
        val results1 = fileSystem.searchFiles(pattern1, maxDepth = 10, maxResults = 100)
        
        println("Pattern: $pattern1")
        println("Results: $results1")
        
        assertTrue(results1.contains("README.md"), "Should match README.md")
        assertTrue(results1.contains("CHANGELOG.md"), "Should match CHANGELOG.md")
        assertTrue(results1.contains("notes.txt"), "Should match notes.txt")
        assertTrue(results1.contains("doc.markdown"), "Should match doc.markdown")
        assertTrue(results1.contains("docs/guide.md"), "Should match docs/guide.md")
        assertTrue(results1.contains("docs/api.md"), "Should match docs/api.md")
        assertFalse(results1.contains("config.json"), "Should NOT match config.json")
        assertFalse(results1.contains("script.sh"), "Should NOT match script.sh")
        
        // Test 2: Simple wildcard pattern
        val pattern2 = "*.md"
        val results2 = fileSystem.searchFiles(pattern2, maxDepth = 10, maxResults = 100)
        
        println("\nPattern: $pattern2")
        println("Results: $results2")
        
        assertTrue(results2.contains("README.md"), "Should match README.md")
        assertTrue(results2.contains("CHANGELOG.md"), "Should match CHANGELOG.md")
        assertTrue(results2.contains("docs/guide.md"), "Should match docs/guide.md")
        assertFalse(results2.contains("notes.txt"), "Should NOT match notes.txt")
        assertFalse(results2.contains("doc.markdown"), "Should NOT match doc.markdown")
        
        // Test 3: Multiple extensions with DocumentReaderViewModel pattern
        val pattern3 = "*.{md,markdown,pdf,doc,docx,ppt,pptx,txt,html,htm}"
        val results3 = fileSystem.searchFiles(pattern3, maxDepth = 10, maxResults = 100)
        
        println("\nPattern: $pattern3")
        println("Results: $results3")
        
        assertTrue(results3.contains("README.md"), "Should match README.md")
        assertTrue(results3.contains("notes.txt"), "Should match notes.txt")
        assertTrue(results3.contains("doc.markdown"), "Should match doc.markdown")
        assertFalse(results3.contains("config.json"), "Should NOT match config.json")
        
        // Verify count
        val expectedCount = 6 // README.md, CHANGELOG.md, notes.txt, doc.markdown, docs/guide.md, docs/api.md
        assertEquals(expectedCount, results3.size, "Should find exactly $expectedCount matching files")
    }
    
    @Test
    fun `should match README in root directory`() {
        // Setup
        tempDir.resolve("README.md").writeText("# Project README")
        tempDir.resolve("src").createDirectories()
        tempDir.resolve("src/README.md").writeText("# Source README")
        
        // Test
        val fileSystem = DefaultFileSystem(tempDir.toString())
        val results = fileSystem.searchFiles("README.md", maxDepth = 10, maxResults = 100)
        
        println("Looking for README.md")
        println("Results: $results")
        
        assertTrue(results.contains("README.md"), "Should find README.md in root")
        assertTrue(results.contains("src/README.md"), "Should find README.md in subdirectory")
        assertEquals(2, results.size, "Should find exactly 2 README.md files")
    }
    
    @Test
    fun `should match wildcard README pattern`() {
        // Setup
        tempDir.resolve("README.md").writeText("# Project README")
        tempDir.resolve("README.txt").writeText("Project README")
        tempDir.resolve("READ_THIS.md").writeText("Read this")
        
        // Test
        val fileSystem = DefaultFileSystem(tempDir.toString())
        val results = fileSystem.searchFiles("README*", maxDepth = 10, maxResults = 100)
        
        println("Pattern: README*")
        println("Results: $results")
        
        assertTrue(results.contains("README.md"), "Should match README.md")
        assertTrue(results.contains("README.txt"), "Should match README.txt")
        assertFalse(results.contains("READ_THIS.md"), "Should NOT match READ_THIS.md")
    }
    
    @Test
    fun `should respect maxResults limit`() {
        // Setup: create many files
        repeat(50) { i ->
            tempDir.resolve("file$i.md").writeText("content $i")
        }
        
        // Test
        val fileSystem = DefaultFileSystem(tempDir.toString())
        val results = fileSystem.searchFiles("*.md", maxDepth = 10, maxResults = 10)
        
        println("Created 50 files, maxResults=10")
        println("Found: ${results.size} files")
        
        assertEquals(10, results.size, "Should respect maxResults limit")
    }
}

