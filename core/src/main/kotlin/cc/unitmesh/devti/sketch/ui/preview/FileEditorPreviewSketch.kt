package cc.unitmesh.devti.sketch.ui.preview

import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LangSketch
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditorWithPreview.Layout
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.JComponent

val editorWithPreviews: List<FileEditorProvider> =
    FileEditorProvider.EP_FILE_EDITOR_PROVIDER.extensionList.filter {
        it.javaClass.simpleName.contains("Preview")
    }

/**
 * @param withPreviewEditorId means a editor extends from [com.intellij.openapi.fileEditor.TextEditorWithPreview]
 */
abstract class FileEditorPreviewSketch(
    val project: Project,
    val virtualFile: VirtualFile,
    val withPreviewEditorId: String
) : ExtensionLangSketch {
    init {
        virtualFile.putUserData(Key.create<Layout>("TextEditorWithPreview.DefaultLayout"), Layout.SHOW_EDITOR_AND_PREVIEW)
    }

    val editorProvider = buildEditorProvider()
    open val editor: FileEditor = editorProvider.createEditor(project, virtualFile)

    open val mainPanel = editor.component

    fun buildEditorProvider(): FileEditorProvider =
        FileEditorProvider.EP_FILE_EDITOR_PROVIDER.extensionList.firstOrNull {
            it.javaClass.simpleName == withPreviewEditorId
        } ?: TextEditorProvider.getInstance()

    override fun getComponent(): JComponent = mainPanel

    override fun getViewText(): String = editor.file.readText()

    override fun updateViewText(text: String, complete: Boolean) {}

    fun VirtualFile.readText(): String {
        return VfsUtilCore.loadText(this)
    }

    fun createRightToolbar(target: JComponent): ActionToolbar {
        val rightToolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.TEXT_EDITOR_WITH_PREVIEW, createViewActionGroup(), true)
        rightToolbar.targetComponent = target
        rightToolbar.isReservePlaceAutoPopupIcon = false

        return rightToolbar
    }

    protected open fun createViewActionGroup(): ActionGroup {
        val actionManager = ActionManager.getInstance()
        return DefaultActionGroup(
            actionManager.getAction("TextEditorWithPreview.Layout.EditorOnly"),
            actionManager.getAction("TextEditorWithPreview.Layout.EditorAndPreview"),
            actionManager.getAction("TextEditorWithPreview.Layout.PreviewOnly")
        )
    }

    override fun dispose() {}

    companion object {
        fun createPreviewEditor(project: Project, file: VirtualFile, content: String, sketchName: String): LangSketch {
            val fileEditor = editorWithPreviews.firstOrNull {
                it.accept(project, file)
            }?.createEditor(project, file)

            if (fileEditor != null) {
                return object : FileEditorPreviewSketch(project, file, fileEditor.javaClass.simpleName),
                    ExtensionLangSketch {
                    override fun getExtensionName(): String = sketchName
                }
            }

            val language = CodeFence.findLanguageByExt(file.extension ?: "")
                ?: CodeFence.findLanguage("txt")

            return object : CodeHighlightSketch(project, content, language), ExtensionLangSketch {
                override fun getExtensionName(): String = sketchName
            }
        }
    }
}