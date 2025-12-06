package cc.unitmesh.xuiper.render

import cc.unitmesh.xuiper.action.ActionResult
import cc.unitmesh.xuiper.action.CustomActionHandler
import cc.unitmesh.xuiper.action.NanoAction
import cc.unitmesh.xuiper.action.NanoActionHandler
import cc.unitmesh.xuiper.state.NanoState

/**
 * Render context for NanoUI components
 * 
 * Bundles together all the context needed for rendering:
 * - State management
 * - Action handling
 * - Theme configuration
 * 
 * Example:
 * ```kotlin
 * val context = NanoRenderContext(
 *     state = NanoState(mapOf("count" to 0)),
 *     actionHandler = ComposeActionHandler(),
 *     theme = NanoTheme.default()
 * )
 * 
 * val html = HtmlRenderer(context).render(ir)
 * ```
 */
data class NanoRenderContext(
    /**
     * Reactive state container
     */
    val state: NanoState,
    
    /**
     * Action handler for user interactions
     */
    val actionHandler: NanoActionHandler,
    
    /**
     * Theme configuration
     */
    val theme: NanoTheme = NanoTheme.Default
) {
    /**
     * Dispatch an action through the handler
     */
    fun dispatch(action: NanoAction): ActionResult {
        return actionHandler.handleAction(action, state)
    }
    
    /**
     * Register a custom action handler
     */
    fun registerAction(name: String, handler: CustomActionHandler) {
        if (actionHandler is cc.unitmesh.xuiper.action.BaseNanoActionHandler) {
            actionHandler.registerCustomAction(name, handler)
        }
    }
    
    companion object {
        /**
         * Create a minimal context for static rendering (no actions)
         */
        fun static(): NanoRenderContext {
            return NanoRenderContext(
                state = NanoState(),
                actionHandler = NoOpActionHandler
            )
        }
    }
}

/**
 * No-op action handler for static rendering
 */
private object NoOpActionHandler : NanoActionHandler {
    override fun handleAction(action: NanoAction, context: cc.unitmesh.xuiper.action.NanoActionContext) = ActionResult.Success
    override fun handleStateMutation(mutation: NanoAction.StateMutation, context: cc.unitmesh.xuiper.action.NanoActionContext) = ActionResult.Success
    override fun handleNavigate(navigate: NanoAction.Navigate, context: cc.unitmesh.xuiper.action.NanoActionContext) = ActionResult.Success
    override fun handleFetch(fetch: NanoAction.Fetch, context: cc.unitmesh.xuiper.action.NanoActionContext) = ActionResult.Success
    override fun handleShowToast(toast: NanoAction.ShowToast, context: cc.unitmesh.xuiper.action.NanoActionContext) = ActionResult.Success
    override fun handleSequence(sequence: NanoAction.Sequence, context: cc.unitmesh.xuiper.action.NanoActionContext) = ActionResult.Success
    override fun handleCustomAction(name: String, payload: Map<String, Any?>, context: cc.unitmesh.xuiper.action.NanoActionContext) = ActionResult.Success
}

