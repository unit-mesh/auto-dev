package cc.unitmesh.agent.plan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlanStateServiceTest {
    
    @Test
    fun `should create plan from tasks`() {
        val service = PlanStateService()
        val tasks = listOf(
            PlanTask(id = "task1", title = "Task 1"),
            PlanTask(id = "task2", title = "Task 2")
        )
        
        val plan = service.createPlan(tasks)
        
        assertNotNull(plan)
        assertEquals(2, plan.taskCount)
        assertEquals(plan, service.getPlan())
    }
    
    @Test
    fun `should create plan from markdown`() {
        val service = PlanStateService()
        val markdown = """
            1. First task
               - [ ] Step one
               - [ ] Step two
            2. Second task
               - [ ] Step three
        """.trimIndent()
        
        val plan = service.createPlanFromMarkdown(markdown)
        
        assertEquals(2, plan.taskCount)
        assertEquals(2, plan.tasks[0].steps.size)
        assertEquals(1, plan.tasks[1].steps.size)
    }
    
    @Test
    fun `should update task status`() {
        val service = PlanStateService()
        val task = PlanTask(id = "task1", title = "Task 1")
        service.createPlan(listOf(task))
        
        service.updateTaskStatus("task1", TaskStatus.IN_PROGRESS)
        
        assertEquals(TaskStatus.IN_PROGRESS, service.getPlan()?.getTask("task1")?.status)
    }
    
    @Test
    fun `should complete step`() {
        val service = PlanStateService()
        val step = PlanStep(id = "step1", description = "Step 1")
        val task = PlanTask(id = "task1", title = "Task 1", steps = mutableListOf(step))
        service.createPlan(listOf(task))
        
        service.completeStep("task1", "step1")
        
        val updatedStep = service.getPlan()?.getTask("task1")?.steps?.find { it.id == "step1" }
        assertEquals(TaskStatus.COMPLETED, updatedStep?.status)
    }
    
    @Test
    fun `should notify listeners on plan created`() {
        val service = PlanStateService()
        var notifiedPlan: AgentPlan? = null
        
        service.addListener(object : DefaultPlanUpdateListener() {
            override fun onPlanCreated(plan: AgentPlan) {
                notifiedPlan = plan
            }
        })
        
        val plan = service.createPlan(listOf(PlanTask(id = "task1", title = "Task 1")))
        
        assertEquals(plan, notifiedPlan)
    }
    
    @Test
    fun `should notify listeners on task updated`() {
        val service = PlanStateService()
        var notifiedTask: PlanTask? = null
        
        service.addListener(object : DefaultPlanUpdateListener() {
            override fun onTaskUpdated(task: PlanTask) {
                notifiedTask = task
            }
        })
        
        service.createPlan(listOf(PlanTask(id = "task1", title = "Task 1")))
        service.updateTaskStatus("task1", TaskStatus.COMPLETED)
        
        assertNotNull(notifiedTask)
        assertEquals("task1", notifiedTask?.id)
        assertEquals(TaskStatus.COMPLETED, notifiedTask?.status)
    }
    
    @Test
    fun `should notify listeners on step completed`() {
        val service = PlanStateService()
        var completedTaskId: String? = null
        var completedStepId: String? = null
        
        service.addListener(object : DefaultPlanUpdateListener() {
            override fun onStepCompleted(taskId: String, stepId: String) {
                completedTaskId = taskId
                completedStepId = stepId
            }
        })
        
        val step = PlanStep(id = "step1", description = "Step 1")
        val task = PlanTask(id = "task1", title = "Task 1", steps = mutableListOf(step))
        service.createPlan(listOf(task))
        service.completeStep("task1", "step1")
        
        assertEquals("task1", completedTaskId)
        assertEquals("step1", completedStepId)
    }
    
    @Test
    fun `should clear plan`() {
        val service = PlanStateService()
        service.createPlan(listOf(PlanTask(id = "task1", title = "Task 1")))
        
        service.clearPlan()
        
        assertNull(service.getPlan())
    }
    
    @Test
    fun `should add task to existing plan`() {
        val service = PlanStateService()
        service.createPlan(listOf(PlanTask(id = "task1", title = "Task 1")))
        
        service.addTask(PlanTask(id = "task2", title = "Task 2"))
        
        assertEquals(2, service.getPlan()?.taskCount)
    }
    
    @Test
    fun `should update step status`() {
        val service = PlanStateService()
        val step = PlanStep(id = "step1", description = "Step 1")
        val task = PlanTask(id = "task1", title = "Task 1", steps = mutableListOf(step))
        service.createPlan(listOf(task))
        
        service.updateStepStatus("task1", "step1", TaskStatus.IN_PROGRESS)
        
        val updatedStep = service.getPlan()?.getTask("task1")?.steps?.find { it.id == "step1" }
        assertEquals(TaskStatus.IN_PROGRESS, updatedStep?.status)
    }
    
    @Test
    fun `should calculate progress correctly`() {
        val service = PlanStateService()
        val steps = mutableListOf(
            PlanStep(id = "step1", description = "Step 1", status = TaskStatus.COMPLETED),
            PlanStep(id = "step2", description = "Step 2", status = TaskStatus.COMPLETED),
            PlanStep(id = "step3", description = "Step 3", status = TaskStatus.TODO),
            PlanStep(id = "step4", description = "Step 4", status = TaskStatus.TODO)
        )
        val task = PlanTask(id = "task1", title = "Task 1", steps = steps)
        service.createPlan(listOf(task))
        
        assertEquals(50, service.getPlan()?.progressPercent)
    }
}

