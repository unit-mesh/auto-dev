package cc.unitmesh.xuiper.state

import cc.unitmesh.xuiper.action.MutationOp
import cc.unitmesh.xuiper.action.NanoActionContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NanoState - Reactive state container for NanoDSL
 *
 * Provides observable state that can be bound to UI components.
 * Supports both one-way subscription (<<) and two-way binding (:=).
 * Implements NanoActionContext for action handler integration.
 *
 * Example usage:
 * ```kotlin
 * val state = NanoState(mapOf("count" to 0, "name" to ""))
 *
 * // Subscribe to changes
 * state.subscribe("count") { value -> println("Count: $value") }
 *
 * // Update state
 * state.set("count", 1)
 * state.update("count") { (it as Int) + 1 }
 *
 * // Mutation operations
 * state.mutate("count", MutationOp.ADD, 5)
 * state.mutate("items", MutationOp.APPEND, newItem)
 * ```
 */
class NanoState(initialState: Map<String, Any?> = emptyMap()) : NanoActionContext {
    
    private val _states = mutableMapOf<String, MutableStateFlow<Any?>>()
    private val subscribers = mutableMapOf<String, MutableList<(Any?) -> Unit>>()
    
    init {
        initialState.forEach { (key, value) ->
            _states[key] = MutableStateFlow(value)
        }
    }

    /**
     * Get StateFlow for a path (for Compose integration)
     */
    fun flow(path: String): StateFlow<Any?> {
        return _states.getOrPut(path) { MutableStateFlow(null) }.asStateFlow()
    }

    /**
     * Update state with a transform function
     */
    fun update(path: String, transform: (Any?) -> Any?) {
        val current = get(path)
        set(path, transform(current))
    }

    /**
     * Subscribe to state changes (one-way binding: <<)
     * Returns an unsubscribe function
     */
    fun subscribe(path: String, callback: (Any?) -> Unit): () -> Unit {
        val list = subscribers.getOrPut(path) { mutableListOf() }
        list.add(callback)
        
        // Call with current value immediately
        callback(get(path))
        
        return {
            list.remove(callback)
        }
    }

    /**
     * Create a two-way binding (:=)
     * Returns a TwoWayBinding object
     */
    fun bind(path: String): TwoWayBinding {
        return TwoWayBinding(
            get = { get(path) },
            set = { value -> set(path, value) }
        )
    }

    /**
     * Check if a state variable exists
     */
    fun has(path: String): Boolean = path in _states

    /**
     * Get all state keys
     */
    fun keys(): Set<String> = _states.keys.toSet()

    /**
     * Get snapshot of all state values
     */
    fun snapshot(): Map<String, Any?> {
        return _states.mapValues { it.value.value }
    }

    /**
     * Reset state to initial values
     */
    fun reset(initialState: Map<String, Any?>) {
        _states.clear()
        subscribers.clear()
        initialState.forEach { (key, value) ->
            _states[key] = MutableStateFlow(value)
        }
    }

    private fun notifySubscribers(path: String, value: Any?) {
        subscribers[path]?.forEach { callback ->
            callback(value)
        }
    }

    // ============================================================================
    // NanoActionContext Implementation
    // ============================================================================

    override operator fun get(path: String): Any? {
        // Support nested path like "user.name"
        if (!path.contains(".")) {
            return _states[path]?.value
        }

        val parts = path.split(".")
        var current: Any? = _states[parts.first()]?.value

        for (i in 1 until parts.size) {
            current = when (current) {
                is Map<*, *> -> current[parts[i]]
                is List<*> -> {
                    val index = parts[i].toIntOrNull()
                    if (index != null && index in current.indices) current[index] else null
                }
                else -> null
            }
            if (current == null) break
        }

        return current
    }

    override operator fun set(path: String, value: Any?) {
        if (!path.contains(".")) {
            val flow = _states.getOrPut(path) { MutableStateFlow(value) }
            flow.value = value
            notifySubscribers(path, value)
            return
        }

        // Handle nested path
        val parts = path.split(".")
        val rootKey = parts.first()
        val rootValue = _states[rootKey]?.value

        val newRootValue = setNestedValue(
            when (rootValue) {
                is Map<*, *> -> @Suppress("UNCHECKED_CAST") (rootValue as Map<String, Any?>).toMutableMap()
                else -> mutableMapOf()
            },
            parts.drop(1),
            value
        )

        val flow = _states.getOrPut(rootKey) { MutableStateFlow(newRootValue) }
        flow.value = newRootValue
        notifySubscribers(rootKey, newRootValue)
    }

    override fun mutate(path: String, operation: MutationOp, value: Any?) {
        val currentValue = get(path)

        val newValue = when (operation) {
            MutationOp.SET -> value

            MutationOp.ADD -> {
                when {
                    currentValue is Number && value is Number ->
                        currentValue.toDouble() + value.toDouble()
                    currentValue is String && value != null ->
                        currentValue + value.toString()
                    else -> value
                }
            }

            MutationOp.SUBTRACT -> {
                if (currentValue is Number && value is Number) {
                    currentValue.toDouble() - value.toDouble()
                } else currentValue
            }

            MutationOp.APPEND -> {
                when (currentValue) {
                    is List<*> -> currentValue + value
                    is MutableList<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        (currentValue as MutableList<Any?>).also { it.add(value) }
                    }
                    null -> listOf(value)
                    else -> listOf(currentValue, value)
                }
            }

            MutationOp.REMOVE -> {
                when (currentValue) {
                    is List<*> -> currentValue - value
                    is MutableList<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        (currentValue as MutableList<Any?>).also { it.remove(value) }
                    }
                    else -> currentValue
                }
            }
        }

        set(path, newValue)
    }

    override fun getState(): Map<String, Any?> = snapshot()

    private fun setNestedValue(
        map: MutableMap<String, Any?>,
        parts: List<String>,
        value: Any?
    ): Map<String, Any?> {
        if (parts.isEmpty()) return map

        val key = parts.first()

        if (parts.size == 1) {
            map[key] = value
        } else {
            val nested = map[key]
            val nestedMap = when (nested) {
                is MutableMap<*, *> -> @Suppress("UNCHECKED_CAST") (nested as MutableMap<String, Any?>)
                is Map<*, *> -> @Suppress("UNCHECKED_CAST") (nested as Map<String, Any?>).toMutableMap()
                else -> mutableMapOf()
            }
            map[key] = setNestedValue(nestedMap, parts.drop(1), value)
        }

        return map
    }
}

/**
 * Two-way binding container
 * Used for input components with := syntax
 */
class TwoWayBinding(
    private val get: () -> Any?,
    private val set: (Any?) -> Unit
) {
    val value: Any? get() = get()
    
    fun update(newValue: Any?) {
        set(newValue)
    }
    
    fun updateString(newValue: String) {
        set(newValue)
    }
    
    fun updateInt(newValue: Int) {
        set(newValue)
    }
    
    fun updateBoolean(newValue: Boolean) {
        set(newValue)
    }
}

