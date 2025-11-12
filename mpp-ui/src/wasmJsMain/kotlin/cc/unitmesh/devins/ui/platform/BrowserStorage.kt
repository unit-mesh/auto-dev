package cc.unitmesh.devins.ui.platform

import kotlinx.browser.localStorage

/**
 * Browser localStorage wrapper for WASM platform
 * Provides a simple key-value storage interface backed by browser's localStorage
 */
object BrowserStorage {
    /**
     * Save a string value to localStorage
     */
    fun setItem(key: String, value: String) {
        try {
            localStorage.setItem(key, value)
        } catch (e: Exception) {
            println("WASM: Failed to save to localStorage: ${e.message}")
        }
    }

    /**
     * Get a string value from localStorage
     */
    fun getItem(key: String): String? {
        return try {
            localStorage.getItem(key)
        } catch (e: Exception) {
            println("WASM: Failed to read from localStorage: ${e.message}")
            null
        }
    }

    /**
     * Remove an item from localStorage
     */
    fun removeItem(key: String) {
        try {
            localStorage.removeItem(key)
        } catch (e: Exception) {
            println("WASM: Failed to remove from localStorage: ${e.message}")
        }
    }

    /**
     * Clear all items from localStorage
     */
    fun clear() {
        try {
            localStorage.clear()
        } catch (e: Exception) {
            println("WASM: Failed to clear localStorage: ${e.message}")
        }
    }

    /**
     * Get all keys from localStorage
     */
    fun keys(): List<String> {
        return try {
            val length = localStorage.length
            (0 until length).mapNotNull { index ->
                localStorage.key(index)
            }
        } catch (e: Exception) {
            println("WASM: Failed to get keys from localStorage: ${e.message}")
            emptyList()
        }
    }

    /**
     * Check if a key exists in localStorage
     */
    fun hasItem(key: String): Boolean {
        return getItem(key) != null
    }
}

/**
 * Console wrapper for WASM platform
 * Uses println for console output
 */
object console {
    fun log(message: String) {
        println("LOG: $message")
    }

    fun error(message: String) {
        println("ERROR: $message")
    }

    fun warn(message: String) {
        println("WARN: $message")
    }

    fun info(message: String) {
        println("INFO: $message")
    }
}

