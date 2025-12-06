package cc.unitmesh.xuiper.action

/**
 * Cross-platform action handler interface for NanoUI
 * 
 * Provides a unified way to handle user interactions across different platforms
 * (Compose, React, HTML). Each platform implements this interface to handle
 * actions in a platform-specific way.
 * 
 * Design follows the component-specific method pattern from NanoRenderer,
 * ensuring compile-time safety when new action types are added.
 * 
 * @see NanoAction for the sealed class of action types
 * @see cc.unitmesh.xuiper.render.NanoRenderer for similar pattern
 */
interface NanoActionHandler {
    
    /**
     * Main dispatch method - routes action to appropriate handler
     * 
     * @param action The action to handle
     * @param context The current render context with state
     * @return Result of the action execution
     */
    fun handleAction(action: NanoAction, context: NanoActionContext): ActionResult
    
    // ============================================================================
    // Built-in Action Handlers
    // ============================================================================
    
    /**
     * Handle state mutation action
     * Example: `state.count += 1` or `state.name = "John"`
     */
    fun handleStateMutation(mutation: NanoAction.StateMutation, context: NanoActionContext): ActionResult
    
    /**
     * Handle navigation action
     * Example: `Navigate(to="/cart")`
     */
    fun handleNavigate(navigate: NanoAction.Navigate, context: NanoActionContext): ActionResult
    
    /**
     * Handle network fetch action
     * Example: `Fetch(url="/api/users", method="POST", body={...})`
     */
    fun handleFetch(fetch: NanoAction.Fetch, context: NanoActionContext): ActionResult
    
    /**
     * Handle toast notification action
     * Example: `ShowToast("Added to cart")`
     */
    fun handleShowToast(toast: NanoAction.ShowToast, context: NanoActionContext): ActionResult
    
    /**
     * Handle sequence of actions
     * Executes multiple actions in order
     */
    fun handleSequence(sequence: NanoAction.Sequence, context: NanoActionContext): ActionResult
    
    // ============================================================================
    // Custom Action Support
    // ============================================================================
    
    /**
     * Handle custom user-defined action
     * 
     * For actions like `AddTask(title=state.new_task)` or `DeleteTask(id=task.id)`
     * that are not part of the built-in action set.
     * 
     * @param name Action name (e.g., "AddTask", "DeleteTask")
     * @param payload Action parameters
     * @param context Current render context
     * @return Result of the action execution
     */
    fun handleCustomAction(
        name: String, 
        payload: Map<String, Any?>, 
        context: NanoActionContext
    ): ActionResult
}

/**
 * Result of an action execution
 */
sealed class ActionResult {
    /**
     * Action completed successfully
     */
    data object Success : ActionResult()
    
    /**
     * Action completed successfully with a value
     */
    data class SuccessWithValue(val value: Any?) : ActionResult()
    
    /**
     * Action failed with an error
     */
    data class Error(val message: String, val cause: Throwable? = null) : ActionResult()
    
    /**
     * Action is pending (async operation in progress)
     * The callback will be invoked when the action completes
     */
    data class Pending(val onComplete: (ActionResult) -> Unit) : ActionResult()
    
    /**
     * Check if the result is successful
     */
    val isSuccess: Boolean
        get() = this is Success || this is SuccessWithValue
    
    /**
     * Check if the result is an error
     */
    val isError: Boolean
        get() = this is Error
}

/**
 * Context passed to action handlers
 * 
 * Contains the current state and methods to mutate it
 */
interface NanoActionContext {
    /**
     * Get a value from state by path
     * Example: get("user.name") returns the user's name
     */
    operator fun get(path: String): Any?

    /**
     * Set a value in state by path
     * Example: set("user.name", "John")
     */
    operator fun set(path: String, value: Any?)
    
    /**
     * Apply a mutation operation to state
     */
    fun mutate(path: String, operation: MutationOp, value: Any?)
    
    /**
     * Get the entire state as a map
     */
    fun getState(): Map<String, Any?>
}

/**
 * Registry for custom action handlers
 * 
 * Allows users to register handlers for custom actions like AddTask, DeleteTask
 */
typealias CustomActionHandler = (payload: Map<String, Any?>, context: NanoActionContext) -> ActionResult

/**
 * Default implementation of NanoActionHandler
 * 
 * Provides a base implementation that can be extended by platform-specific handlers.
 * Routes actions to the appropriate handler method.
 */
abstract class BaseNanoActionHandler : NanoActionHandler {
    
    private val customHandlers = mutableMapOf<String, CustomActionHandler>()
    
    /**
     * Register a custom action handler
     */
    fun registerCustomAction(name: String, handler: CustomActionHandler) {
        customHandlers[name] = handler
    }
    
    /**
     * Register multiple custom action handlers
     */
    fun registerCustomActions(handlers: Map<String, CustomActionHandler>) {
        customHandlers.putAll(handlers)
    }
    
    override fun handleAction(action: NanoAction, context: NanoActionContext): ActionResult {
        return when (action) {
            is NanoAction.StateMutation -> handleStateMutation(action, context)
            is NanoAction.Navigate -> handleNavigate(action, context)
            is NanoAction.Fetch -> handleFetch(action, context)
            is NanoAction.ShowToast -> handleShowToast(action, context)
            is NanoAction.Sequence -> handleSequence(action, context)
        }
    }
    
    override fun handleStateMutation(
        mutation: NanoAction.StateMutation, 
        context: NanoActionContext
    ): ActionResult {
        return try {
            context.mutate(mutation.path, mutation.operation, mutation.value)
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Error("Failed to mutate state: ${e.message}", e)
        }
    }
    
    override fun handleSequence(
        sequence: NanoAction.Sequence, 
        context: NanoActionContext
    ): ActionResult {
        for (action in sequence.actions) {
            val result = handleAction(action, context)
            if (result.isError) {
                return result
            }
        }
        return ActionResult.Success
    }
    
    override fun handleCustomAction(
        name: String,
        payload: Map<String, Any?>,
        context: NanoActionContext
    ): ActionResult {
        val handler = customHandlers[name]
            ?: return ActionResult.Error("Unknown custom action: $name")
        
        return try {
            handler(payload, context)
        } catch (e: Exception) {
            ActionResult.Error("Custom action '$name' failed: ${e.message}", e)
        }
    }
}

