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
    fun testGetTool() {
        val readFileTool = registry.getTool(ToolType.ReadFile.name)
        assertNotNull(readFileTool, "Read file tool should be registered")
        assertEquals(ToolType.ReadFile.name, readFileTool.name)
    }
    
    @Test
    fun testGetAllTools() {
        val allTools = registry.getAllTools()
        assertTrue(allTools.isNotEmpty(), "Should have registered tools")
        assertTrue(ToolType.ReadFile.name in allTools.keys, "Should contain read-file tool")
        assertTrue(ToolType.WriteFile.name in allTools.keys, "Should contain write-file tool")
        assertTrue(ToolType.Grep.name in allTools.keys, "Should contain grep tool")
        assertTrue(ToolType.Glob.name in allTools.keys, "Should contain glob tool")
    }
    
    @Test
    fun testCreateInvocation() {
        val params = ReadFileParams(path = "test.txt")
        val invocation = registry.createInvocation(ToolType.ReadFile.name, params)
        
        assertNotNull(invocation, "Should create invocation")
        assertEquals(params, invocation.params)
        assertEquals(ToolType.ReadFile.name, invocation.tool.name)
    }
    
    @Test
    fun testGetAgentTools() {
        val agentTools = registry.getAgentTools()
        assertTrue(agentTools.isNotEmpty(), "Should have agent tools")
        
        val readFileTool = agentTools.find { it.name == ToolType.ReadFile.name }
        assertNotNull(readFileTool, "Should contain read-file agent tool")
        assertTrue(readFileTool.description.isNotEmpty(), "Should have description")
        assertTrue(readFileTool.example.isNotEmpty(), "Should have example")
    }
    
    @Test
    fun testHasToolNamed() {
        assertTrue(registry.hasToolNamed(ToolType.ReadFile.name), "Should have read-file tool")
        assertTrue(registry.hasToolNamed(ToolType.WriteFile.name), "Should have write-file tool")
        assertTrue(!registry.hasToolNamed("non-existent-tool"), "Should not have non-existent tool")
    }
    
    @Test
    fun testGetToolInfo() {
        val toolInfo = registry.getToolInfo(ToolType.ReadFile.name)
        assertNotNull(toolInfo, "Should get tool info")
        assertEquals(ToolType.ReadFile.name, toolInfo.name)
        assertTrue(toolInfo.description.isNotEmpty(), "Should have description")
        assertTrue(toolInfo.isDevIns, "Should be marked as DevIns tool")
    }

    @Test
    fun testPlanManagementToolRegistered() {
        val planTool = registry.getTool("plan")
        assertNotNull(planTool, "Plan management tool should be registered")
        assertEquals("plan", planTool.name)
        assertTrue(planTool.description.contains("plan"), "Should have plan-related description")
    }
}
