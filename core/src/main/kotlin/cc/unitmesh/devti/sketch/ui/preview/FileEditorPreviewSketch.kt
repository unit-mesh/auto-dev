package cc.unitmesh.devti.sketch.ui.preview

import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LangSketch
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.lang.Language
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
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
) :
    ExtensionLangSketch {
    val editor: FileEditor = getEditorProvider().createEditor(project, virtualFile)

    open val mainPanel = editor.component

    fun getEditorProvider(): FileEditorProvider =
        FileEditorProvider.EP_FILE_EDITOR_PROVIDER.extensionList.firstOrNull {
            it.javaClass.simpleName == withPreviewEditorId
        } ?: TextEditorProvider.Companion.getInstance()

    override fun getComponent(): JComponent = mainPanel

    override fun getViewText(): String = virtualFile.readText()

    override fun updateViewText(text: String, complete: Boolean) {}

    fun VirtualFile.readText(): String {
        return VfsUtilCore.loadText(this)
    }

    override fun updateLanguage(language: Language?, originLanguage: String?) {}
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

            val language = CodeFence.Companion.findLanguageByExt(file.extension ?: "")
                ?: CodeFence.Companion.findLanguage("txt")

            return object : CodeHighlightSketch(project, content, language), ExtensionLangSketch {
                override fun getExtensionName(): String = sketchName
            }
        }
    }
}