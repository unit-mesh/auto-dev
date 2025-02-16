package cc.unitmesh.devti.sketch.ui

import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.JComponent


interface ExtensionLangSketch : LangSketch {
    fun getExtensionName(): String
}

/**
 * @param withPreviewEditorId means a editor extends from [com.intellij.openapi.fileEditor.TextEditorWithPreview]
 */
abstract class FileEditorSketch(val project: Project, val virtualFile: VirtualFile, val withPreviewEditorId: String) :
    ExtensionLangSketch {
    val editor: FileEditor = getEditorProvider().createEditor(project, virtualFile)

    open val mainPanel = editor.component

    fun getEditorProvider(): FileEditorProvider =
        FileEditorProvider.EP_FILE_EDITOR_PROVIDER.extensionList.firstOrNull {
            it.javaClass.simpleName == withPreviewEditorId
        } ?: TextEditorProvider.getInstance()

    override fun getViewText(): String = virtualFile.readText()

    override fun updateViewText(text: String, complete: Boolean) {
//        if (editor as? TextEditorWithPreview != null) {
//            val document = editor.editor.document
//            document.setText(text)
//        } else {
//            // done nothing
//        }
    }

    fun VirtualFile.readText(): String {
        return VfsUtilCore.loadText(this)
    }

    override fun getComponent(): JComponent = mainPanel

    override fun updateLanguage(language: Language?, originLanguage: String?) {}

    override fun dispose() {}
}

interface LanguageSketchProvider {
    fun isSupported(lang: String): Boolean

    fun create(project: Project, content: String): ExtensionLangSketch

    companion object {
        private val EP_NAME: ExtensionPointName<LanguageSketchProvider> =
            ExtensionPointName("cc.unitmesh.langSketchProvider")

        fun provide(language: String): LanguageSketchProvider? {
            val lang = language.lowercase()
            return EP_NAME.extensionList.firstOrNull {
                it.isSupported(lang)
            }
        }
    }
}