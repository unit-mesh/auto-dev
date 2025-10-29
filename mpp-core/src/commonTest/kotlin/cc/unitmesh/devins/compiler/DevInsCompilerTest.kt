package cc.unitmesh.devins.compiler

import cc.unitmesh.devins.compiler.context.CompilerOptions
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * DevIns 编译器测试
 */
class DevInsCompilerTest {
    
    @Test
    fun testBasicCreation() {
        // 基本的创建测试，不涉及协程
        val compiler = DevInsCompiler.create()
        assertTrue(true, "Compiler should be created successfully")
    }
    
    @Test
    fun testSimpleTextCompilation() = runTest {
        val source = "Hello, World!"
        val result = DevInsCompilerFacade.compile(source)
        
        assertTrue(result.isSuccess(), "Compilation should succeed")
        assertEquals("Hello, World!", result.output)
    }
    
    @Test
    fun testVariableCompilation() = runTest {
        val source = "Hello, \$name!"
        val variables = mapOf("name" to "DevIns")
        val result = DevInsCompilerFacade.compile(source, variables)
        
        assertTrue(result.isSuccess(), "Compilation should succeed")
        assertEquals("Hello, DevIns!", result.output)
    }
    
    @Test
    fun testComplexVariableCompilation() = runTest {
        val source = "Hello, \$name! Welcome to \$project."
        val variables = mapOf("name" to "Alice", "project" to "TestProject")
        val result = DevInsCompilerFacade.compile(source, variables)
        
        assertTrue(result.isSuccess(), "Compilation should succeed")
        assertEquals("Hello, Alice! Welcome to TestProject.", result.output)
        assertEquals(2, result.statistics.variableCount)
    }
    
    @Test
    fun testCompileToString() = runTest {
        val source = "Hello, \$name!"
        val variables = mapOf("name" to "World")
        val output = DevInsCompilerFacade.compileToString(source, variables)
        
        assertEquals("Hello, World!", output)
    }
    
    @Test
    fun testCompilerBuilder() = runTest {
        val source = "Debug: \$debug"
        val result = DevInsCompilerFacade.builder()
            .debug(true)
            .variable("debug", "enabled")
            .compile(source)

        assertTrue(result.isSuccess(), "Compilation should succeed")
        assertEquals("Debug: enabled", result.output)
    }

    @Test
    fun testEdgeCaseVariablesStartingWithVariable() = runTest {
        val source = "\$var1 and \$var2 and \$var3"
        val variables = mapOf("var1" to "First", "var2" to "Second", "var3" to "Third")
        val result = DevInsCompilerFacade.compile(source, variables)

        assertTrue(result.isSuccess(), "Compilation should succeed")
        assertEquals("First and Second and Third", result.output)
        assertEquals(3, result.statistics.variableCount)
    }

    @Test
    fun testEdgeCaseMultipleVariablesWithText() = runTest {
        val source = "Multiple \$a, \$b, \$c variables."
        val variables = mapOf("a" to "A", "b" to "B", "c" to "C")
        val result = DevInsCompilerFacade.compile(source, variables)

        assertTrue(result.isSuccess(), "Compilation should succeed")
        assertEquals("Multiple A, B, C variables.", result.output)
        assertEquals(3, result.statistics.variableCount)
    }

    @Test
    fun testComplexVariablePatterns() = runTest {
        val source = "\$start text \$middle more text \$end"
        val variables = mapOf("start" to "Begin", "middle" to "Center", "end" to "Finish")
        val result = DevInsCompilerFacade.compile(source, variables)

        assertTrue(result.isSuccess(), "Compilation should succeed")
        assertEquals("Begin text Center more text Finish", result.output)
        assertEquals(3, result.statistics.variableCount)
    }
}
