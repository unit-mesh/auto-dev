package cc.unitmesh.devti.gui.chat.ui.viewmodel

import cc.unitmesh.devti.gui.chat.ui.file.FilePresentation
import cc.unitmesh.devti.util.isInProject
import com.intellij.diff.editor.DiffVirtualFileBase
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.EventListener
import javax.swing.DefaultListModel
import java.awt.Point

/**
 * ViewModel for managing the list of file presentations.
 */
class FileListViewModel(private val project: Project) : Disposable {
    private val listModel = DefaultListModel<FilePresentation>()
    
    interface FileListChangeListener : EventListener {
        fun onFileAdded(file: FilePresentation)
        fun onFileRemoved(file: FilePresentation)
        fun onListCleared()
    }
    
    enum class FileActionType {
        INSERT,
        REMOVE,
        NONE
    }
    
    private val listeners = mutableListOf<FileListChangeListener>()
    
    fun addChangeListener(listener: FileListChangeListener) {
        listeners.add(listener)
    }
    
    fun removeChangeListener(listener: FileListChangeListener) {
        listeners.remove(listener)
    }
    
    fun getListModel(): DefaultListModel<FilePresentation> = listModel
    
    fun addFileIfAbsent(vfile: VirtualFile, first: Boolean = false) {
        if (!vfile.isValid || vfile.fileType.isBinary) return
        if (!isInProject(vfile, project)) return
        if (vfile is DiffVirtualFileBase) return

        if (listModel.elements().asSequence().none { it.virtualFile == vfile }) {
            val filePresentation = FilePresentation(vfile)
            if (first) {
                listModel.insertElementAt(filePresentation, 0)
            } else {
                listModel.addElement(filePresentation)
            }
            
            listeners.forEach { it.onFileAdded(filePresentation) }
        }
    }
    
    fun removeFile(file: FilePresentation) {
        if (listModel.contains(file)) {
            listModel.removeElement(file)
            listeners.forEach { it.onFileRemoved(file) }
        }
    }
    
    fun removeFileByVirtualFile(vfile: VirtualFile) {
        val filePresentation = listModel.elements().asSequence()
            .find { it.virtualFile == vfile }
        
        filePresentation?.let {
            listModel.removeElement(it)
            listeners.forEach { listener -> listener.onFileRemoved(it) }
        }
    }
    
    fun clearAllFiles() {
        listModel.removeAllElements()
        listeners.forEach { it.onListCleared() }
    }
    
    fun getRecentlyOpenedFiles(maxFiles: Int = 12): List<FilePresentation> {
        val fileEditorManager = FileEditorManager.getInstance(project)
        return fileEditorManager.openFiles
            .take(maxFiles)
            .map { FilePresentation.from(project, it) }
            .toMutableList()
    }
    
    fun addRecentlyOpenedFiles(): List<FilePresentation> {
        val files = getRecentlyOpenedFiles()
        files.forEach { file ->
            if (listModel.elements().asSequence().none { it.virtualFile == file.virtualFile }) {
                listModel.addElement(file)
                listeners.forEach { it.onFileAdded(file) }
            }
        }

        return files
    }
    
    /**
     * Handles file operations based on the action type.
     * 
     * @param filePresentation The file presentation to operate on
     * @param actionType The type of action to perform
     * @param callback Callback to handle UI-specific operations
     * @return True if an action was performed, false otherwise
     */
    fun handleFileAction(filePresentation: FilePresentation,
                         actionType: FileActionType,
                         callback: (VirtualFile, String?) -> Unit): Boolean {
        when (actionType) {
            FileActionType.INSERT -> {
                val vfile = filePresentation.virtualFile
                if (!vfile.isValid) return false
                
                // Get relative path for display
                val relativePath = try {
                    project.basePath?.let { basePath ->
                        vfile.path.substringAfter(basePath).removePrefix("/")
                    } ?: vfile.path
                } catch (e: Exception) {
                    vfile.path
                }
                
                // Remove and re-add to prioritize
                removeFile(filePresentation)
                addFileIfAbsent(vfile)
                
                // Call back to UI with file and path
                callback(vfile, relativePath)
                
                // Remove from list after processing
                removeFileByVirtualFile(vfile)
                return true
            }
            FileActionType.REMOVE -> {
                removeFile(filePresentation)
                return true
            }
            FileActionType.NONE -> return false
        }
    }
    
    /**
     * Determines the appropriate action for a file based on component coordinates.
     * 
     * @param filePresentation The file presentation
     * @param componentPoint The local point in the component
     * @param componentBounds The bounds of the component cell
     * @return The appropriate action type
     */
    fun determineFileAction(filePresentation: FilePresentation, componentPoint: Point, 
                            componentBounds: java.awt.Rectangle): FileActionType {
        // Extract component hit detection logic
        val hitComponent = filePresentation.panel?.components?.firstOrNull { 
            it.contains(componentPoint.x - componentBounds.x - it.x, it.height - 1) 
        }
        
        return when {
            hitComponent is javax.swing.JPanel -> FileActionType.INSERT
            hitComponent is javax.swing.JLabel && 
                hitComponent.icon == com.intellij.icons.AllIcons.Actions.Close -> FileActionType.REMOVE
            else -> FileActionType.NONE
        }
    }
    
    override fun dispose() {
        listeners.clear()
    }
}
