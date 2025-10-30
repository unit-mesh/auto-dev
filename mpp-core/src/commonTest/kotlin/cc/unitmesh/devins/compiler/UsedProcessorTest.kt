package cc.unitmesh.devins.compiler

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 测试 UsedProcessor 处理器
 * 验证统一的命令、代理和变量处理入口点
 */
class UsedProcessorTest {
    
    @Test
    fun testCommandProcessing() = runTest {
        // 测试命令处理（如 /file:example.kt）
        val source = "/file:test.kt"
        val result = DevInsCompilerFacade.compile(source)
        
        assertTrue(result.isSuccess(), "Command processing should succeed")
        assertTrue(result.statistics.commandCount > 0, "Should have processed at least one command")
    }
    
    @Test
    fun testVariableProcessing() = runTest {
        // 测试变量处理（如 $variable）
        val source = "Hello, ${'$'}name!"
        val variables = mapOf("name" to "World")
        val result = DevInsCompilerFacade.compile(source, variables)
        
        assertTrue(result.isSuccess(), "Variable processing should succeed")
        assertEquals("Hello, World!", result.output)
        assertEquals(1, result.statistics.variableCount)
    }
    
    @Test
    fun testAgentProcessing() = runTest {
        // 测试代理处理（如 @agent）
        val source = "@helper"
        val result = DevInsCompilerFacade.compile(source)
        
        assertTrue(result.isSuccess(), "Agent processing should succeed")
        assertTrue(result.statistics.agentCount > 0, "Should have processed at least one agent")
    }
    
    @Test
    fun testMixedProcessing() = runTest {
        // 测试混合处理：命令 + 变量 + 代理
        val source = """
            @helper
            /file:${'$'}filename
            Process ${'$'}input
        """.trimIndent()
        
        val variables = mapOf(
            "filename" to "test.kt",
            "input" to "data"
        )
        
        val result = DevInsCompilerFacade.compile(source, variables)
        
        assertTrue(result.isSuccess(), "Mixed processing should succeed")
        assertTrue(result.statistics.agentCount > 0, "Should have processed agent")
        assertTrue(result.statistics.commandCount > 0, "Should have processed command")
        assertTrue(result.statistics.variableCount > 0, "Should have processed variables")
    }
    
    @Test
    fun testReadFileCommand() = runTest {
        // 测试原始问题：/read-file:.devin/gandalf.devin
        val source = "/read-file:.devin/gandalf.devin"
        val result = DevInsCompilerFacade.compile(source)
        
        // 命令应该被处理（即使文件不存在，处理器也应该运行）
        assertTrue(result.isSuccess() || result.hasError, "Command should be processed")
        assertTrue(result.statistics.commandCount > 0, "Should have processed read-file command")
    }
    
    @Test
    fun testMultipleCommandsInSequence() = runTest {
        // 测试多个连续命令
        val source = """
            /file:first.kt
            /file:second.kt
            /file:third.kt
        """.trimIndent()
        
        val result = DevInsCompilerFacade.compile(source)
        
        assertTrue(result.isSuccess(), "Multiple commands should be processed")
        assertTrue(result.statistics.commandCount >= 3, "Should have processed at least 3 commands")
    }
    
    @Test
    fun testCommandWithVariableArgument() = runTest {
        // 测试命令参数中包含变量
        val source = "/file:${'$'}targetFile"
        val variables = mapOf("targetFile" to "example.kt")
        val result = DevInsCompilerFacade.compile(source, variables)
        
        // 命令应该被处理（变量可能在命令参数中）
        assertTrue(result.statistics.commandCount > 0, "Should have processed command")
    }
}

