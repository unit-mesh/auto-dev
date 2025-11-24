package cc.unitmesh.devins.ui.compose.agent.codereview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for PlanPriority utility functions
 * Verifies priority adjustment logic for code style issues
 */
class PlanPriorityTest {
    
    @Test
    fun `should identify code style issues`() {
        assertTrue(PlanPriority.isCodeStyleIssue("代码风格问题"))
        assertTrue(PlanPriority.isCodeStyleIssue("Code Style Issues"))
        assertTrue(PlanPriority.isCodeStyleIssue("字符串格式化问题"))
        assertTrue(PlanPriority.isCodeStyleIssue("String Formatting"))
        assertTrue(PlanPriority.isCodeStyleIssue("格式化问题"))
        assertTrue(PlanPriority.isCodeStyleIssue("Formatting Issues"))
        assertTrue(PlanPriority.isCodeStyleIssue("代码风格"))
        assertTrue(PlanPriority.isCodeStyleIssue("Style"))
        
        assertFalse(PlanPriority.isCodeStyleIssue("Null Safety Issues"))
        assertFalse(PlanPriority.isCodeStyleIssue("性能问题"))
        assertFalse(PlanPriority.isCodeStyleIssue("Security Vulnerability"))
    }
    
    @Test
    fun `should downgrade code style issues from CRITICAL to MEDIUM`() {
        val adjusted = PlanPriority.adjustPriority("代码风格问题", "CRITICAL")
        assertEquals("MEDIUM", adjusted)
    }
    
    @Test
    fun `should downgrade code style issues from HIGH to MEDIUM`() {
        val adjusted = PlanPriority.adjustPriority("Code Style Issues", "HIGH")
        assertEquals("MEDIUM", adjusted)
    }
    
    @Test
    fun `should not change priority for non-code-style issues`() {
        assertEquals("CRITICAL", PlanPriority.adjustPriority("Null Safety Issues", "CRITICAL"))
        assertEquals("HIGH", PlanPriority.adjustPriority("Performance Issues", "HIGH"))
        assertEquals("MEDIUM", PlanPriority.adjustPriority("Code Quality", "MEDIUM"))
    }
    
    @Test
    fun `should not change priority if already MEDIUM`() {
        val adjusted = PlanPriority.adjustPriority("代码风格问题", "MEDIUM")
        assertEquals("MEDIUM", adjusted)
    }
    
    @Test
    fun `should identify high priority correctly`() {
        assertTrue(PlanPriority.isHighPriority("CRITICAL"))
        assertTrue(PlanPriority.isHighPriority("关键"))
        assertTrue(PlanPriority.isHighPriority("HIGH"))
        assertTrue(PlanPriority.isHighPriority("高"))
        
        assertFalse(PlanPriority.isHighPriority("MEDIUM"))
        assertFalse(PlanPriority.isHighPriority("中等"))
        assertFalse(PlanPriority.isHighPriority("LOW"))
        assertFalse(PlanPriority.isHighPriority("低"))
    }
    
    @Test
    fun `should get adjusted priority for plan item`() {
        val codeStyleItem = PlanItem(
            number = 1,
            title = "代码风格问题",
            priority = "CRITICAL",
            steps = emptyList()
        )
        assertEquals("MEDIUM", PlanPriority.getAdjustedPriority(codeStyleItem))
        
        val normalItem = PlanItem(
            number = 2,
            title = "Null Safety Issues",
            priority = "CRITICAL",
            steps = emptyList()
        )
        assertEquals("CRITICAL", PlanPriority.getAdjustedPriority(normalItem))
    }
    
    @Test
    fun `should handle case-insensitive matching`() {
        assertTrue(PlanPriority.isCodeStyleIssue("CODE STYLE"))
        assertTrue(PlanPriority.isCodeStyleIssue("code style"))
        assertTrue(PlanPriority.isCodeStyleIssue("Code Style"))
        
        assertTrue(PlanPriority.isHighPriority("critical"))
        assertTrue(PlanPriority.isHighPriority("Critical"))
        assertTrue(PlanPriority.isHighPriority("CRITICAL"))
    }
    
    @Test
    fun `should handle mixed language titles`() {
        assertTrue(PlanPriority.isCodeStyleIssue("代码风格 Code Style"))
        assertTrue(PlanPriority.isCodeStyleIssue("String Formatting 字符串格式化"))
    }
}

