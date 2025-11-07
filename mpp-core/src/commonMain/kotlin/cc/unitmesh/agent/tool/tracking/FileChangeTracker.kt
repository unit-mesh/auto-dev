package cc.unitmesh.agent.tool.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks file changes made by tools (e.g., WriteFileTool, EditFileTool)
 * This is a cross-platform singleton that maintains a list of all file changes
 * in the current session.
 */
object FileChangeTracker {
    private val _changes = MutableStateFlow<List<FileChange>>(emptyList())
    
    /**
     * Observable state of all file changes
     */
    val changes: StateFlow<List<FileChange>> = _changes.asStateFlow()
    
    private val listeners = mutableListOf<FileChangeListener>()
    
    /**
     * Record a new file change
     */
    fun recordChange(change: FileChange) {
        val currentChanges = _changes.value.toMutableList()
        currentChanges.add(change)
        _changes.value = currentChanges
        
        // Notify all listeners
        listeners.forEach { listener ->
            listener.onFileChanged(change)
        }
    }
    
    /**
     * Add a listener to be notified of file changes
     */
    fun addListener(listener: FileChangeListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }
    
    /**
     * Remove a listener
     */
    fun removeListener(listener: FileChangeListener) {
        listeners.remove(listener)
    }
    
    /**
     * Clear all tracked changes
     */
    fun clearChanges() {
        _changes.value = emptyList()
    }
    
    /**
     * Get the current list of changes (snapshot)
     */
    fun getChanges(): List<FileChange> = _changes.value
    
    /**
     * Get changes for a specific file path
     */
    fun getChangesForFile(filePath: String): List<FileChange> {
        return _changes.value.filter { it.filePath == filePath }
    }
    
    /**
     * Get the count of changes
     */
    fun getChangeCount(): Int = _changes.value.size
    
    /**
     * Remove a specific change
     */
    fun removeChange(change: FileChange) {
        val currentChanges = _changes.value.toMutableList()
        currentChanges.remove(change)
        _changes.value = currentChanges
    }
    
    /**
     * Get unique file paths that were changed
     */
    fun getChangedFilePaths(): List<String> {
        return _changes.value.map { it.filePath }.distinct()
    }
}

/**
 * Listener interface for file changes
 */
interface FileChangeListener {
    /**
     * Called when a file is changed
     */
    fun onFileChanged(change: FileChange)
}

