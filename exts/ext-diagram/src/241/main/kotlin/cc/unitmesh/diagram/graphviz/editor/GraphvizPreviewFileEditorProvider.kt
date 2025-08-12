package cc.unitmesh.diagram.graphviz.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import cc.unitmesh.diagram.graphviz.DotFileType

/**
 * Provider for Graphviz preview file editor
 * Similar to JdlPreviewFileEditorProvider in JHipster UML implementation
 */
class GraphvizPreviewFileEditorProvider : FileEditorProvider {
    
    override fun getEditorTypeId(): String = "graphviz-uml-editor"
    
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
    
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return (file.fileType == DotFileType.INSTANCE || isDotFile(file)) && !isMermaidFile(file)
    }
    
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return GraphvizPreviewFileEditor(project, file)
    }
    
    /**
     * Check if a file is a DOT file based on extension
     */
    private fun isDotFile(file: VirtualFile): Boolean {
        val extension = file.extension?.lowercase()
        return extension == "dot" || extension == "gv" || extension == "graphviz"
    }

    private fun isMermaidFile(file: VirtualFile): Boolean {
        val extension = file.extension?.lowercase()
        return extension == "mmd" || extension == "mermaid"
    }
}
