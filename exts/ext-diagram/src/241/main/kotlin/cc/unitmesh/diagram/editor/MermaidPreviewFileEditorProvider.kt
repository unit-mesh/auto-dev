package cc.unitmesh.diagram.editor

import cc.unitmesh.diagram.MermaidFileType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Provider for Mermaid preview file editor
 * Similar to GraphvizPreviewFileEditorProvider but for Mermaid files
 */
class MermaidPreviewFileEditorProvider : FileEditorProvider {
    
    override fun getEditorTypeId(): String = "mermaid-uml-editor"
    
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
    
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.fileType == MermaidFileType.INSTANCE || isMermaidFile(file)
    }
    
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return MermaidPreviewFileEditor(project, file)
    }
    
    private fun isMermaidFile(file: VirtualFile): Boolean {
        val extension = file.extension?.lowercase()
        return extension == "mmd" || extension == "mermaid"
    }
}
