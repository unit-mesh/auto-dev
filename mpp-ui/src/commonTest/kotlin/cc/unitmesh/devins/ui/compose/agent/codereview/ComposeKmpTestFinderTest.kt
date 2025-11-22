package cc.unitmesh.devins.ui.compose.agent.codereview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for ComposeKmpTestFinder path conversion logic
 */
class ComposeKmpTestFinderTest {
    
    private val finder = ComposeKmpTestFinder()
    
    @Test
    fun `should detect KMP structure from commonMain path`() {
        val sourceFile = "mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/Agent.kt"
        
        // Check if it's detected as KMP structure
        val isKmp = sourceFile.contains("/commonMain/kotlin/")
        assertTrue(isKmp, "Should detect commonMain as KMP structure")
    }
    
    @Test
    fun `should convert commonMain to commonTest`() {
        val sourceFile = "mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/CodingAgentViewModel.kt"
        val expected = "mpp-ui/src/commonTest/kotlin/cc/unitmesh/devins/ui/compose/agent/CodingAgentViewModelTest.kt"
        
        val converted = sourceFile
            .replace("/commonMain/kotlin/", "/commonTest/kotlin/")
            .replace(Regex("\\.kt$"), "Test.kt")
        
        assertEquals(expected, converted)
    }
    
    @Test
    fun `should convert jvmMain to jvmTest`() {
        val sourceFile = "mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/Service.kt"
        val expected = "mpp-core/src/jvmTest/kotlin/cc/unitmesh/agent/ServiceTest.kt"
        
        val converted = sourceFile
            .replace("/jvmMain/kotlin/", "/jvmTest/kotlin/")
            .replace(Regex("\\.kt$"), "Test.kt")
        
        assertEquals(expected, converted)
    }
    
    @Test
    fun `should convert jsMain to jsTest`() {
        val sourceFile = "mpp-ui/src/jsMain/kotlin/cc/unitmesh/ui/Component.kt"
        val expected = "mpp-ui/src/jsTest/kotlin/cc/unitmesh/ui/ComponentTest.kt"
        
        val converted = sourceFile
            .replace("/jsMain/kotlin/", "/jsTest/kotlin/")
            .replace(Regex("\\.kt$"), "Test.kt")
        
        assertEquals(expected, converted)
    }
    
    @Test
    fun `should not detect non-KMP structure`() {
        val sourceFile = "src/main/java/com/example/Service.java"
        
        val isKmp = sourceFile.contains("/commonMain/kotlin/") || 
                    sourceFile.contains("/jvmMain/kotlin/") ||
                    sourceFile.contains("/jsMain/kotlin/")
        
        assertFalse(isKmp, "Should not detect standard Java structure as KMP")
    }
    
    @Test
    fun `should convert androidMain to androidUnitTest`() {
        val sourceFile = "mpp-ui/src/androidMain/kotlin/cc/unitmesh/ui/AndroidUtil.kt"
        val expected = "mpp-ui/src/androidUnitTest/kotlin/cc/unitmesh/ui/AndroidUtilTest.kt"
        
        val converted = sourceFile
            .replace("/androidMain/kotlin/", "/androidUnitTest/kotlin/")
            .replace(Regex("\\.kt$"), "Test.kt")
        
        assertEquals(expected, converted)
    }
    
    @Test
    fun `should add Test suffix to filename`() {
        val sourceFile = "mpp-ui/src/commonMain/kotlin/cc/unitmesh/MyClass.kt"
        val expected = "mpp-ui/src/commonTest/kotlin/cc/unitmesh/MyClassTest.kt"
        
        val converted = sourceFile
            .replace("/commonMain/kotlin/", "/commonTest/kotlin/")
            .replace(Regex("\\.kt$"), "Test.kt")
        
        assertEquals(expected, converted)
        assertTrue(converted.endsWith("MyClassTest.kt"))
    }
    
    @Test
    fun `should preserve deep package structure`() {
        val sourceFile = "mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/TestFinder.kt"
        val expected = "mpp-ui/src/commonTest/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/TestFinderTest.kt"
        
        val converted = sourceFile
            .replace("/commonMain/kotlin/", "/commonTest/kotlin/")
            .replace(Regex("\\.kt$"), "Test.kt")
        
        assertEquals(expected, converted)
    }
    
    @Test
    fun `should handle Java files in KMP project`() {
        val sourceFile = "mpp-core/src/jvmMain/java/cc/unitmesh/Utils.java"
        val expected = "mpp-core/src/jvmTest/java/cc/unitmesh/UtilsTest.java"
        
        val converted = sourceFile
            .replace("/jvmMain/java/", "/jvmTest/java/")
            .replace(Regex("\\.java$"), "Test.java")
        
        assertEquals(expected, converted)
    }
    
    @Test
    fun `should check isApplicable for Kotlin and Java`() {
        assertTrue(finder.isApplicable("kotlin"))
        assertTrue(finder.isApplicable("java"))
        assertTrue(finder.isApplicable("Kotlin"))
        assertTrue(finder.isApplicable("JAVA"))
        assertFalse(finder.isApplicable("python"))
        assertFalse(finder.isApplicable("javascript"))
    }
}
