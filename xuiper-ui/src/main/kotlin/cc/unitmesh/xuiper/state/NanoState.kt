package cc.unitmesh.xuiper.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NanoState - Reactive state container for NanoDSL
 * 
 * Provides observable state that can be bound to UI components.
 * Supports both one-way subscription (<<) and two-way binding (:=).
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
 * ```
 */
class NanoState(initialState: Map<String, Any?> = emptyMap()) {
    
    private val _states = mutableMapOf<String, MutableStateFlow<Any?>>()
    private val subscribers = mutableMapOf<String, MutableList<(Any?) -> Unit>>()
    
    init {
        initialState.forEach { (key, value) ->
            _states[key] = MutableStateFlow(value)
        }
    }

    /**
     * Get current value of a state variable
     */
    operator fun get(path: String): Any? {
        return _states[path]?.value
    }

    /**
     * Set value of a state variable
     */
    operator fun set(path: String, value: Any?) {
        val flow = _states.getOrPut(path) { MutableStateFlow(value) }
        flow.value = value
        notifySubscribers(path, value)
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

