package cc.unitmesh.devins.compiler

import cc.unitmesh.devins.workspace.WorkspaceManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 测试 read-file 命令功能
 */
class ReadFileCommandTest {
    
    @Test
    fun `test read-file command is recognized`() = runTest {
        // 使用项目根目录作为工作空间
        val projectRoot = System.getProperty("user.dir")
        println("\n" + "=".repeat(80))
        println("测试 /read-file:README.md 命令")
        println("=".repeat(80))
        println("📁 项目根目录: $projectRoot")
        
        WorkspaceManager.openWorkspace("Test Workspace", projectRoot)
        println("✅ 工作空间已打开")
        
        val source = "/read-file:README.md"
        println("📝 输入: $source")
        
        val result = DevInsCompilerFacade.compile(source)
        
        println("\n✅ 编译状态: ${if (result.isSuccess()) "成功" else "失败"}")
        println("📊 统计:")
        println("   - 命令数: ${result.statistics.commandCount}")
        println("   - 变量数: ${result.statistics.variableCount}")
        println("   - 代理数: ${result.statistics.agentCount}")
        println("   - 节点数: ${result.statistics.nodeCount}")
        println("\n📄 输出长度: ${result.output.length}")
        println("📄 输出内容:")
        println(result.output)
        
        if (result.hasError) {
            println("\n❌ 有错误:")
            println("   错误消息: ${result.errorMessage}")
        }
        println("=".repeat(80))
        
        // 验证命令被识别和处理
        // MPP 版本使用 TemplateCompiler 会实际读取文件内容
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
        
        println("\n测试 /file:README.md 命令 (原始命令)")
        println("✅ 编译状态: ${if (result.isSuccess()) "成功" else "失败"}")
        println("📄 输出: ${result.output}")
        
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
        
        println("\n测试 read-file 和 file 命令等价性")
        println("read-file 输出: ${result1.output}")
        println("file 输出: ${result2.output}")
        
        // 两个命令应该产生相同的输出
        assertEquals(result1.output, result2.output, "read-file 和 file 应该产生相同的输出")
    }
}

