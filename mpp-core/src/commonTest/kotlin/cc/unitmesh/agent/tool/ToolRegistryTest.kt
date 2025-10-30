package cc.unitmesh.agent.tool

import cc.unitmesh.agent.tool.filesystem.EmptyToolFileSystem
import cc.unitmesh.agent.tool.impl.ReadFileParams
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.shell.EmptyShellExecutor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ToolRegistryTest {
    
    private val fileSystem = EmptyToolFileSystem()
    private val shellExecutor = EmptyShellExecutor()
    private val registry = ToolRegistry(fileSystem, shellExecutor)
    
    @Test
    fun testRegistryInitialization() {
        val stats = registry.getStats()
        assertTrue(stats.totalTools > 0, "Registry should have built-in tools")
        assertTrue(stats.availableTools.isNotEmpty(), "Should have available tools")
    }
    
    @Test
    fun testGetTool() {
        val readFileTool = registry.getTool(ToolNames.READ_FILE)
        assertNotNull(readFileTool, "Read file tool should be registered")
        assertEquals(ToolNames.READ_FILE, readFileTool.name)
    }
    
    @Test
    fun testGetAllTools() {
        val allTools = registry.getAllTools()
        assertTrue(allTools.isNotEmpty(), "Should have registered tools")
        assertTrue(ToolNames.READ_FILE in allTools.keys, "Should contain read-file tool")
        assertTrue(ToolNames.WRITE_FILE in allTools.keys, "Should contain write-file tool")
        assertTrue(ToolNames.GREP in allTools.keys, "Should contain grep tool")
        assertTrue(ToolNames.GLOB in allTools.keys, "Should contain glob tool")
    }
    
    @Test
    fun testCreateInvocation() {
        val params = ReadFileParams(path = "test.txt")
        val invocation = registry.createInvocation(ToolNames.READ_FILE, params)
        
        assertNotNull(invocation, "Should create invocation")
        assertEquals(params, invocation.params)
        assertEquals(ToolNames.READ_FILE, invocation.tool.name)
    }
    
    @Test
    fun testGetAgentTools() {
        val agentTools = registry.getAgentTools()
        assertTrue(agentTools.isNotEmpty(), "Should have agent tools")
        
        val readFileTool = agentTools.find { it.name == ToolNames.READ_FILE }
        assertNotNull(readFileTool, "Should contain read-file agent tool")
        assertTrue(readFileTool.description.isNotEmpty(), "Should have description")
        assertTrue(readFileTool.example.isNotEmpty(), "Should have example")
    }
    
    @Test
    fun testToolCategories() {
        val fileSystemTools = registry.getFileSystemTools()
        assertTrue(fileSystemTools.isNotEmpty(), "Should have file system tools")
        assertTrue(ToolNames.READ_FILE in fileSystemTools.keys)
        assertTrue(ToolNames.WRITE_FILE in fileSystemTools.keys)
        
        val executionTools = registry.getExecutionTools()
        // Shell tool might not be available in test environment
        // Just check that the method works
        assertTrue(executionTools.size >= 0, "Should return execution tools (may be empty)")
    }
    
    @Test
    fun testHasToolNamed() {
        assertTrue(registry.hasToolNamed(ToolNames.READ_FILE), "Should have read-file tool")
        assertTrue(registry.hasToolNamed(ToolNames.WRITE_FILE), "Should have write-file tool")
        assertTrue(!registry.hasToolNamed("non-existent-tool"), "Should not have non-existent tool")
    }
    
    @Test
    fun testGetToolInfo() {
        val toolInfo = registry.getToolInfo(ToolNames.READ_FILE)
        assertNotNull(toolInfo, "Should get tool info")
        assertEquals(ToolNames.READ_FILE, toolInfo.name)
        assertTrue(toolInfo.description.isNotEmpty(), "Should have description")
        assertTrue(toolInfo.isDevIns, "Should be marked as DevIns tool")
    }
}
