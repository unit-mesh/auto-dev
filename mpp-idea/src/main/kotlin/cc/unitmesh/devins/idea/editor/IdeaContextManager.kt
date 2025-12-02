package cc.unitmesh.devins.idea.editor

import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages context files for the AI assistant.
 * Provides state management for selected files, default context, and rules.
 *
 * Features:
 * - Auto-add current editor file to context
 * - Related classes suggestion via LookupManagerListener
 * - Default context preset management
 * - Context rules (file patterns, include/exclude)
 */
@Service(Service.Level.PROJECT)
class IdeaContextManager(private val project: Project) : Disposable {

    // Selected files in the current context
    private val _selectedFiles = MutableStateFlow<List<VirtualFile>>(emptyList())
    val selectedFiles: StateFlow<List<VirtualFile>> = _selectedFiles.asStateFlow()

    // Default context files (saved preset)
    private val _defaultContextFiles = MutableStateFlow<List<VirtualFile>>(emptyList())
    val defaultContextFiles: StateFlow<List<VirtualFile>> = _defaultContextFiles.asStateFlow()

    // Context rules
    private val _rules = MutableStateFlow<List<ContextRule>>(emptyList())
    val rules: StateFlow<List<ContextRule>> = _rules.asStateFlow()

    // Related files suggested by the system
    private val _relatedFiles = MutableStateFlow<List<VirtualFile>>(emptyList())
    val relatedFiles: StateFlow<List<VirtualFile>> = _relatedFiles.asStateFlow()

    // Auto-add current file setting
    private val _autoAddCurrentFile = MutableStateFlow(true)
    val autoAddCurrentFile: StateFlow<Boolean> = _autoAddCurrentFile.asStateFlow()

    // Listeners setup flag
    private var listenersSetup = false

    init {
        setupListeners()
    }

    /**
     * Setup editor and lookup listeners for auto-adding files
     */
    private fun setupListeners() {
        if (listenersSetup) return
        listenersSetup = true

        // Listen to file editor changes
        project.messageBus.connect(this).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    if (!_autoAddCurrentFile.value) return
                    val file = event.newFile ?: return
                    if (canBeAdded(file)) {
                        ApplicationManager.getApplication().invokeLater {
                            addRelatedFile(file)
                        }
                    }
                }
            }
        )

        // Initialize with current file
        val currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        currentFile?.let {
            if (canBeAdded(it)) {
                addRelatedFile(it)
            }
        }
    }

    /**
     * Add a file to the selected context
     */
    fun addFile(file: VirtualFile) {
        if (!file.isValid) return
        val current = _selectedFiles.value.toMutableList()
        if (current.none { it.path == file.path }) {
            current.add(file)
            _selectedFiles.value = current
        }
    }

    /**
     * Add multiple files to the selected context
     */
    fun addFiles(files: List<VirtualFile>) {
        val current = _selectedFiles.value.toMutableList()
        files.filter { it.isValid && current.none { existing -> existing.path == it.path } }
            .forEach { current.add(it) }
        _selectedFiles.value = current
    }

    /**
     * Remove a file from the selected context
     */
    fun removeFile(file: VirtualFile) {
        _selectedFiles.value = _selectedFiles.value.filter { it.path != file.path }
    }

    /**
     * Clear all selected files
     */
    fun clearContext() {
        _selectedFiles.value = emptyList()
        _relatedFiles.value = emptyList()
    }

    /**
     * Set the current selection as default context
     */
    fun setAsDefaultContext() {
        _defaultContextFiles.value = _selectedFiles.value.toList()
    }

    /**
     * Load the default context
     */
    fun loadDefaultContext() {
        val defaults = _defaultContextFiles.value
        if (defaults.isNotEmpty()) {
            _selectedFiles.value = defaults.filter { it.isValid }
        }
    }

    /**
     * Clear the default context
     */
    fun clearDefaultContext() {
        _defaultContextFiles.value = emptyList()
    }

    /**
     * Check if default context is set
     */
    fun hasDefaultContext(): Boolean = _defaultContextFiles.value.isNotEmpty()

    /**
     * Add a related file (from editor listener or lookup)
     */
    private fun addRelatedFile(file: VirtualFile) {
        if (!file.isValid) return
        val current = _relatedFiles.value.toMutableList()
        if (current.none { it.path == file.path }) {
            // Keep only the most recent 10 related files
            if (current.size >= 10) {
                current.removeAt(current.size - 1)
            }
            current.add(0, file)
            _relatedFiles.value = current
        }
    }

    /**
     * Add a context rule
     */
    fun addRule(rule: ContextRule) {
        val current = _rules.value.toMutableList()
        current.add(rule)
        _rules.value = current
    }

    /**
     * Remove a context rule
     */
    fun removeRule(rule: ContextRule) {
        _rules.value = _rules.value.filter { it.id != rule.id }
    }

    /**
     * Clear all rules
     */
    fun clearRules() {
        _rules.value = emptyList()
    }

    /**
     * Toggle auto-add current file setting
     */
    fun setAutoAddCurrentFile(enabled: Boolean) {
        _autoAddCurrentFile.value = enabled
    }

    /**
     * Check if a file can be added to context
     */
    private fun canBeAdded(file: VirtualFile): Boolean {
        if (!file.isValid) return false
        if (file.isDirectory) return false

        // Skip binary files
        val extension = file.extension?.lowercase() ?: ""
        val binaryExtensions = setOf(
            "jar", "class", "exe", "dll", "so", "dylib",
            "png", "jpg", "jpeg", "gif", "ico", "pdf",
            "zip", "tar", "gz", "rar", "7z"
        )
        if (extension in binaryExtensions) return false

        return true
    }

    override fun dispose() {
        // Cleanup if needed
    }

    companion object {
        fun getInstance(project: Project): IdeaContextManager = project.service()
    }
}

/**
 * Represents a context rule for filtering files
 */
data class ContextRule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: ContextRuleType,
    val pattern: String,
    val enabled: Boolean = true
)

/**
 * Types of context rules
 */
enum class ContextRuleType {
    INCLUDE_PATTERN,  // Include files matching pattern
    EXCLUDE_PATTERN,  // Exclude files matching pattern
    FILE_EXTENSION,   // Filter by file extension
    DIRECTORY         // Include/exclude directory
}

