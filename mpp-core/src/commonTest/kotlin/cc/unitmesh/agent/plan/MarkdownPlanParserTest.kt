package cc.unitmesh.agent.plan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownPlanParserTest {
    
    @Test
    fun `should parse simple plan with tasks only`() {
        val markdown = """
            1. Analyze existing code
            2. Implement feature
            3. Add tests
        """.trimIndent()
        
        val tasks = MarkdownPlanParser.parse(markdown)
        
        assertEquals(3, tasks.size)
        assertEquals("Analyze existing code", tasks[0].title)
        assertEquals("Implement feature", tasks[1].title)
        assertEquals("Add tests", tasks[2].title)
    }
    
    @Test
    fun `should parse plan with tasks and steps`() {
        val markdown = """
            1. Analyze existing code
               - [ ] Review project structure
               - [ ] Identify relevant files
            2. Implement feature
               - [ ] Create new module
               - [ ] Add tests
        """.trimIndent()
        
        val tasks = MarkdownPlanParser.parse(markdown)
        
        assertEquals(2, tasks.size)
        assertEquals(2, tasks[0].steps.size)
        assertEquals(2, tasks[1].steps.size)
        assertEquals("Review project structure", tasks[0].steps[0].description)
        assertEquals("Create new module", tasks[1].steps[0].description)
    }
    
    @Test
    fun `should parse step status markers correctly`() {
        val markdown = """
            1. Task with various statuses
               - [x] Completed step
               - [*] In-progress step
               - [ ] Todo step
               - [!] Failed step
        """.trimIndent()
        
        val tasks = MarkdownPlanParser.parse(markdown)
        val steps = tasks[0].steps
        
        assertEquals(4, steps.size)
        assertEquals(TaskStatus.COMPLETED, steps[0].status)
        assertEquals(TaskStatus.IN_PROGRESS, steps[1].status)
        assertEquals(TaskStatus.TODO, steps[2].status)
        assertEquals(TaskStatus.FAILED, steps[3].status)
    }
    
    @Test
    fun `should parse checkmark symbol`() {
        val markdown = """
            1. Task with checkmark
               - [x] Completed with x marker
        """.trimIndent()

        val tasks = MarkdownPlanParser.parse(markdown)

        assertEquals(TaskStatus.COMPLETED, tasks[0].steps[0].status)
    }
    
    @Test
    fun `should extract code file links from steps`() {
        val markdown = """
            1. Modify files
               - [ ] Update [Main.kt](src/main/kotlin/Main.kt)
               - [ ] Fix [Config.kt](src/config/Config.kt) and [Utils.kt](src/utils/Utils.kt)
        """.trimIndent()
        
        val tasks = MarkdownPlanParser.parse(markdown)
        val steps = tasks[0].steps
        
        assertEquals(1, steps[0].codeFileLinks.size)
        assertEquals("Main.kt", steps[0].codeFileLinks[0].displayText)
        assertEquals("src/main/kotlin/Main.kt", steps[0].codeFileLinks[0].filePath)
        
        assertEquals(2, steps[1].codeFileLinks.size)
        assertEquals("Config.kt", steps[1].codeFileLinks[0].displayText)
        assertEquals("Utils.kt", steps[1].codeFileLinks[1].displayText)
    }
    
    @Test
    fun `should update task status from steps`() {
        val markdown = """
            1. Partially completed task
               - [x] Done step
               - [*] Working step
               - [ ] Todo step
        """.trimIndent()
        
        val tasks = MarkdownPlanParser.parse(markdown)
        
        assertEquals(TaskStatus.IN_PROGRESS, tasks[0].status)
    }
    
    @Test
    fun `should mark task as completed when all steps done`() {
        val markdown = """
            1. Completed task
               - [x] Step 1
               - [x] Step 2
        """.trimIndent()
        
        val tasks = MarkdownPlanParser.parse(markdown)
        
        assertEquals(TaskStatus.COMPLETED, tasks[0].status)
        assertTrue(tasks[0].isCompleted)
    }
    
    @Test
    fun `should format tasks back to markdown`() {
        val tasks = listOf(
            PlanTask(
                id = "task1",
                title = "First task",
                steps = mutableListOf(
                    PlanStep("step1", "Do something", TaskStatus.COMPLETED),
                    PlanStep("step2", "Do another thing", TaskStatus.TODO)
                )
            )
        )
        
        val markdown = MarkdownPlanParser.formatToMarkdown(tasks)
        
        assertTrue(markdown.contains("1. First task"))
        assertTrue(markdown.contains("Do something"))
        assertTrue(markdown.contains("Do another thing"))
    }
    
    @Test
    fun `should handle empty content`() {
        val tasks = MarkdownPlanParser.parse("")
        assertTrue(tasks.isEmpty())
    }
    
    @Test
    fun `should parse plan to AgentPlan`() {
        val markdown = """
            1. Task one
               - [ ] Step one
            2. Task two
               - [ ] Step two
        """.trimIndent()
        
        val plan = MarkdownPlanParser.parseToPlan(markdown)
        
        assertEquals(2, plan.taskCount)
        assertTrue(plan.id.startsWith("plan_"))
    }
}

