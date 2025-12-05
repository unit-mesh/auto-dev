package cc.unitmesh.devti.language.debugger.editor

import cc.unitmesh.devti.language.DevInFileType
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile


class ShireSplitEditorProvider : WeighedFileEditorProvider() {
    override fun getEditorTypeId() = "devin-split-editor"
    private val mainProvider: TextEditorProvider = TextEditorProvider.getInstance()
    private val previewProvider: FileEditorProvider = ShirePreviewEditorProvider()

    override fun accept(project: Project, file: VirtualFile) =
        FileTypeRegistry.getInstance().isFileOfType(file, DevInFileType.INSTANCE)

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val editor = TextEditorProvider.getInstance().createEditor(project, file)
        if (editor.file is LightVirtualFile) {
            return editor
        }

        val mainEditor = mainProvider.createEditor(project, file) as TextEditor
        val preview = previewProvider.createEditor(project, file) as ShirePreviewEditor
        return ShireFileEditorWithPreview(mainEditor, preview, project)
    }

    override fun getPolicy() = FileEditorPolicy.HIDE_OTHER_EDITORS
}

