package cc.unitmesh.devins.compiler

import cc.unitmesh.devins.workspace.WorkspaceManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class ReadFileCommandTest {
    
    @Test
    fun `test read-file command is recognized`() = runTest {
        val projectRoot = System.getProperty("user.dir")

        WorkspaceManager.openWorkspace("Test Workspace", projectRoot)

        val source = "/read-file:README.md"
        
        val result = DevInsCompilerFacade.compile(source)

        assertTrue(result.isSuccess(), "编译应该成功")
        assertEquals(1, result.statistics.commandCount, "应该处理1个命令")
        assertTrue(result.output.contains("## file: README.md"), 
            "输出应该包含文件标题，实际输出: ${result.output}")
        assertTrue(result.output.contains("```"), 
            "输出应该包含代码块标记，实际输出: ${result.output}")
    }
    
    @Test
    fun `test file command (original) still works`() = runTest {
        val projectRoot = System.getProperty("user.dir")
        WorkspaceManager.openWorkspace("Test Workspace", projectRoot)
        
        val source = "/file:README.md"
        val result = DevInsCompilerFacade.compile(source)
        
        assertTrue(result.isSuccess(), "编译应该成功")
        assertEquals(1, result.statistics.commandCount, "应该处理1个命令")
        assertTrue(result.output.contains("## file: README.md"), "输出应该包含文件标题")
        assertTrue(result.output.contains("```"), "输出应该包含代码块标记")
    }
    
    @Test
    fun `test read-file and file commands are equivalent`() = runTest {
        val projectRoot = System.getProperty("user.dir")
        WorkspaceManager.openWorkspace("Test Workspace", projectRoot)
        
        val result1 = DevInsCompilerFacade.compile("/read-file:test.txt")
        val result2 = DevInsCompilerFacade.compile("/file:test.txt")

        // 两个命令应该产生相同的输出
        assertEquals(result1.output, result2.output, "read-file 和 file 应该产生相同的输出")
    }
}

