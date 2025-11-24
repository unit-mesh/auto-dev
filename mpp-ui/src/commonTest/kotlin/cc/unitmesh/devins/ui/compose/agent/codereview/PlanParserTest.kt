package cc.unitmesh.devins.ui.compose.agent.codereview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for PlanParser
 * Verifies parsing of Plan format markdown into structured plan items
 */
class PlanParserTest {
    
    @Test
    fun `should parse simple plan with code block`() {
        val planOutput = """
            ```plan
            1. Null Safety Issues - CRITICAL
                - [ ] Fix [UserService.kt](src/main/kotlin/UserService.kt) line 45: user parameter not null-checked
                - [ ] Prevents NullPointerException
                - [ ] Add requireNotNull(user) at line 45
            
            2. Exception Handling - HIGH
                - [ ] Fix [DatabaseHelper.kt](src/main/kotlin/DatabaseHelper.kt) line 62: catching overly generic Exception
                - [ ] May hide specific errors
                - [ ] Catch specific exceptions instead
            ```
        """.trimIndent()
        
        val items = PlanParser.parse(planOutput)
        
        assertEquals(2, items.size)
        
        val firstItem = items[0]
        assertEquals(1, firstItem.number)
        assertEquals("Null Safety Issues", firstItem.title)
        assertEquals("CRITICAL", firstItem.priority)
        assertEquals(3, firstItem.steps.size)
        assertEquals(StepStatus.TODO, firstItem.steps[0].status)
        assertTrue(firstItem.steps[0].fileLinks.isNotEmpty())
        assertEquals("UserService.kt", firstItem.steps[0].fileLinks[0].displayText)
        assertEquals("src/main/kotlin/UserService.kt", firstItem.steps[0].fileLinks[0].filePath)
        
        val secondItem = items[1]
        assertEquals(2, secondItem.number)
        assertEquals("Exception Handling", secondItem.title)
        assertEquals("HIGH", secondItem.priority)
    }
    
    @Test
    fun `should parse plan without code block`() {
        val planOutput = """
            1. Code Style Issues - MEDIUM
                - [ ] Fix formatting
                - [ ] Improve readability
            
            2. Performance - HIGH
                - [*] Optimize algorithm
        """.trimIndent()
        
        val items = PlanParser.parse(planOutput)
        
        assertEquals(2, items.size)
        assertEquals("Code Style Issues", items[0].title)
        assertEquals("MEDIUM", items[0].priority)
        assertEquals(StepStatus.IN_PROGRESS, items[1].steps[0].status)
    }
    
    @Test
    fun `should parse step status markers correctly`() {
        val planOutput = """
            1. Test Item - MEDIUM
                - [ ] TODO item
                - [✓] Completed item
                - [!] Failed item
                - [*] In progress item
        """.trimIndent()
        
        val items = PlanParser.parse(planOutput)
        val steps = items[0].steps
        
        assertEquals(4, steps.size)
        assertEquals(StepStatus.TODO, steps[0].status)
        assertEquals(StepStatus.COMPLETED, steps[1].status)
        assertEquals(StepStatus.FAILED, steps[2].status)
        assertEquals(StepStatus.IN_PROGRESS, steps[3].status)
    }
    
    @Test
    fun `should extract file links from step text`() {
        val planOutput = """
            1. File Links Test - MEDIUM
                - [ ] Fix [File1.kt](path/to/File1.kt) and [File2.kt](path/to/File2.kt)
        """.trimIndent()
        
        val items = PlanParser.parse(planOutput)
        val step = items[0].steps[0]
        
        assertEquals(2, step.fileLinks.size)
        assertEquals("File1.kt", step.fileLinks[0].displayText)
        assertEquals("path/to/File1.kt", step.fileLinks[0].filePath)
        assertEquals("File2.kt", step.fileLinks[1].displayText)
        assertEquals("path/to/File2.kt", step.fileLinks[1].filePath)
    }
    
    @Test
    fun `should handle empty plan`() {
        val items = PlanParser.parse("")
        assertTrue(items.isEmpty())
    }
    
    @Test
    fun `should handle plan with default priority`() {
        val planOutput = """
            1. Item without priority
                - [ ] Some step
        """.trimIndent()
        
        val items = PlanParser.parse(planOutput)
        assertEquals(1, items.size)
        assertEquals("MEDIUM", items[0].priority)
    }
    
    @Test
    fun `should handle multi-line step continuation`() {
        val planOutput = """
            1. Multi-line Test - MEDIUM
                - [ ] First line
                - continuation line
        """.trimIndent()
        
        val items = PlanParser.parse(planOutput)
        val step = items[0].steps[0]
        assertTrue(step.text.contains("continuation line"))
    }
}

