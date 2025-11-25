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
            
            // We actually want to fix this, so eventually this test should FAIL if we assert foundReadme
            // But for now, let's just see the behavior
        }
    }
}
