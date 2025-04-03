package cc.unitmesh.devti.gui.chat.ui.viewmodel

import cc.unitmesh.devti.gui.chat.ui.FilePresentation
import cc.unitmesh.devti.util.isInProject
import com.intellij.diff.editor.DiffVirtualFileBase
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.EventListener
import javax.swing.DefaultListModel

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
    
    fun addRecentlyOpenedFiles() {
        val files = getRecentlyOpenedFiles()
        files.forEach { file ->
            if (listModel.elements().asSequence().none { it.virtualFile == file.virtualFile }) {
                listModel.addElement(file)
                listeners.forEach { it.onFileAdded(file) }
            }
        }
    }
    
    override fun dispose() {
        listeners.clear()
    }
}
