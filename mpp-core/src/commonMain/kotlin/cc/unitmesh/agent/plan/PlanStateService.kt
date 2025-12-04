package cc.unitmesh.agent.plan

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service for managing plan state.
 * 
 * Provides reactive state management using StateFlow and
 * listener-based notifications for plan updates.
 * 
 * This is the central point for all plan-related state management
 * in the agent system.
 */
class PlanStateService {
    
    private val _currentPlan = MutableStateFlow<AgentPlan?>(null)
    
    /**
     * Observable state of the current plan.
     * Use this for reactive UI updates.
     */
    val currentPlan: StateFlow<AgentPlan?> = _currentPlan.asStateFlow()
    
    private val listeners = mutableListOf<PlanUpdateListener>()
    
    /**
     * Get the current plan (non-reactive).
     */
    fun getPlan(): AgentPlan? = _currentPlan.value
    
    /**
     * Create a new plan from a list of tasks.
     */
    fun createPlan(tasks: List<PlanTask>): AgentPlan {
        val plan = AgentPlan.create(tasks)
        _currentPlan.value = plan
        notifyPlanCreated(plan)
        return plan
    }
    
    /**
     * Create a new plan from markdown content.
     */
    fun createPlanFromMarkdown(markdown: String): AgentPlan {
        val tasks = MarkdownPlanParser.parse(markdown)
        return createPlan(tasks)
    }
    
    /**
     * Set the current plan directly.
     */
    fun setPlan(plan: AgentPlan) {
        _currentPlan.value = plan
        notifyPlanCreated(plan)
    }
    
    /**
     * Update the current plan with new tasks.
     */
    fun updatePlan(tasks: List<PlanTask>) {
        val plan = _currentPlan.value
        if (plan != null) {
            plan.tasks.clear()
            plan.tasks.addAll(tasks)
            notifyPlanUpdated(plan)
        } else {
            createPlan(tasks)
        }
    }
    
    /**
     * Update the current plan from markdown content.
     */
    fun updatePlanFromMarkdown(markdown: String) {
        val tasks = MarkdownPlanParser.parse(markdown)
        updatePlan(tasks)
    }
    
    /**
     * Add a task to the current plan.
     */
    fun addTask(task: PlanTask) {
        val plan = _currentPlan.value ?: createPlan(emptyList())
        plan.addTask(task)
        notifyPlanUpdated(plan)
    }
    
    /**
     * Update a task's status.
     */
    fun updateTaskStatus(taskId: String, status: TaskStatus) {
        val plan = _currentPlan.value ?: return
        val task = plan.getTask(taskId) ?: return
        task.updateStatus(status)
        // Trigger StateFlow update by creating a new plan instance with updated timestamp
        _currentPlan.value = plan.copy(updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
        notifyTaskUpdated(task)
    }
    
    /**
     * Complete a step within a task.
     */
    fun completeStep(taskId: String, stepId: String) {
        val plan = _currentPlan.value ?: return
        plan.completeStep(taskId, stepId)
        // Trigger StateFlow update by creating a new plan instance with updated timestamp
        _currentPlan.value = plan.copy(updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
        notifyStepCompleted(taskId, stepId)
    }
    
    /**
     * Update a step's status.
     */
    fun updateStepStatus(taskId: String, stepId: String, status: TaskStatus) {
        val plan = _currentPlan.value ?: return
        val task = plan.getTask(taskId) ?: return
        task.updateStepStatus(stepId, status)
        // Trigger StateFlow update by creating a new plan instance with updated timestamp
        _currentPlan.value = plan.copy(updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
        notifyTaskUpdated(task)
    }
    
    /**
     * Clear the current plan.
     */
    fun clearPlan() {
        _currentPlan.value = null
        notifyPlanCleared()
    }
    
    /**
     * Add a listener for plan updates.
     */
    fun addListener(listener: PlanUpdateListener) {
        listeners.add(listener)
    }
    
    /**
     * Remove a listener.
     */
    fun removeListener(listener: PlanUpdateListener) {
        listeners.remove(listener)
    }
    
    // Notification methods
    private fun notifyPlanCreated(plan: AgentPlan) {
        listeners.forEach { it.onPlanCreated(plan) }
    }
    
    private fun notifyPlanUpdated(plan: AgentPlan) {
        listeners.forEach { it.onPlanUpdated(plan) }
    }
    
    private fun notifyTaskUpdated(task: PlanTask) {
        listeners.forEach { it.onTaskUpdated(task) }
    }
    
    private fun notifyStepCompleted(taskId: String, stepId: String) {
        listeners.forEach { it.onStepCompleted(taskId, stepId) }
    }
    
    private fun notifyPlanCleared() {
        listeners.forEach { it.onPlanCleared() }
    }
}

