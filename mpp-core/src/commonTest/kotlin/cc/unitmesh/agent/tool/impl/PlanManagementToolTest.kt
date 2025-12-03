package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.plan.PlanStateService
import cc.unitmesh.agent.tool.ToolExecutionContext
import cc.unitmesh.agent.tool.ToolResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PlanManagementToolTest {

    private fun createTool() = PlanManagementTool(PlanStateService())

    @Test
    fun `should create plan from markdown`() = runTest {
        val tool = createTool()
        val params = PlanManagementParams(
            action = "CREATE",
            planMarkdown = """
                1. Setup project
                   - [ ] Create directory structure
                   - [ ] Initialize git
                2. Implement feature
                   - [ ] Write code
                   - [ ] Add tests
            """.trimIndent()
        )

        val invocation = tool.createInvocation(params)
        val result = invocation.execute(ToolExecutionContext())

        assertIs<ToolResult.Success>(result)
        assertTrue(result.content.contains("Plan created with 2 tasks"))
        assertEquals("2", result.metadata["task_count"])
    }

    @Test
    fun `should return error when planMarkdown is empty for CREATE`() = runTest {
        val tool = createTool()
        val params = PlanManagementParams(action = "CREATE", planMarkdown = "")

        val invocation = tool.createInvocation(params)
        val result = invocation.execute(ToolExecutionContext())

        assertIs<ToolResult.Error>(result)
        assertTrue(result.message.contains("planMarkdown is required"))
    }

    @Test
    fun `should complete step successfully`() = runTest {
        val tool = createTool()

        // First create a plan
        val createParams = PlanManagementParams(
            action = "CREATE",
            planMarkdown = "1. Task\n   - [ ] Step 1\n   - [ ] Step 2"
        )
        tool.createInvocation(createParams).execute(ToolExecutionContext())

        // Then complete a step
        val completeParams = PlanManagementParams(
            action = "COMPLETE_STEP",
            taskIndex = 1,
            stepIndex = 1
        )
        val invocation = tool.createInvocation(completeParams)
        val result = invocation.execute(ToolExecutionContext())

        assertIs<ToolResult.Success>(result)
        assertTrue(result.content.contains("Step completed"))
        assertEquals("COMPLETED", result.metadata["status"])
    }

    @Test
    fun `should fail step successfully`() = runTest {
        val tool = createTool()

        // First create a plan
        val createParams = PlanManagementParams(
            action = "CREATE",
            planMarkdown = "1. Task\n   - [ ] Step 1"
        )
        tool.createInvocation(createParams).execute(ToolExecutionContext())

        // Then fail a step
        val failParams = PlanManagementParams(
            action = "FAIL_STEP",
            taskIndex = 1,
            stepIndex = 1
        )
        val invocation = tool.createInvocation(failParams)
        val result = invocation.execute(ToolExecutionContext())

        assertIs<ToolResult.Success>(result)
        assertTrue(result.content.contains("Step failed"))
        assertEquals("FAILED", result.metadata["status"])
    }

    @Test
    fun `should return error when no plan exists for COMPLETE_STEP`() = runTest {
        val tool = createTool()
        val params = PlanManagementParams(action = "COMPLETE_STEP", taskIndex = 1, stepIndex = 1)

        val invocation = tool.createInvocation(params)
        val result = invocation.execute(ToolExecutionContext())

        assertIs<ToolResult.Error>(result)
        assertTrue(result.message.contains("No active plan"))
    }

    @Test
    fun `should view current plan`() = runTest {
        val tool = createTool()

        // First create a plan
        val createParams = PlanManagementParams(
            action = "CREATE",
            planMarkdown = "1. Task\n   - [ ] Step 1"
        )
        tool.createInvocation(createParams).execute(ToolExecutionContext())

        // Then view it
        val viewParams = PlanManagementParams(action = "VIEW")
        val invocation = tool.createInvocation(viewParams)
        val result = invocation.execute(ToolExecutionContext())

        assertIs<ToolResult.Success>(result)
        assertTrue(result.content.contains("Task"))
        assertEquals("1", result.metadata["task_count"])
    }

    @Test
    fun `should return no active plan for VIEW when empty`() = runTest {
        val tool = createTool()
        val params = PlanManagementParams(action = "VIEW")

        val invocation = tool.createInvocation(params)
        val result = invocation.execute(ToolExecutionContext())

        assertIs<ToolResult.Success>(result)
        assertTrue(result.content.contains("No active plan"))
        assertEquals("false", result.metadata["has_plan"])
    }

    @Test
    fun `should update existing plan`() = runTest {
        val tool = createTool()

        // First create a plan
        val createParams = PlanManagementParams(
            action = "CREATE",
            planMarkdown = "1. Task 1\n   - [ ] Step 1"
        )
        tool.createInvocation(createParams).execute(ToolExecutionContext())

        // Then update it
        val updateParams = PlanManagementParams(
            action = "UPDATE",
            planMarkdown = "1. Task 1\n   - [x] Step 1\n2. Task 2\n   - [ ] Step 2"
        )
        val invocation = tool.createInvocation(updateParams)
        val result = invocation.execute(ToolExecutionContext())

        assertIs<ToolResult.Success>(result)
        assertTrue(result.content.contains("Plan updated with 2 tasks"))
    }
}

