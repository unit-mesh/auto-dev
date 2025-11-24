package cc.unitmesh.agent.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WalkthroughExtractorTest {

    @Test
    fun `should extract single complete walkthrough section`() {
        val input = """
            Some header text
            <!-- walkthrough_start -->
            This is the walkthrough content
            with multiple lines
            <!-- walkthrough_end -->
            Some footer text
        """.trimIndent()

        val result = WalkthroughExtractor.extract(input)

        assertEquals("This is the walkthrough content\nwith multiple lines", result)
    }

    @Test
    fun `should extract incomplete walkthrough section`() {
        val input = """
            Some header text
            <!-- walkthrough_start -->
            This is incomplete walkthrough content
            that continues until the end
        """.trimIndent()

        val result = WalkthroughExtractor.extract(input)

        assertEquals("This is incomplete walkthrough content\nthat continues until the end", result)
    }

    @Test
    fun `should extract multiple walkthrough sections`() {
        val input = """
            <!-- walkthrough_start -->
            First section content
            <!-- walkthrough_end -->
            Some middle text
            <!-- walkthrough_start -->
            Second section content
            <!-- walkthrough_end -->
        """.trimIndent()

        val result = WalkthroughExtractor.extract(input)

        assertEquals("First section content\n\nSecond section content", result)
    }

    @Test
    fun `should return empty string when no walkthrough markers`() {
        val input = """
            This is just regular text
            without any walkthrough markers
        """.trimIndent()

        val result = WalkthroughExtractor.extract(input)

        assertEquals("", result)
    }

    @Test
    fun `should return empty string for blank input`() {
        val result = WalkthroughExtractor.extract("")
        assertEquals("", result)

        val result2 = WalkthroughExtractor.extract("   ")
        assertEquals("", result2)
    }

    @Test
    fun `should handle walkthrough with whitespace in markers`() {
        val input = """
            <!--  walkthrough_start  -->
            Content with spaces in marker
            <!--  walkthrough_end  -->
        """.trimIndent()

        val result = WalkthroughExtractor.extract(input)

        assertEquals("Content with spaces in marker", result)
    }

    @Test
    fun `should trim walkthrough content`() {
        val input = """
            <!-- walkthrough_start -->
            
            
            Content with leading/trailing whitespace
            
            
            <!-- walkthrough_end -->
        """.trimIndent()

        val result = WalkthroughExtractor.extract(input)

        assertEquals("Content with leading/trailing whitespace", result)
    }

    @Test
    fun `should skip empty walkthrough sections`() {
        val input = """
            <!-- walkthrough_start -->
            
            
            <!-- walkthrough_end -->
            <!-- walkthrough_start -->
            Actual content
            <!-- walkthrough_end -->
        """.trimIndent()

        val result = WalkthroughExtractor.extract(input)

        assertEquals("Actual content", result)
    }

    @Test
    fun `should detect walkthrough presence`() {
        val withWalkthrough = """
            <!-- walkthrough_start -->
            Content
            <!-- walkthrough_end -->
        """.trimIndent()

        val withoutWalkthrough = "Regular text"

        assertTrue(WalkthroughExtractor.hasWalkthrough(withWalkthrough))
        assertFalse(WalkthroughExtractor.hasWalkthrough(withoutWalkthrough))
    }

    @Test
    fun `should detect incomplete walkthrough`() {
        val incomplete = """
            <!-- walkthrough_start -->
            Content without end
        """.trimIndent()

        assertTrue(WalkthroughExtractor.hasWalkthrough(incomplete))
    }

    @Test
    fun `should extract only complete walkthrough with extractComplete`() {
        val complete = """
            <!-- walkthrough_start -->
            Complete content
            <!-- walkthrough_end -->
        """.trimIndent()

        val incomplete = """
            <!-- walkthrough_start -->
            Incomplete content
        """.trimIndent()

        assertEquals("Complete content", WalkthroughExtractor.extractComplete(complete))
        assertNull(WalkthroughExtractor.extractComplete(incomplete))
    }

    @Test
    fun `should return null for empty walkthrough with extractComplete`() {
        val empty = """
            <!-- walkthrough_start -->
            
            
            <!-- walkthrough_end -->
        """.trimIndent()

        assertNull(WalkthroughExtractor.extractComplete(empty))
    }

    @Test
    fun `should handle mixed content with code blocks`() {
        val input = """
            # Analysis Results
            
            <!-- walkthrough_start -->
            ## Issue 1: Security Vulnerability
            
            ```kotlin
            // Bad code
            val password = "hardcoded"
            ```
            
            ## Issue 2: Performance Issue
            
            Use lazy initialization
            <!-- walkthrough_end -->
            
            Additional notes
        """.trimIndent()

        val result = WalkthroughExtractor.extract(input)

        assertTrue(result.contains("Security Vulnerability"))
        assertTrue(result.contains("Performance Issue"))
        assertTrue(result.contains("```kotlin"))
        assertFalse(result.contains("Additional notes"))
    }

    @Test
    fun `should handle real-world code review output`() {
        val input = """
            # Code Review Results
            
            Analyzed 3 files with 12 issues found.
            
            <!-- walkthrough_start -->
            ## 🔴 Critical Issues (Must Fix)
            
            ### File: `src/main/kotlin/Example.kt`
            
            **Line 42**: Potential null pointer exception
            - Current: `user.name.length`
            - Suggested: `user.name?.length ?: 0`
            
            ## ⚠️ Warnings
            
            ### File: `src/main/kotlin/Utils.kt`
            
            **Line 15**: Unused import statement
            - Remove: `import java.util.Random`
            
            <!-- walkthrough_end -->
            
            ## Summary
            
            Total: 2 critical, 10 warnings
        """.trimIndent()

        val result = WalkthroughExtractor.extract(input)

        assertTrue(result.contains("Critical Issues"))
        assertTrue(result.contains("Warnings"))
        assertTrue(result.contains("user.name?.length"))
        assertFalse(result.contains("Summary"))
        assertFalse(result.contains("Analyzed 3 files"))
    }

    @Test
    fun `should handle nested markers gracefully`() {
        val input = """
            <!-- walkthrough_start -->
            Outer content
            <!-- walkthrough_start -->
            This is tricky nested content
            <!-- walkthrough_end -->
        """.trimIndent()

        // Should extract from first start to first end
        val result = WalkthroughExtractor.extract(input)

        assertTrue(result.contains("Outer content"))
    }

    @Test
    fun `should preserve markdown formatting`() {
        val input = """
            <!-- walkthrough_start -->
            ## Heading
            
            - List item 1
            - List item 2
            
            **Bold** and *italic* text
            
            1. Numbered item
            2. Another item
            <!-- walkthrough_end -->
        """.trimIndent()

        val result = WalkthroughExtractor.extract(input)

        assertTrue(result.contains("## Heading"))
        assertTrue(result.contains("- List item 1"))
        assertTrue(result.contains("**Bold**"))
        assertTrue(result.contains("1. Numbered item"))
    }

    @Test
    fun `should handle Chinese content`() {
        val input = """
            <!-- walkthrough_start -->
            ## 代码审查结果
            
            ### 问题 1: 安全漏洞
            
            发现硬编码密码问题
            
            ### 问题 2: 性能问题
            
            建议使用懒加载
            <!-- walkthrough_end -->
        """.trimIndent()

        val result = WalkthroughExtractor.extract(input)

        assertTrue(result.contains("代码审查结果"))
        assertTrue(result.contains("安全漏洞"))
        assertTrue(result.contains("性能问题"))
    }

    @Test
    fun `should handle consecutive walkthrough sections`() {
        val input = """
            <!-- walkthrough_start -->
            Section 1
            <!-- walkthrough_end -->
            <!-- walkthrough_start -->
            Section 2
            <!-- walkthrough_end -->
            <!-- walkthrough_start -->
            Section 3
            <!-- walkthrough_end -->
        """.trimIndent()

        val result = WalkthroughExtractor.extract(input)

        assertEquals("Section 1\n\nSection 2\n\nSection 3", result)
    }
}
