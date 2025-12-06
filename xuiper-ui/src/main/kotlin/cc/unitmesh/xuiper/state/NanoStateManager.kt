package cc.unitmesh.xuiper.state

import cc.unitmesh.xuiper.action.MutationOp
import cc.unitmesh.xuiper.action.NanoAction
import cc.unitmesh.xuiper.ir.NanoIR
import cc.unitmesh.xuiper.ir.NanoStateIR
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * NanoStateManager - Manages state for NanoUI components
 * 
 * Handles:
 * - State initialization from NanoIR
 * - Action dispatching (mutations, navigation, fetch)
 * - Expression evaluation for bindings
 * 
 * Reference: DivKit action protocol, Enaml binding operators
 */
class NanoStateManager {
    
    private val state = NanoState()
    private val actionHandlers = mutableListOf<(NanoAction) -> Unit>()
    
    /**
     * Initialize state from NanoIR state definition
     */
    fun initFromIR(stateIR: NanoStateIR?) {
        if (stateIR == null) return
        
        stateIR.variables.forEach { (name, varDef) ->
            val defaultValue = varDef.defaultValue?.let { parseJsonValue(it) }
            state[name] = defaultValue ?: getDefaultForType(varDef.type)
        }
    }

    /**
     * Initialize state from NanoIR component (convenience method)
     */
    fun initFromComponent(ir: NanoIR) {
        initFromIR(ir.state)
    }

    /**
     * Get the underlying NanoState
     */
    fun getState(): NanoState = state

    /**
     * Get a state value by path
     */
    operator fun get(path: String): Any? = state[path]

    /**
     * Set a state value by path
     */
    operator fun set(path: String, value: Any?) {
        state[path] = value
    }

    /**
     * Dispatch an action
     */
    fun dispatch(action: NanoAction) {
        when (action) {
            is NanoAction.StateMutation -> handleStateMutation(action)
            is NanoAction.Navigate -> handleNavigate(action)
            is NanoAction.Fetch -> handleFetch(action)
            is NanoAction.ShowToast -> handleShowToast(action)
            is NanoAction.Sequence -> action.actions.forEach { dispatch(it) }
        }
        
        // Notify external handlers
        actionHandlers.forEach { it(action) }
    }

    /**
     * Register an external action handler
     */
    fun onAction(handler: (NanoAction) -> Unit) {
        actionHandlers.add(handler)
    }

    /**
     * Evaluate a binding expression
     * 
     * Supports:
     * - Simple path: "state.count" -> value of count
     * - F-string: f"Total: ${state.count}" -> interpolated string
     * - Arithmetic: "state.count * state.price" -> computed value
     */
    fun evaluate(expression: String): Any? {
        val trimmed = expression.trim()
        
        // F-string interpolation
        if (trimmed.startsWith("f\"") || trimmed.startsWith("f'")) {
            return evaluateFString(trimmed)
        }
        
        // Simple state path
        if (trimmed.startsWith("state.")) {
            val path = trimmed.removePrefix("state.")
            return state[path]
        }
        
        // Direct path
        if (state.has(trimmed)) {
            return state[trimmed]
        }
        
        // Return as literal
        return trimmed
    }

    /**
     * Create a derived value from an expression
     * Re-evaluates whenever dependencies change
     */
    fun derived(expression: String, callback: (Any?) -> Unit): () -> Unit {
        // Extract state paths from expression
        val paths = extractStatePaths(expression)
        
        // Subscribe to all paths
        val unsubscribes = paths.map { path ->
            state.subscribe(path) {
                callback(evaluate(expression))
            }
        }
        
        // Initial evaluation
        callback(evaluate(expression))
        
        // Return combined unsubscribe
        return {
            unsubscribes.forEach { it() }
        }
    }

    private fun handleStateMutation(action: NanoAction.StateMutation) {
        val currentValue = state[action.path]
        val newValue = parseValue(action.value)
        
        when (action.operation) {
            MutationOp.SET -> state[action.path] = newValue
            MutationOp.ADD -> state[action.path] = add(currentValue, newValue)
            MutationOp.SUBTRACT -> state[action.path] = subtract(currentValue, newValue)
            MutationOp.APPEND -> {
                val list = (currentValue as? MutableList<Any?>) ?: mutableListOf()
                list.add(newValue)
                state[action.path] = list
            }
            MutationOp.REMOVE -> {
                val list = (currentValue as? MutableList<Any?>) ?: mutableListOf()
                list.remove(newValue)
                state[action.path] = list
            }
        }
    }

    private fun handleNavigate(action: NanoAction.Navigate) {
        // Navigation is typically handled by external handler
        // Store navigation request in state for reactive handling
        state["__navigation__"] = action.to
    }

    private fun handleFetch(action: NanoAction.Fetch) {
        // Fetch is typically handled by external handler
        state["__fetch__"] = mapOf("url" to action.url, "method" to action.method)
    }

    private fun handleShowToast(action: NanoAction.ShowToast) {
        state["__toast__"] = action.message
    }

    private fun add(a: Any?, b: Any?): Any? {
        return when {
            a is Int && b is Int -> a + b
            a is Double && b is Double -> a + b
            a is Int && b is Double -> a + b
            a is Double && b is Int -> a + b
            a is String && b is String -> a + b
            else -> b
        }
    }

    private fun subtract(a: Any?, b: Any?): Any? {
        return when {
            a is Int && b is Int -> a - b
            a is Double && b is Double -> a - b
            a is Int && b is Double -> a - b
            a is Double && b is Int -> a - b
            else -> b
        }
    }

    private fun parseValue(value: String): Any? {
        val trimmed = value.trim()

        // Boolean
        if (trimmed == "true") return true
        if (trimmed == "false") return false

        // Integer
        trimmed.toIntOrNull()?.let { return it }

        // Double
        trimmed.toDoubleOrNull()?.let { return it }

        // String (remove quotes if present)
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length - 1)
        }

        // State reference
        if (trimmed.startsWith("state.")) {
            return state[trimmed.removePrefix("state.")]
        }

        return trimmed
    }

    private fun parseJsonValue(element: kotlinx.serialization.json.JsonElement): Any? {
        return when (element) {
            is JsonPrimitive -> {
                element.booleanOrNull
                    ?: element.intOrNull
                    ?: element.doubleOrNull
                    ?: element.content
            }
            else -> element.toString()
        }
    }

    private fun getDefaultForType(type: String): Any? {
        return when (type.lowercase()) {
            "int", "integer" -> 0
            "float", "double", "number" -> 0.0
            "bool", "boolean" -> false
            "string", "str" -> ""
            "list", "array" -> mutableListOf<Any?>()
            "map", "object" -> mutableMapOf<String, Any?>()
            else -> null
        }
    }

    private fun evaluateFString(fstring: String): String {
        // Remove f" prefix and " suffix
        val content = fstring.removePrefix("f\"").removePrefix("f'")
            .removeSuffix("\"").removeSuffix("'")

        // Replace ${expression} with evaluated value
        return content.replace(Regex("""\$\{([^}]+)\}""")) { match ->
            val expr = match.groupValues[1]
            evaluate(expr)?.toString() ?: ""
        }
    }

    private fun extractStatePaths(expression: String): List<String> {
        val paths = mutableListOf<String>()
        val regex = Regex("""state\.(\w+)""")
        regex.findAll(expression).forEach { match ->
            paths.add(match.groupValues[1])
        }
        return paths.distinct()
    }
}

