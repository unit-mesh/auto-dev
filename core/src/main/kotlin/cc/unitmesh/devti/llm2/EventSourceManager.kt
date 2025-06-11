package cc.unitmesh.devti.llm2

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.sse.EventSource
import java.util.concurrent.atomic.AtomicReference

/**
 * Manages EventSource lifecycle with proper cleanup and thread safety.
 * 
 * This class provides safe management of OkHttp EventSource instances,
 * ensuring proper resource cleanup and thread-safe operations.
 */
class EventSourceManager {
    
    companion object {
        private val logger: Logger = logger<EventSourceManager>()
    }
    
    private val currentEventSource = AtomicReference<EventSource?>(null)
    private val mutex = Mutex()
    
    /**
     * Sets a new EventSource, automatically cancelling any existing one.
     * 
     * @param eventSource The new EventSource to manage
     */
    suspend fun setEventSource(eventSource: EventSource?) {
        mutex.withLock {
            // Cancel existing EventSource if present
            val existing = currentEventSource.getAndSet(eventSource)
            if (existing != null && existing !== eventSource) {
                cancelEventSourceSafely(existing)
            }
        }
    }
    
    /**
     * Gets the current EventSource if available.
     * 
     * @return The current EventSource or null if none is set
     */
    fun getCurrentEventSource(): EventSource? {
        return currentEventSource.get()
    }
    
    /**
     * Cancels the current EventSource and clears the reference.
     * This method is thread-safe and can be called multiple times safely.
     */
    suspend fun cancelCurrent() {
        mutex.withLock {
            val eventSource = currentEventSource.getAndSet(null)
            if (eventSource != null) {
                cancelEventSourceSafely(eventSource)
            }
        }
    }
    
    /**
     * Synchronously cancels the current EventSource without waiting.
     * This is useful for cleanup scenarios where blocking is not desirable.
     */
    fun cancelCurrentSync() {
        val eventSource = currentEventSource.getAndSet(null)
        if (eventSource != null) {
            cancelEventSourceSafely(eventSource)
        }
    }
    
    /**
     * Clears the EventSource reference without cancelling.
     * This should only be used when the EventSource has already been closed/cancelled.
     */
    fun clearReference() {
        currentEventSource.set(null)
    }
    
    /**
     * Checks if there's an active EventSource.
     * 
     * @return true if an EventSource is currently set, false otherwise
     */
    fun hasActiveEventSource(): Boolean {
        return currentEventSource.get() != null
    }
    
    /**
     * Safely cancels an EventSource with proper error handling.
     * 
     * @param eventSource The EventSource to cancel
     */
    private fun cancelEventSourceSafely(eventSource: EventSource) {
        try {
            eventSource.cancel()
            logger.debug("EventSource cancelled successfully")
        } catch (e: Exception) {
            logger.warn("Error cancelling EventSource", e)
        }
    }
}