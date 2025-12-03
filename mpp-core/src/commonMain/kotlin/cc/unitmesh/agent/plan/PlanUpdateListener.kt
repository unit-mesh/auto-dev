package cc.unitmesh.agent.plan

/**
 * Listener interface for plan updates.
 * 
 * Implement this interface to receive notifications when
 * the plan state changes.
 */
interface PlanUpdateListener {
    /**
     * Called when a new plan is created or the entire plan is replaced.
     */
    fun onPlanCreated(plan: AgentPlan)
    
    /**
     * Called when the plan is updated (tasks added, removed, or modified).
     */
    fun onPlanUpdated(plan: AgentPlan)
    
    /**
     * Called when a specific task is updated.
     */
    fun onTaskUpdated(task: PlanTask)
    
    /**
     * Called when a specific step is completed.
     */
    fun onStepCompleted(taskId: String, stepId: String)
    
    /**
     * Called when the plan is cleared/reset.
     */
    fun onPlanCleared()
}

/**
 * Default implementation of PlanUpdateListener with empty methods.
 * Extend this class to only override the methods you need.
 */
open class DefaultPlanUpdateListener : PlanUpdateListener {
    override fun onPlanCreated(plan: AgentPlan) {}
    override fun onPlanUpdated(plan: AgentPlan) {}
    override fun onTaskUpdated(task: PlanTask) {}
    override fun onStepCompleted(taskId: String, stepId: String) {}
    override fun onPlanCleared() {}
}

