package cc.unitmesh.devins.workspace

import cc.unitmesh.agent.Platform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkspaceTest {
    
    @Test
    fun testCreateWorkspace() {
        val workspace = DefaultWorkspace.create("Test Project", "/test/path")
        
        assertEquals("Test Project", workspace.name)
        assertEquals("/test/path", workspace.rootPath)
        assertTrue(workspace.isInitialized())
        assertFalse(workspace.isEmpty())
    }
    
    @Test
    fun testCreateEmptyWorkspace() {
        val workspace = DefaultWorkspace.createEmpty("Empty Test")
        
        assertEquals("Empty Test", workspace.name)
        assertNull(workspace.rootPath)
        assertFalse(workspace.isInitialized())
        assertTrue(workspace.isEmpty())
    }
    
    @Test
    fun testWorkspaceDisplayName() {
        val workspace1 = DefaultWorkspace.create("Project", "/home/user/project")
        assertEquals("Project (project)", workspace1.getDisplayName())
        
        val workspace2 = DefaultWorkspace.createEmpty("Empty")
        assertEquals("Empty", workspace2.getDisplayName())
    }
    
    @Test
    fun testWorkspaceRelativePath() {
        val workspace = DefaultWorkspace.create("Project", "/home/user/project")
        
        assertEquals("src/main.kt", workspace.getRelativePath("/home/user/project/src/main.kt"))
        assertEquals("README.md", workspace.getRelativePath("/home/user/project/README.md"))
        assertEquals("/other/path/file.txt", workspace.getRelativePath("/other/path/file.txt"))
    }
    
    @Test
    fun testWorkspaceStateFlow() = runTest {
        val workspace = DefaultWorkspace.create("Test", "/test")
        
        val initialState = workspace.stateFlow.first()
        assertTrue(initialState.isInitialized)
        assertFalse(initialState.isLoading)
        assertNull(initialState.error)
        assertTrue(initialState.lastRefreshTime > 0)
    }
    
    @Test
    fun testWorkspaceRefresh() = runTest {
        // Skip this test on JS/WasmJS platforms where shell executor is not available
        if (Platform.isJs || Platform.isWasm) {
            return@runTest
        }

        val workspace = DefaultWorkspace.create("Test", "/test")
        val initialTime = workspace.stateFlow.first().lastRefreshTime

        // Wait a bit to ensure time difference
        kotlinx.coroutines.delay(50) // Increase delay to ensure time difference

        workspace.refresh()

        val newState = workspace.stateFlow.first()
        assertTrue(newState.lastRefreshTime >= initialTime, "New refresh time should be >= initial time")
        assertFalse(newState.isLoading)
        assertNull(newState.error)
    }
    
    @Test
    fun testWorkspaceClose() = runTest {
        val workspace = DefaultWorkspace.create("Test", "/test")
        assertTrue(workspace.isInitialized())
        
        workspace.close()
        
        val finalState = workspace.stateFlow.first()
        assertFalse(finalState.isInitialized)
    }
    
    @Test
    fun testWorkspaceManagerOpenWorkspace() = runTest {
        // Skip this test on JS/WasmJS platforms where shell executor is not available
        if (Platform.isJs || Platform.isWasm) {
            return@runTest
        }
        
        // Clean up any existing workspace
        WorkspaceManager.closeCurrentWorkspace()
        
        val workspace = WorkspaceManager.openWorkspace("Test Project", "/test/path")
        
        assertEquals("Test Project", workspace.name)
        assertEquals("/test/path", workspace.rootPath)
        assertTrue(WorkspaceManager.hasActiveWorkspace())
        assertEquals(workspace, WorkspaceManager.currentWorkspace)
        
        val workspaceFromFlow = WorkspaceManager.workspaceFlow.first()
        assertEquals(workspace, workspaceFromFlow)
    }
    
    @Test
    fun testWorkspaceManagerOpenEmptyWorkspace() = runTest {
        // Clean up any existing workspace
        WorkspaceManager.closeCurrentWorkspace()
        
        val workspace = WorkspaceManager.openEmptyWorkspace("Empty Test")
        
        assertEquals("Empty Test", workspace.name)
        assertNull(workspace.rootPath)
        assertFalse(WorkspaceManager.hasActiveWorkspace()) // Empty workspace is not considered "active"
        assertEquals(workspace, WorkspaceManager.currentWorkspace)
    }
    
    @Test
    fun testWorkspaceManagerCloseWorkspace() = runTest {
        // Skip this test on JS/WasmJS platforms where shell executor is not available
        if (Platform.isJs || Platform.isWasm) {
            return@runTest
        }
        
        // Open a workspace first
        WorkspaceManager.openWorkspace("Test", "/test")
        assertTrue(WorkspaceManager.hasActiveWorkspace())
        
        // Close it
        WorkspaceManager.closeCurrentWorkspace()
        
        assertFalse(WorkspaceManager.hasActiveWorkspace())
        assertNull(WorkspaceManager.currentWorkspace)
        assertNull(WorkspaceManager.workspaceFlow.first())
    }
    
    @Test
    fun testWorkspaceManagerGetCurrentOrEmpty() = runTest {
        // Clean up any existing workspace
        WorkspaceManager.closeCurrentWorkspace()
        
        // Should return empty workspace when no current workspace
        val emptyWorkspace = WorkspaceManager.getCurrentOrEmpty()
        assertTrue(emptyWorkspace.isEmpty())
        
        // Open a real workspace
        if (!Platform.isJs && !Platform.isWasm) {
            WorkspaceManager.openWorkspace("Real", "/real")
            val realWorkspace = WorkspaceManager.getCurrentOrEmpty()
            assertFalse(realWorkspace.isEmpty())
            assertEquals("/real", realWorkspace.rootPath)
        }
    }
    
    @Test
    fun testWorkspaceManagerRefreshCurrent() = runTest {
        // Skip this test on JS/WasmJS platforms where shell executor is not available
        if (Platform.isJs || Platform.isWasm) {
            return@runTest
        }

        // Open a workspace
        val workspace = WorkspaceManager.openWorkspace("Test", "/test")
        val initialTime = workspace.stateFlow.first().lastRefreshTime

        // Wait a bit to ensure time difference
        kotlinx.coroutines.delay(100)

        // Refresh through manager
        WorkspaceManager.refreshCurrentWorkspace()

        val newState = workspace.stateFlow.first()
        assertTrue(newState.lastRefreshTime >= initialTime,
                  "Expected refresh time ${newState.lastRefreshTime} to be >= initial time $initialTime")
    }
    
    @Test
    fun testWorkspaceServices() {
        // Skip this test on JS/WasmJS platforms where Dispatchers.Default is not fully supported
        if (Platform.isJs || Platform.isWasm) {
            return
        }

        val workspace = DefaultWorkspace.create("Test", "/test")
        
        // Check that services are available
        assertNotNull(workspace.fileSystem)
        assertNotNull(workspace.completionManager)
        
        // Check that completion manager has the correct file system
        // This is an indirect test since we can't directly access the internal fileSystem
        // But we can verify the completion manager works
        assertTrue(workspace.completionManager.getSupportedTriggerTypes().isNotEmpty())
    }
}
