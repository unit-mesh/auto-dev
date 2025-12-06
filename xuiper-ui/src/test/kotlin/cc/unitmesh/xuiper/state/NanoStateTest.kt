package cc.unitmesh.xuiper.state

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NanoStateTest {

    @Test
    fun `should initialize with empty state`() {
        val state = NanoState()
        
        assertTrue(state.keys().isEmpty())
        assertNull(state["nonexistent"])
    }

    @Test
    fun `should initialize with initial values`() {
        val state = NanoState(mapOf(
            "count" to 0,
            "name" to "Test"
        ))
        
        assertEquals(0, state["count"])
        assertEquals("Test", state["name"])
        assertEquals(setOf("count", "name"), state.keys())
    }

    @Test
    fun `should set and get values`() {
        val state = NanoState()
        
        state["count"] = 10
        state["name"] = "Hello"
        state["flag"] = true
        
        assertEquals(10, state["count"])
        assertEquals("Hello", state["name"])
        assertEquals(true, state["flag"])
    }

    @Test
    fun `should update value with transform function`() {
        val state = NanoState(mapOf("count" to 5))
        
        state.update("count") { (it as Int) + 1 }
        
        assertEquals(6, state["count"])
    }

    @Test
    fun `should notify subscribers on value change`() {
        val state = NanoState(mapOf("count" to 0))
        var receivedValue: Any? = null
        
        state.subscribe("count") { value ->
            receivedValue = value
        }
        
        // Initial value is received on subscribe
        assertEquals(0, receivedValue)
        
        // Update triggers notification
        state["count"] = 42
        assertEquals(42, receivedValue)
    }

    @Test
    fun `should unsubscribe correctly`() {
        val state = NanoState(mapOf("count" to 0))
        var callCount = 0
        
        val unsubscribe = state.subscribe("count") {
            callCount++
        }
        
        // Initial call
        assertEquals(1, callCount)
        
        // Update triggers
        state["count"] = 1
        assertEquals(2, callCount)
        
        // Unsubscribe
        unsubscribe()
        
        // No more notifications
        state["count"] = 2
        assertEquals(2, callCount)
    }

    @Test
    fun `should create two-way binding`() {
        val state = NanoState(mapOf("name" to "initial"))
        
        val binding = state.bind("name")
        
        assertEquals("initial", binding.value)
        
        // Update through binding
        binding.updateString("updated")
        
        assertEquals("updated", state["name"])
        assertEquals("updated", binding.value)
    }

    @Test
    fun `should check if key exists`() {
        val state = NanoState(mapOf("count" to 0))
        
        assertTrue(state.has("count"))
        assertFalse(state.has("nonexistent"))
    }

    @Test
    fun `should get snapshot of all values`() {
        val state = NanoState(mapOf(
            "a" to 1,
            "b" to "two"
        ))
        
        val snapshot = state.snapshot()
        
        assertEquals(mapOf("a" to 1, "b" to "two"), snapshot)
    }

    @Test
    fun `should reset state`() {
        val state = NanoState(mapOf("old" to "value"))
        
        state.reset(mapOf("new" to "value"))
        
        assertFalse(state.has("old"))
        assertTrue(state.has("new"))
        assertEquals("value", state["new"])
    }
}

