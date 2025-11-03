package cc.unitmesh.agent.tool.gitignore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitIgnoreFilterTest {
    
    @Test
    fun testSimplePattern() {
        val filter = DefaultGitIgnoreFilter()
        filter.addPattern("*.log")
        
        assertTrue(filter.isIgnored("test.log"))
        assertTrue(filter.isIgnored("error.log"))
        assertFalse(filter.isIgnored("test.txt"))
        assertFalse(filter.isIgnored("log"))
    }
    
    @Test
    fun testDirectoryPattern() {
        val filter = DefaultGitIgnoreFilter()
        filter.addPattern("node_modules/")
        
        assertTrue(filter.isIgnored("node_modules/"))
        assertTrue(filter.isIgnored("node_modules/package"))
        assertFalse(filter.isIgnored("node_modules"))
    }
    
    @Test
    fun testRecursivePattern() {
        val filter = DefaultGitIgnoreFilter()
        filter.addPattern("**/build")
        
        assertTrue(filter.isIgnored("build"))
        assertTrue(filter.isIgnored("src/build"))
        assertTrue(filter.isIgnored("src/main/build"))
        assertFalse(filter.isIgnored("builder"))
    }
    
    @Test
    fun testAnchoredPattern() {
        val filter = DefaultGitIgnoreFilter()
        filter.addPattern("/config.json")
        
        assertTrue(filter.isIgnored("config.json"))
        assertFalse(filter.isIgnored("src/config.json"))
        assertFalse(filter.isIgnored("test/config.json"))
    }
    
    @Test
    fun testNegationPattern() {
        val filter = DefaultGitIgnoreFilter()
        filter.addPattern("*.log")
        filter.addPattern("!important.log")
        
        assertTrue(filter.isIgnored("test.log"))
        assertTrue(filter.isIgnored("error.log"))
        assertFalse(filter.isIgnored("important.log"))
    }
    
    @Test
    fun testWildcardPattern() {
        val filter = DefaultGitIgnoreFilter()
        filter.addPattern("test?.txt")
        
        assertTrue(filter.isIgnored("test1.txt"))
        assertTrue(filter.isIgnored("testa.txt"))
        assertFalse(filter.isIgnored("test.txt"))
        assertFalse(filter.isIgnored("test12.txt"))
    }
    
    @Test
    fun testCharacterClassPattern() {
        val filter = DefaultGitIgnoreFilter()
        filter.addPattern("test[0-9].txt")
        
        assertTrue(filter.isIgnored("test0.txt"))
        assertTrue(filter.isIgnored("test5.txt"))
        assertTrue(filter.isIgnored("test9.txt"))
        assertFalse(filter.isIgnored("testa.txt"))
        assertFalse(filter.isIgnored("test.txt"))
    }
    
    @Test
    fun testCommentAndEmptyLines() {
        val filter = DefaultGitIgnoreFilter()
        filter.addPattern("# This is a comment")
        filter.addPattern("")
        filter.addPattern("   ")
        filter.addPattern("*.log")
        
        assertTrue(filter.isIgnored("test.log"))
        assertFalse(filter.isIgnored("# This is a comment"))
    }
    
    @Test
    fun testPathNormalization() {
        val normalized1 = GitIgnorePatternMatcher.normalizePath("./src/main.kt")
        assertEquals("src/main.kt", normalized1)

        val normalized2 = GitIgnorePatternMatcher.normalizePath("/src/main.kt")
        assertEquals("src/main.kt", normalized2)

        val normalized3 = GitIgnorePatternMatcher.normalizePath("src\\main.kt")
        assertEquals("src/main.kt", normalized3)

        // Trailing slash is kept to indicate directories
        val normalized4 = GitIgnorePatternMatcher.normalizePath("src/main.kt/")
        assertEquals("src/main.kt/", normalized4)
    }
    
    @Test
    fun testComplexPatterns() {
        val filter = DefaultGitIgnoreFilter()
        filter.addPattern("*.class")
        filter.addPattern("target/")
        filter.addPattern("!important.class")
        filter.addPattern("/config.properties")
        filter.addPattern("**/temp")
        
        // Test *.class
        assertTrue(filter.isIgnored("Main.class"))
        assertTrue(filter.isIgnored("src/Main.class"))
        
        // Test negation
        assertFalse(filter.isIgnored("important.class"))
        
        // Test directory
        assertTrue(filter.isIgnored("target/"))
        assertTrue(filter.isIgnored("target/classes"))
        
        // Test anchored
        assertTrue(filter.isIgnored("config.properties"))
        assertFalse(filter.isIgnored("src/config.properties"))
        
        // Test recursive
        assertTrue(filter.isIgnored("temp"))
        assertTrue(filter.isIgnored("src/temp"))
        assertTrue(filter.isIgnored("src/main/temp"))
    }
    
    @Test
    fun testParseGitIgnoreContent() {
        val content = """
            # Build outputs
            *.class
            *.jar
            
            # Directories
            target/
            build/
            
            # Keep important files
            !important.jar
            
            # Config
            /local.properties
        """.trimIndent()
        
        val filter = parseGitIgnoreContent(content)
        
        assertTrue(filter.isIgnored("Test.class"))
        assertTrue(filter.isIgnored("app.jar"))
        assertFalse(filter.isIgnored("important.jar"))
        assertTrue(filter.isIgnored("target/"))
        assertTrue(filter.isIgnored("build/classes"))
        assertTrue(filter.isIgnored("local.properties"))
        assertFalse(filter.isIgnored("src/local.properties"))
    }
    
    @Test
    fun testDoubleStarPattern() {
        val filter = DefaultGitIgnoreFilter()
        filter.addPattern("**/node_modules")
        
        assertTrue(filter.isIgnored("node_modules"))
        assertTrue(filter.isIgnored("src/node_modules"))
        assertTrue(filter.isIgnored("src/main/node_modules"))
        assertTrue(filter.isIgnored("a/b/c/node_modules"))
    }
    
    @Test
    fun testDoubleStarWithSuffix() {
        val filter = DefaultGitIgnoreFilter()
        filter.addPattern("**/*.log")
        
        assertTrue(filter.isIgnored("test.log"))
        assertTrue(filter.isIgnored("src/test.log"))
        assertTrue(filter.isIgnored("src/main/test.log"))
        assertFalse(filter.isIgnored("test.txt"))
    }
    
    @Test
    fun testGitDirectory() {
        val filter = DefaultGitIgnoreFilter()
        filter.addPattern(".git")
        filter.addPattern(".git/")
        
        assertTrue(filter.isIgnored(".git"))
        assertTrue(filter.isIgnored(".git/"))
        assertTrue(filter.isIgnored(".git/config"))
    }
    
    @Test
    fun testMultiplePatternOrdering() {
        val filter = DefaultGitIgnoreFilter()
        filter.addPattern("*.log")
        filter.addPattern("!important.log")
        filter.addPattern("very-important.log")
        
        // First pattern ignores all .log files
        // Second pattern un-ignores important.log
        // Third pattern should be treated as a file to ignore (not a negation)
        assertTrue(filter.isIgnored("test.log"))
        assertFalse(filter.isIgnored("important.log"))
        assertTrue(filter.isIgnored("very-important.log"))
    }
}

