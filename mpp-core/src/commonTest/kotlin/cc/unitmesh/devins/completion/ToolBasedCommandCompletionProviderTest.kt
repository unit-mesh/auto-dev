package cc.unitmesh.devins.completion

import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.filesystem.FileInfo
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.agent.tool.shell.ShellResult
import cc.unitmesh.agent.tool.shell.ShellExecutionConfig
import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolException
import cc.unitmesh.devins.completion.providers.ToolBasedCommandCompletionProvider
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToolBasedCommandCompletionProviderTest {

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

    private class MockShellExecutor : ShellExecutor {
        override suspend fun execute(command: String, config: ShellExecutionConfig): ShellResult {
            throw ToolException("Shell execution not supported in tests", ToolErrorType.NOT_SUPPORTED)
        }

        override fun isAvailable(): Boolean = false

        override fun getDefaultShell(): String? = null

        override fun validateCommand(command: String): Boolean = true
    }

    private val fileSystem = MockFileSystem()
    private val shellExecutor = MockShellExecutor()
    private val toolRegistry = ToolRegistry(fileSystem, shellExecutor)
    private val provider = ToolBasedCommandCompletionProvider(toolRegistry)
    
    @Test
    fun testSupportsCommandTriggerType() {
        assertTrue(provider.supports(CompletionTriggerType.COMMAND))
        assertTrue(!provider.supports(CompletionTriggerType.AGENT))
        assertTrue(!provider.supports(CompletionTriggerType.VARIABLE))
    }
    
    @Test
    fun testGetAllToolCompletions() {
        val context = CompletionContext(
            fullText = "/",
            cursorPosition = 1,
            triggerType = CompletionTriggerType.COMMAND,
            triggerOffset = 0,
            queryText = ""
        )
        
        val completions = provider.getCompletions(context)
        
        assertTrue(completions.isNotEmpty(), "Should have tool completions")
        
        // Check that all built-in tools are present
        val toolNames = completions.map { it.text }.toSet()
        assertTrue("read-file" in toolNames, "Should contain read-file tool")
        assertTrue("write-file" in toolNames, "Should contain write-file tool")
        assertTrue("grep" in toolNames, "Should contain grep tool")
        assertTrue("glob" in toolNames, "Should contain glob tool")
    }
    
    @Test
    fun testFilterCompletionsByQuery() {
        val context = CompletionContext(
            fullText = "/read",
            cursorPosition = 5,
            triggerType = CompletionTriggerType.COMMAND,
            triggerOffset = 0,
            queryText = "read"
        )
        
        val completions = provider.getCompletions(context)
        
        assertTrue(completions.isNotEmpty(), "Should have matching completions")
        
        // Should prioritize exact matches
        val firstCompletion = completions.first()
        assertEquals("read-file", firstCompletion.text)
        assertTrue(firstCompletion.description?.isNotEmpty() == true, "Should have description")
        assertEquals("📄", firstCompletion.icon)
    }
    
    @Test
    fun testCompletionItemHasCorrectProperties() {
        val context = CompletionContext(
            fullText = "/write",
            cursorPosition = 6,
            triggerType = CompletionTriggerType.COMMAND,
            triggerOffset = 0,
            queryText = "write"
        )
        
        val completions = provider.getCompletions(context)
        val writeFileCompletion = completions.find { it.text == "write-file" }
        
        assertTrue(writeFileCompletion != null, "Should find write-file completion")
        assertEquals("write-file", writeFileCompletion.text)
        assertEquals("write-file", writeFileCompletion.displayText)
        assertTrue(writeFileCompletion.description?.contains("write") == true, "Description should mention writing")
        assertEquals("✏️", writeFileCompletion.icon)
        assertTrue(writeFileCompletion.insertHandler != null, "Should have insert handler")
    }
    
    @Test
    fun testInsertHandler() {
        val context = CompletionContext(
            fullText = "/read",
            cursorPosition = 5,
            triggerType = CompletionTriggerType.COMMAND,
            triggerOffset = 0,
            queryText = "read"
        )
        
        val completions = provider.getCompletions(context)
        val readFileCompletion = completions.find { it.text == "read-file" }
        
        assertTrue(readFileCompletion != null, "Should find read-file completion")
        
        val insertHandler = readFileCompletion.insertHandler
        assertTrue(insertHandler != null, "Should have insert handler")
        
        val result = insertHandler.invoke("/read", 5)
        assertEquals("/read-file ", result.newText)
        assertEquals(11, result.newCursorPosition) // Position after "/read-file "
        assertEquals(false, result.shouldTriggerNextCompletion)
    }
    
    @Test
    fun testNoMatchingCompletions() {
        val context = CompletionContext(
            fullText = "/xyz",
            cursorPosition = 4,
            triggerType = CompletionTriggerType.COMMAND,
            triggerOffset = 0,
            queryText = "xyz"
        )
        
        val completions = provider.getCompletions(context)
        
        assertTrue(completions.isEmpty(), "Should have no matching completions for non-existent tool")
    }
    
    @Test
    fun testPartialMatching() {
        val context = CompletionContext(
            fullText = "/gr",
            cursorPosition = 3,
            triggerType = CompletionTriggerType.COMMAND,
            triggerOffset = 0,
            queryText = "gr"
        )
        
        val completions = provider.getCompletions(context)
        
        assertTrue(completions.isNotEmpty(), "Should have matching completions")
        
        // Should find grep tool
        val grepCompletion = completions.find { it.text == "grep" }
        assertTrue(grepCompletion != null, "Should find grep tool")
    }
}
