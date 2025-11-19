package cc.unitmesh.agent.tool

import cc.unitmesh.agent.tool.schema.ToolCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ToolTypeTest {
    
    @Test
    fun testToolTypeBasicProperties() {
        val readFile = ToolType.ReadFile
        assertEquals("read-file", readFile.name)
        assertEquals("Read File", readFile.displayName)
        assertEquals("📄", readFile.tuiEmoji)
        assertEquals("file_open", readFile.composeIcon)
        assertEquals(ToolCategory.FileSystem, readFile.category)
    }
    
    @Test
    fun testFromName() {
        assertEquals(ToolType.ReadFile, ToolType.fromName("read-file"))
        assertEquals(ToolType.WriteFile, ToolType.fromName("write-file"))
        assertEquals(ToolType.Shell, ToolType.fromName("shell"))
        assertEquals(ToolType.Glob, ToolType.fromName("glob"))
        assertEquals(ToolType.Grep, ToolType.fromName("grep"))
        assertNull(ToolType.fromName("non-existent-tool"))
    }
    
    @Test
    fun testAllTools() {
        val allTools = ToolType.ALL_TOOLS
        assertTrue(allTools.isNotEmpty())
        assertTrue(allTools.contains(ToolType.ReadFile))
        assertTrue(allTools.contains(ToolType.WriteFile))
        assertTrue(allTools.contains(ToolType.Shell))
        assertTrue(allTools.contains(ToolType.Glob))
        assertTrue(allTools.contains(ToolType.Grep))
        assertTrue(allTools.contains(ToolType.ErrorAgent))
        assertTrue(allTools.contains(ToolType.AnalysisAgent))
        assertTrue(allTools.contains(ToolType.CodeAgent))
    }
    
    @Test
    fun testAllToolNames() {
        val allToolNames = ToolType.ALL_TOOL_NAMES
        assertTrue(allToolNames.contains("read-file"))
        assertTrue(allToolNames.contains("write-file"))
        assertTrue(allToolNames.contains("shell"))
        assertTrue(allToolNames.contains("glob"))
        assertTrue(allToolNames.contains("grep"))
        assertTrue(allToolNames.contains("error-agent"))
        assertTrue(allToolNames.contains("analysis-agent"))
        assertTrue(allToolNames.contains("code-agent"))
    }
    
    @Test
    fun testIsValidToolName() {
        assertTrue(ToolType.isValidToolName("read-file"))
        assertTrue(ToolType.isValidToolName("write-file"))
        assertTrue(ToolType.isValidToolName("shell"))
        assertFalse(ToolType.isValidToolName("non-existent-tool"))
        assertFalse(ToolType.isValidToolName(""))
    }
    
    @Test
    fun testRequiresFileSystem() {
        assertTrue(ToolType.requiresFileSystem(ToolType.ReadFile))
        assertTrue(ToolType.requiresFileSystem(ToolType.WriteFile))
        assertTrue(ToolType.requiresFileSystem(ToolType.Glob))
        assertTrue(ToolType.requiresFileSystem(ToolType.Grep))
        assertFalse(ToolType.requiresFileSystem(ToolType.Shell))
    }
    
    @Test
    fun testIsExecutionTool() {
        assertTrue(ToolType.isExecutionTool(ToolType.Shell))
        assertFalse(ToolType.isExecutionTool(ToolType.ReadFile))
        assertFalse(ToolType.isExecutionTool(ToolType.WriteFile))
        assertFalse(ToolType.isExecutionTool(ToolType.Glob))
    }
    
    @Test
    fun testExtensionFunctions() {
        assertEquals(ToolType.ReadFile, "read-file".toToolType())
        assertEquals(ToolType.WriteFile, "write-file".toToolType())
        assertNull("non-existent-tool".toToolType())
        
        assertTrue("read-file".isValidToolName())
        assertTrue("write-file".isValidToolName())
        assertFalse("non-existent-tool".isValidToolName())
        
        assertEquals(ToolCategory.FileSystem, "read-file".getToolCategory())
        assertEquals(ToolCategory.Execution, "shell".getToolCategory())
        assertNull("non-existent-tool".getToolCategory())
    }
    
    @Test
    fun testToolCategoryProperties() {
        val fileSystemCategory = ToolCategory.FileSystem
        assertEquals("File System", fileSystemCategory.displayName)
        assertEquals("📁", fileSystemCategory.tuiEmoji)
        assertEquals("folder", fileSystemCategory.composeIcon)
        
        val executionCategory = ToolCategory.Execution
        assertEquals("Execution", executionCategory.displayName)
        assertEquals("⚡", executionCategory.tuiEmoji)
        assertEquals("play_arrow", executionCategory.composeIcon)
    }
    
    @Test
    fun testUniqueToolNames() {
        val allNames = ToolType.ALL_TOOLS.map { it.name }
        val uniqueNames = allNames.toSet()
        assertEquals(allNames.size, uniqueNames.size, "All tool names should be unique")
    }
}
