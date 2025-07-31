package cc.unitmesh.devti.vcs.gitignore

import cc.unitmesh.devti.settings.coder.AutoDevCoderSettingService
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test

/**
 * Tests for the GitIgnoreFlagWrapper that manages dual-engine switching.
 * These tests require a project context to test the feature flag functionality.
 */
class GitIgnoreFlagWrapperTest : BasePlatformTestCase() {
    
    @Test
    fun testEngineSwitch() {
        val wrapper = GitIgnoreFlagWrapper(project)
        
        // Test with homespun engine enabled
        project.service<AutoDevCoderSettingService>().state.enableHomeSpunGitIgnore = true
        assertEquals(IgnoreEngineFactory.EngineType.HOMESPUN, wrapper.getActiveEngineType())

        // Test with homespun engine disabled (fallback to basjes)
        project.service<AutoDevCoderSettingService>().state.enableHomeSpunGitIgnore = false
        assertEquals(IgnoreEngineFactory.EngineType.BASJES, wrapper.getActiveEngineType())
    }
    
    @Test
    fun testBasicIgnoreOperations() {
        val wrapper = GitIgnoreFlagWrapper(project)
        
        wrapper.addRule("*.log")
        wrapper.addRule("build/")
        
        assertTrue(wrapper.isIgnored("app.log"))
        assertTrue(wrapper.isIgnored("build/output.txt"))
        assertFalse(wrapper.isIgnored("app.txt"))
    }
    
    @Test
    fun testLoadFromContent() {
        val gitIgnoreContent = """
            *.class
            *.jar
            build/
            !important.jar
        """.trimIndent()
        
        val wrapper = GitIgnoreFlagWrapper(project, gitIgnoreContent)
        
        assertTrue(wrapper.isIgnored("App.class"))
        assertTrue(wrapper.isIgnored("app.jar"))
        assertTrue(wrapper.isIgnored("build/output"))
        assertFalse(wrapper.isIgnored("important.jar"))
        assertFalse(wrapper.isIgnored("App.java"))
    }
    
    @Test
    fun testRuleManagement() {
        val wrapper = GitIgnoreFlagWrapper(project)
        
        wrapper.addRule("*.tmp")
        wrapper.addRule("*.log")
        
        assertTrue(wrapper.isIgnored("file.tmp"))
        assertTrue(wrapper.isIgnored("file.log"))
        
        wrapper.removeRule("*.tmp")
        
        assertFalse(wrapper.isIgnored("file.tmp"))
        assertTrue(wrapper.isIgnored("file.log"))
        
        wrapper.clearRules()
        
        assertFalse(wrapper.isIgnored("file.log"))
    }
    
    @Test
    fun testStatistics() {
        val wrapper = GitIgnoreFlagWrapper(project)
        wrapper.addRule("*.log")
        wrapper.addRule("build/")
        
        val stats = wrapper.getStatistics()
        
        assertTrue(stats.containsKey("activeEngine"))
        assertTrue(stats.containsKey("homeSpun") || stats.containsKey("basjes"))
        
        val activeEngine = stats["activeEngine"] as String
        assertTrue(activeEngine == "HOMESPUN" || activeEngine == "BASJES")
    }
    
    @Test
    fun testEngineConsistency() {
        val wrapper = GitIgnoreFlagWrapper(project)
        
        // Add rules and test with homespun engine
        project.service<AutoDevCoderSettingService>().state.enableHomeSpunGitIgnore = true
        wrapper.addRule("*.log")
        wrapper.addRule("build/")
        
        val resultHomespun1 = wrapper.isIgnored("app.log")
        val resultHomespun2 = wrapper.isIgnored("build/output")
        val resultHomespun3 = wrapper.isIgnored("app.txt")
        
        // Switch to basjes engine and test same patterns
        project.service<AutoDevCoderSettingService>().state.enableHomeSpunGitIgnore = false
        
        val resultBasjes1 = wrapper.isIgnored("app.log")
        val resultBasjes2 = wrapper.isIgnored("build/output")
        val resultBasjes3 = wrapper.isIgnored("app.txt")
        
        // Results should be consistent between engines for basic patterns
        assertEquals("*.log pattern should be consistent", resultHomespun1, resultBasjes1)
        assertEquals("build/ pattern should be consistent", resultHomespun2, resultBasjes2)
        assertEquals("non-matching pattern should be consistent", resultHomespun3, resultBasjes3)
    }
    
    @Test
    fun testFallbackBehavior() {
        val wrapper = GitIgnoreFlagWrapper(project)
        
        // Test that wrapper handles engine failures gracefully
        // This is more of an integration test to ensure robustness
        wrapper.addRule("*.log")
        
        // Even if one engine has issues, the wrapper should still function
        assertTrue(wrapper.isIgnored("app.log"))
        assertFalse(wrapper.isIgnored("app.txt"))
    }
    
    @Test
    fun testComplexPatterns() {
        val gitIgnoreContent = """
            # Compiled output
            *.class
            *.jar

            # IDE files
            .idea/
            *.iml

            # Logs
            *.log
            !important.log

            # OS files
            .DS_Store
            Thumbs.db
        """.trimIndent()

        val wrapper = GitIgnoreFlagWrapper(project, gitIgnoreContent)

        // Test basic patterns that should work consistently
        assertTrue(wrapper.isIgnored("App.class"))
        assertTrue(wrapper.isIgnored("lib.jar"))
        assertTrue(wrapper.isIgnored(".idea/workspace.xml"))
        assertTrue(wrapper.isIgnored("project.iml"))
        assertTrue(wrapper.isIgnored("debug.log"))
        assertTrue(wrapper.isIgnored(".DS_Store"))

        assertFalse(wrapper.isIgnored("important.log"))
        assertFalse(wrapper.isIgnored("src/main/App.java"))
        assertFalse(wrapper.isIgnored("README.md"))

        // Note: Complex patterns like **/target/** may behave differently between engines
        // This is acceptable for the dual-engine architecture
    }
}
