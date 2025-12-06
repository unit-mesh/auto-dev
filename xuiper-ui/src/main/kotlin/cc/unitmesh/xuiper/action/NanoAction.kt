package cc.unitmesh.xuiper.action

/**
 * NanoDSL Action System
 * 
 * Defines all possible actions that can be triggered by user interactions.
 * Actions are limited to a predefined set for security (prevents arbitrary code execution).
 * 
 * Reference: DivKit Action Protocol
 */
sealed class NanoAction {
    /**
     * State mutation action
     * Example: `on_click: state.count += 1`
     */
    data class StateMutation(
        val path: String,
        val operation: MutationOp,
        val value: String
    ) : NanoAction()

    /**
     * Navigation action
     * Example: `on_click: Navigate(to="/cart")`
     */
    data class Navigate(
        val to: String
    ) : NanoAction()

    /**
     * Network fetch action
     * Example: `on_click: Fetch(url="/api/buy", method="POST")`
     */
    data class Fetch(
        val url: String,
        val method: String = "GET",
        val body: Map<String, String>? = null
    ) : NanoAction()

    /**
     * Toast notification
     * Example: `on_click: ShowToast("Added to cart")`
     */
    data class ShowToast(
        val message: String
    ) : NanoAction()

    /**
     * Compound action (multiple actions in sequence)
     */
    data class Sequence(
        val actions: List<NanoAction>
    ) : NanoAction()
}

/**
 * State mutation operations
 */
enum class MutationOp {
    SET,        // state.x = value
    ADD,        // state.x += value
    SUBTRACT,   // state.x -= value
    APPEND,     // state.list.append(value)
    REMOVE      // state.list.remove(value)
}

