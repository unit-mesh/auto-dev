package cc.unitmesh.devti.mcp.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.WeighedFileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class McpPreviewEditorProvider : WeighedFileEditorProvider() {
    override fun accept(project: Project, file: VirtualFile) = file.name.contains(".mcp.json")

    override fun createEditor(project: Project, virtualFile: VirtualFile): FileEditor {
        return McpPreviewEditor(project, virtualFile)
    }

    override fun getEditorTypeId(): String = "mcp-preview-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
}
