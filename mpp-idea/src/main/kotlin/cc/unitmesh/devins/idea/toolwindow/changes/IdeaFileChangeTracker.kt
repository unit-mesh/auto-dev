package cc.unitmesh.devins.idea.toolwindow.changes

import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.agent.diff.FileChange
import cc.unitmesh.agent.diff.FileChangeTracker
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListListener
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

/**
 * Bridge between IntelliJ VCS/VFS and the cross-platform FileChangeTracker.
 *
 * This service monitors file changes in the project and syncs them to the
 * FileChangeTracker for display in the IdeaFileChangeSummary component.
 *
 * It can track changes from:
 * 1. VCS ChangeListManager (for VCS-tracked changes)
 * 2. VFS BulkFileListener (for real-time file modifications)
 */
@Service(Service.Level.PROJECT)
class IdeaFileChangeTracker(private val project: Project) : Disposable {

    private val changeListManager = ChangeListManager.getInstance(project)
    private var isTracking = false

    // Cache of original file contents before changes
    private val originalContents = mutableMapOf<String, String?>()

    init {
        // Subscribe to VFS events for real-time file change tracking
        project.messageBus.connect(this).subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun before(events: List<VFileEvent>) {
                    if (!isTracking) return
                    
                    events.forEach { event ->
                        when (event) {
                            is VFileContentChangeEvent -> {
                                // Cache original content before change
                                val file = event.file
                                if (isProjectFile(file)) {
                                    val document = FileDocumentManager.getInstance().getDocument(file)
                                    originalContents[file.path] = document?.text
                                }
                            }
                            is VFileDeleteEvent -> {
                                // Cache content before deletion
                                val file = event.file
                                if (isProjectFile(file)) {
                                    val document = FileDocumentManager.getInstance().getDocument(file)
                                    originalContents[file.path] = document?.text
                                }
                            }
                        }
                    }
                }

                override fun after(events: List<VFileEvent>) {
                    if (!isTracking) return
                    
                    events.forEach { event ->
                        when (event) {
                            is VFileContentChangeEvent -> {
                                val file = event.file
                                if (isProjectFile(file)) {
                                    val document = FileDocumentManager.getInstance().getDocument(file)
                                    val newContent = document?.text
                                    val originalContent = originalContents.remove(file.path)
                                    
                                    if (originalContent != newContent) {
                                        recordChange(
                                            filePath = file.path,
                                            changeType = ChangeType.EDIT,
                                            originalContent = originalContent,
                                            newContent = newContent
                                        )
                                    }
                                }
                            }
                            is VFileCreateEvent -> {
                                val file = event.file
                                if (file != null && isProjectFile(file)) {
                                    val document = FileDocumentManager.getInstance().getDocument(file)
                                    recordChange(
                                        filePath = file.path,
                                        changeType = ChangeType.CREATE,
                                        originalContent = null,
                                        newContent = document?.text
                                    )
                                }
                            }
                            is VFileDeleteEvent -> {
                                val file = event.file
                                if (isProjectFile(file)) {
                                    val originalContent = originalContents.remove(file.path)
                                    recordChange(
                                        filePath = file.path,
                                        changeType = ChangeType.DELETE,
                                        originalContent = originalContent,
                                        newContent = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * Start tracking file changes
     */
    fun startTracking() {
        isTracking = true
    }

    /**
     * Stop tracking file changes
     */
    fun stopTracking() {
        isTracking = false
        originalContents.clear()
    }

    /**
     * Record a file change to the FileChangeTracker
     */
    fun recordChange(
        filePath: String,
        changeType: ChangeType,
        originalContent: String?,
        newContent: String?
    ) {
        val change = FileChange(
            filePath = filePath,
            changeType = changeType,
            originalContent = originalContent,
            newContent = newContent
        )
        FileChangeTracker.recordChange(change)
    }

    /**
     * Sync VCS changes to FileChangeTracker
     */
    fun syncVcsChanges() {
        val changes = changeListManager.defaultChangeList.changes
        changes.forEach { change ->
            val fileChange = convertVcsChange(change)
            if (fileChange != null) {
                FileChangeTracker.recordChange(fileChange)
            }
        }
    }

    /**
     * Convert IntelliJ VCS Change to FileChange
     */
    private fun convertVcsChange(change: Change): FileChange? {
        val virtualFile = change.virtualFile ?: return null
        val filePath = virtualFile.path

        val changeType = when (change.type) {
            Change.Type.NEW -> ChangeType.CREATE
            Change.Type.DELETED -> ChangeType.DELETE
            Change.Type.MOVED -> ChangeType.RENAME
            else -> ChangeType.EDIT
        }

        val originalContent = change.beforeRevision?.content
        val newContent = change.afterRevision?.content

        return FileChange(
            filePath = filePath,
            changeType = changeType,
            originalContent = originalContent,
            newContent = newContent
        )
    }

    /**
     * Check if a file is within the project
     */
    private fun isProjectFile(file: VirtualFile): Boolean {
        val projectBasePath = project.basePath ?: return false
        return file.path.startsWith(projectBasePath)
    }

    override fun dispose() {
        stopTracking()
    }

    companion object {
        fun getInstance(project: Project): IdeaFileChangeTracker {
            return project.getService(IdeaFileChangeTracker::class.java)
        }
    }
}
