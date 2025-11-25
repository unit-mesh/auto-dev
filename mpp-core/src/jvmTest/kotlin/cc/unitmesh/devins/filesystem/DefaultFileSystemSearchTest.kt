package cc.unitmesh.devins.filesystem

import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import java.io.File

class DefaultFileSystemSearchTest {
    
    private fun withTempDir(block: (Path) -> Unit) {
        val tempDir = Files.createTempDirectory("fs-test")
        try {
            block(tempDir)
        } finally {
            // Cleanup
            tempDir.toFile().deleteRecursively()
        }
    }
    
    @Test
    fun shouldFindRootFilesWhenLimitIsNotReached() {
        withTempDir { tempDir ->
            val readmePath = tempDir.resolve("README.md")
            val subDir = tempDir.resolve(".augment").also { it.createDirectories() }
            val augmentMd = subDir.resolve("test.md")
            
            readmePath.writeText("# README")
            augmentMd.writeText("# Test")
            
            val fileSystem = DefaultFileSystem(tempDir.toString())
            val results = fileSystem.searchFiles("*.md", maxDepth = 10, maxResults = 100)
            
            assertTrue(results.any { it.endsWith("README.md") }, "Should find README.md")
        }
    }
    
    @Test
    fun shouldMissRootFilesWhenLimitIsReachedBySubdir() {
        withTempDir { tempDir ->
            // Create root file
            val readmePath = tempDir.resolve("README.md")
            readmePath.writeText("# README")
            
            // Create subdir that comes alphabetically before README.md
            // .augment comes before R
            val subDir = tempDir.resolve(".augment").also { it.createDirectories() }
            
            // Create 110 files in subdir
            for (i in 1..110) {
                subDir.resolve("doc_$i.md").writeText("Content $i")
            }
            
            val fileSystem = DefaultFileSystem(tempDir.toString())
            // Limit to 100 results
            val results = fileSystem.searchFiles("*.md", maxDepth = 10, maxResults = 100)
            
            println("Found ${results.size} files")
            
            // If traversal is alphabetical, .augment is visited first
            // So we expect to find 100 files from .augment and NO README.md
            val foundReadme = results.any { it.endsWith("README.md") }
            val foundSubdirFiles = results.count { it.contains(".augment") }
            
            println("Found README: $foundReadme")
            println("Found .augment files: $foundSubdirFiles")
            
            // This confirms the hypothesis if true
            if (!foundReadme && foundSubdirFiles == 100) {
                println("HYPOTHESIS CONFIRMED: Root files missed due to limit reached by subdir files")
            } else {
                println("HYPOTHESIS REJECTED: README found or limit not reached as expected")
            }
        }
    }
    
    @Test
    fun shouldRespectGitIgnore() {
        withTempDir { tempDir ->
            // Setup
            val gitIgnore = tempDir.resolve(".gitignore")
            val ignoredFile = tempDir.resolve("ignored.md")
            val includedFile = tempDir.resolve("included.md")
            
            gitIgnore.writeText("ignored.md")
            ignoredFile.writeText("# Ignored")
            includedFile.writeText("# Included")
            
            // Test
            val fileSystem = DefaultFileSystem(tempDir.toString())
            val results = fileSystem.searchFiles("*.md", maxDepth = 10, maxResults = 100)
            
            println("Found files (gitignore): $results")
            
            // Verify
            assertTrue(results.any { it.endsWith("included.md") }, "Should find included.md")
            assertFalse(results.any { it.endsWith("ignored.md") }, "Should NOT find ignored.md")
        }
    }
    
    @Test
    fun shouldSupportMultiExtensionGlob() {
        withTempDir { tempDir ->
            // Setup
            val mdFile = tempDir.resolve("doc.md")
            val pdfFile = tempDir.resolve("doc.pdf")
            val txtFile = tempDir.resolve("doc.txt")
            val otherFile = tempDir.resolve("doc.other")
            
            mdFile.writeText("# MD")
            pdfFile.writeText("PDF")
            txtFile.writeText("TXT")
            otherFile.writeText("OTHER")
            
            // Test
            val fileSystem = DefaultFileSystem(tempDir.toString())
            // Search for md and pdf, but not txt or other
            val results = fileSystem.searchFiles("*.{md,pdf}", maxDepth = 10, maxResults = 100)
            
            println("Found files (glob): $results")
            
            // Verify
            assertTrue(results.any { it.endsWith("doc.md") }, "Should find doc.md")
            assertTrue(results.any { it.endsWith("doc.pdf") }, "Should find doc.pdf")
            assertFalse(results.any { it.endsWith("doc.txt") }, "Should NOT find doc.txt")
            assertFalse(results.any { it.endsWith("doc.other") }, "Should NOT find doc.other")
        }
    }

    @Test
    fun testRealProjectSearch() {
        val projectRoot = "/Volumes/source/ai/autocrud"
        val fileSystem = DefaultFileSystem(projectRoot)
        
        println("Searching in real project: $projectRoot")
        val startTime = System.currentTimeMillis()
        
        // Pattern used in DocumentIndexService
        val pattern = "**/*.{md,markdown,pdf,doc,docx,ppt,pptx,txt,html,htm}"
        val results = fileSystem.searchFiles(pattern, maxDepth = 20, maxResults = 2000)
        
        val endTime = System.currentTimeMillis()
        
        println("Search completed in ${endTime - startTime}ms")
        println("Found ${results.size} files:")
        results.take(20).forEach { println("- $it") }
        if (results.size > 20) println("... and ${results.size - 20} more")
        
        // Verify we found some expected files
        assertTrue(results.any { it.endsWith("README.md") }, "Should find README.md")
        // Verify we didn't find ignored files (if any known ones exist, e.g. in build/)
        assertFalse(results.any { it.contains("/build/") }, "Should not find files in build/")
    }
}
