package cc.unitmesh.devti.sketch.ui

import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.JComponent
import kotlin.jvm.javaClass


interface ExtensionLangSketch : LangSketch {
    fun getExtensionName(): String
}

abstract class FileEditorSketch(val project: Project, val virtualFile: VirtualFile, val editorProviderId: String) :
    ExtensionLangSketch {
    val editor = getEditorProvider().createEditor(project, virtualFile) as TextEditorWithPreview

    open val mainPanel = editor.component

    fun getEditorProvider(): FileEditorProvider =
        FileEditorProvider.EP_FILE_EDITOR_PROVIDER.extensionList.firstOrNull {
            it.javaClass.simpleName == editorProviderId
        } ?: TextEditorProvider.getInstance()

    override fun getViewText(): String = virtualFile.readText()

    override fun updateViewText(text: String, complete: Boolean) {
        val document = editor.editor.document
        document.setText(text)
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