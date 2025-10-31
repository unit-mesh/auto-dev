package cc.unitmesh.devins.compiler

import cc.unitmesh.devins.parser.DevInsParser
import cc.unitmesh.devins.workspace.WorkspaceManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * 诊断测试 - 用于检查完整的编译流程
 */
class DiagnosticTest {
    
    @Test
    fun `diagnostic - full compilation flow for read-file`() = runTest {
        val projectRoot = System.getProperty("user.dir")
        
        println("\n" + "=".repeat(100))
        println("🔍 完整诊断：/read-file:README.md")
        println("=".repeat(100))
        
        // 1. 检查工作空间
        println("\n【步骤 1】工作空间检查")
        println("项目根目录: $projectRoot")
        WorkspaceManager.openWorkspace("Diagnostic Workspace", projectRoot)
        val workspace = WorkspaceManager.currentWorkspace
        println("✅ 工作空间已打开: ${workspace?.name}")
        println("   - 根路径: ${workspace?.rootPath}")
        println("   - FileSystem: ${workspace?.fileSystem}")
        
        // 2. 检查 Lexer
        val source = "/read-file:README.md"
        println("\n【步骤 2】Lexer 分析")
        println("输入: $source")
        val lexer = cc.unitmesh.devins.lexer.DevInsLexer(source)
        val tokens = lexer.tokenize()
        println("✅ Token 列表:")
        tokens.forEach { token ->
            println("   - ${token.type}: '${token.text}' (line ${token.line}, col ${token.column})")
        }
        
        // 3. 检查 Parser
        println("\n【步骤 3】Parser 解析")
        val parser = DevInsParser(source)
        val parseResult = parser.parse()
        
        if (parseResult.isSuccess) {
            println("✅ 解析成功")
            val fileNode = parseResult.getOrNull()
            println("   - 子节点数量: ${fileNode?.children?.size}")
            fileNode?.children?.forEach { child ->
                println("   - 节点类型: ${child::class.simpleName}")
                println("     内容: ${child.getText()}")
                
                // 如果是 UsedNode，显示详细信息
                if (child is cc.unitmesh.devins.ast.DevInsUsedNode) {
                    println("     ↳ Used类型: ${child.type}")
                    println("     ↳ 标识符: ${child.identifier.getText()}")
                    println("     ↳ 子节点数量: ${child.children.size}")
                }
            }
        } else {
            val failure = parseResult as cc.unitmesh.devins.parser.ParseResult.Failure
            println("❌ 解析失败: ${failure.error.message}")
        }
        
        // 4. 检查 Compiler
        println("\n【步骤 4】Compiler 编译")
        val compileResult = DevInsCompilerFacade.compile(source)
        
        println("编译状态: ${if (compileResult.isSuccess()) "✅ 成功" else "❌ 失败"}")
        println("统计信息:")
        println("   - 命令数: ${compileResult.statistics.commandCount}")
        println("   - 变量数: ${compileResult.statistics.variableCount}")
        println("   - 代理数: ${compileResult.statistics.agentCount}")
        println("   - 节点数: ${compileResult.statistics.nodeCount}")
        println("   - 是否本地命令: ${compileResult.isLocalCommand}")
        println("   - 是否有错误: ${compileResult.hasError}")
        
        if (compileResult.hasError) {
            println("\n❌ 错误信息: ${compileResult.errorMessage}")
        }
        
        println("\n输出结果:")
        println("   长度: ${compileResult.output.length} 字符")
        println("   内容: ${compileResult.output}")
        
        // 5. 检查处理器链
        println("\n【步骤 5】处理器链检查")
        val compiler = cc.unitmesh.devins.compiler.DevInsCompiler()
        println("✅ Compiler 已创建")
        
        println("\n" + "=".repeat(100))
        println("🎯 诊断完成")
        println("=".repeat(100))
    }
    
    @Test
    fun `diagnostic - compare read-file vs file`() = runTest {
        val projectRoot = System.getProperty("user.dir")
        WorkspaceManager.openWorkspace("Diagnostic Workspace", projectRoot)
        
        println("\n" + "=".repeat(80))
        println("🔍 对比测试: /read-file vs /file")
        println("=".repeat(80))
        
        // 测试 /read-file
        println("\n【测试 1】/read-file:README.md")
        val result1 = DevInsCompilerFacade.compile("/read-file:README.md")
        println("✅ 状态: ${if (result1.isSuccess()) "成功" else "失败"}")
        println("   输出: ${result1.output}")
        println("   命令数: ${result1.statistics.commandCount}")
        
        // 测试 /file
        println("\n【测试 2】/file:README.md")
        val result2 = DevInsCompilerFacade.compile("/file:README.md")
        println("✅ 状态: ${if (result2.isSuccess()) "成功" else "失败"}")
        println("   输出: ${result2.output}")
        println("   命令数: ${result2.statistics.commandCount}")
        
        // 对比结果
        println("\n【对比结果】")
        println("输出是否相同: ${result1.output == result2.output}")
        println("命令数是否相同: ${result1.statistics.commandCount == result2.statistics.commandCount}")
        
        if (result1.output != result2.output) {
            println("\n❌ 输出不同！")
            println("read-file: ${result1.output}")
            println("file: ${result2.output}")
        } else {
            println("\n✅ 两个命令产生相同的输出")
        }
        
        println("\n" + "=".repeat(80))
    }
    
    @Test
    fun `diagnostic - without workspace`() = runTest {
        println("\n" + "=".repeat(80))
        println("🔍 测试：没有工作空间的情况")
        println("=".repeat(80))
        
        // 不打开工作空间，直接编译
        val result = DevInsCompilerFacade.compile("/read-file:README.md")
        
        println("✅ 状态: ${if (result.isSuccess()) "成功" else "失败"}")
        println("   输出: ${result.output}")
        println("   命令数: ${result.statistics.commandCount}")
        println("   是否有错误: ${result.hasError}")
        
        if (result.hasError) {
            println("   错误信息: ${result.errorMessage}")
        }
        
        println("\n💡 注意：没有工作空间时，命令仍然应该被识别，但可能无法读取文件内容")
        println("=".repeat(80))
    }
}

