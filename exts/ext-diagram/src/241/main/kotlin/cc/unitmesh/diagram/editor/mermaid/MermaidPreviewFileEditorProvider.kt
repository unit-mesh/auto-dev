package cc.unitmesh.diagram.editor.mermaid

import cc.unitmesh.diagram.MermaidFileType
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.ex.StructureViewFileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Provider for Mermaid preview file editor
 * Similar to GraphvizPreviewFileEditorProvider but for Mermaid files
 */
class MermaidPreviewFileEditorProvider : FileEditorProvider, DumbAware, StructureViewFileEditorProvider {
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

    override fun getStructureViewBuilder(project: Project, file: VirtualFile): StructureViewBuilder? {
        return null
    }
}
