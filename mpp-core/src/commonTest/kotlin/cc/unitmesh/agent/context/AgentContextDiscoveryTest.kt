package cc.unitmesh.agent.context

import cc.unitmesh.agent.tool.filesystem.FileInfo
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test AgentContextDiscovery implementation
 */
class AgentContextDiscoveryTest {
    
    /**
     * Mock file system for testing
     */
    private class MockFileSystem(
        private val files: Map<String, String> = emptyMap(),
        private val gitRoot: String? = null,
        private val projectPath: String = "/"
    ) : ToolFileSystem {
        
        override fun getProjectPath(): String = projectPath
        
        override suspend fun readFile(path: String): String? {
            return files[path]
        }
        
        override suspend fun writeFile(path: String, content: String, createDirectories: Boolean) {
            // Not implemented for test
        }
        
        override fun exists(path: String): Boolean {
            // .git marker exists ONLY at git root (exact match)
            if (path.endsWith("/.git") && gitRoot != null) {
                return path == "$gitRoot/.git"
            }
            return files.containsKey(path)
        }
        
        override fun listFiles(path: String, pattern: String?): List<String> {
            return emptyList()
        }
        
        override fun resolvePath(relativePath: String): String {
            return relativePath
        }
        
        override fun getFileInfo(path: String): FileInfo? {
            val content = files[path] ?: return null
            return FileInfo(
                path = path,
                isDirectory = false,
                size = content.length.toLong()
            )
        }
        
        override fun createDirectory(path: String, createParents: Boolean) {
            // Not implemented for test
        }
        
        override fun delete(path: String, recursive: Boolean) {
            // Not implemented for test
        }
    }
    
    @Test
    fun testNoFilesFound() = runTest {
        val fs = MockFileSystem()
        val discovery = AgentContextDiscovery(fs, maxBytes = 1024)
        
        val result = discovery.loadAgentContext("/project")
        
        assertEquals("", result)
    }
    
    @Test
    fun testSingleAgentsMdFile() = runTest {
        val content = "# Project Rules\n\nUse TypeScript for all new code."
        val fs = MockFileSystem(
            files = mapOf("/project/AGENTS.md" to content)
        )
        val discovery = AgentContextDiscovery(fs, maxBytes = 1024)
        
        val result = discovery.loadAgentContext("/project")
        
        assertTrue(result.contains("Project Rules"))
        assertTrue(result.contains("TypeScript"))
        assertTrue(result.contains("--- AGENTS.md from:"))
    }
    
    @Test
    fun testHierarchicalFiles() = runTest {
        val rootContent = "# Root Rules\n\nGeneral coding standards."
        val subContent = "# Subdirectory Rules\n\nSpecific to this module."
        
        val fs = MockFileSystem(
            files = mapOf(
                "/project/AGENTS.md" to rootContent,
                "/project/subdir/AGENTS.md" to subContent
            ),
            gitRoot = "/project"
        )
        val discovery = AgentContextDiscovery(fs, maxBytes = 2048)
        
        val result = discovery.loadAgentContext("/project/subdir")
        
        // Should include both files (root first, then subdir)
        assertTrue(result.contains("Root Rules"))
        assertTrue(result.contains("Subdirectory Rules"))
        
        // Root should appear before subdir
        val rootIndex = result.indexOf("Root Rules")
        val subdirIndex = result.indexOf("Subdirectory Rules")
        assertTrue(rootIndex < subdirIndex, "Root rules should appear before subdirectory rules")
    }
    
    @Test
    fun testOverrideFileHasPriority() = runTest {
        val overrideContent = "# Override Rules\n\nLocal development only."
        val standardContent = "# Standard Rules\n\nShould not be used."
        
        val fs = MockFileSystem(
            files = mapOf(
                "/project/AGENTS.override.md" to overrideContent,
                "/project/AGENTS.md" to standardContent
            )
        )
        val discovery = AgentContextDiscovery(fs, maxBytes = 2048)
        
        val result = discovery.loadAgentContext("/project")
        
        // Should only include override file
        assertTrue(result.contains("Override Rules"))
        assertTrue(!result.contains("Standard Rules"), "Standard file should be ignored when override exists")
    }
    
    @Test
    fun testFallbackFilenames() = runTest {
        val claudeContent = "# Claude Rules\n\nUse Claude.md format."
        
        val fs = MockFileSystem(
            files = mapOf("/project/CLAUDE.md" to claudeContent)
        )
        val discovery = AgentContextDiscovery(fs, maxBytes = 2048)
        
        val result = discovery.loadAgentContext(
            projectPath = "/project",
            fallbackFilenames = listOf("CLAUDE.md")
        )
        
        assertTrue(result.contains("Claude Rules"))
    }
    
    @Test
    fun testByteLimitTruncation() = runTest {
        val content = "A".repeat(1000)
        
        val fs = MockFileSystem(
            files = mapOf("/project/AGENTS.md" to content)
        )
        val discovery = AgentContextDiscovery(fs, maxBytes = 500)
        
        val result = discovery.loadAgentContext("/project")
        
        // Content should be truncated (including formatting)
        assertTrue(result.length < content.length + 200) // Allow for formatting overhead
    }
    
    @Test
    fun testZeroMaxBytesDisablesLoading() = runTest {
        val content = "# Rules\n\nSome content."
        
        val fs = MockFileSystem(
            files = mapOf("/project/AGENTS.md" to content)
        )
        val discovery = AgentContextDiscovery(fs, maxBytes = 0)
        
        val result = discovery.loadAgentContext("/project")
        
        assertEquals("", result, "Loading should be disabled when maxBytes is 0")
    }
}

