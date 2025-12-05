package cc.unitmesh.devti.mcp.editor

import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

class McpEditorProvider : WeighedFileEditorProvider() {
    override fun getEditorTypeId() = "mcp-split-editor"
    private val mainProvider: TextEditorProvider = TextEditorProvider.getInstance()
    private val previewProvider: FileEditorProvider = McpPreviewEditorProvider()

    override fun accept(project: Project, file: VirtualFile) = file.name.contains(".mcp.json")

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val editor = TextEditorProvider.getInstance().createEditor(project, file)
        if (editor.file is LightVirtualFile) {
            return editor
        }

        val mainEditor = mainProvider.createEditor(project, file) as TextEditor
        val preview = previewProvider.createEditor(project, file) as McpPreviewEditor
        return McpFileEditorWithPreview(mainEditor, preview, project)
    }

    override fun getPolicy() = FileEditorPolicy.HIDE_OTHER_EDITORS
}

