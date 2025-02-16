package cc.unitmesh.devti.sketch.ui

import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.util.parser.CodeFence
import cc.unitmesh.devti.util.parser.CodeFence.Companion.findLanguage
import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import javax.swing.JComponent


interface ExtensionLangSketch : LangSketch {
    fun getExtensionName(): String
}


private val editorWithPreviews: List<FileEditorProvider> =
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

    companion object {
        fun createPreviewEditor(content: String, project: Project, extName: String): LangSketch {
            val file = LightVirtualFile("${System.currentTimeMillis()}.$extName", content)
            val fileEditor = editorWithPreviews.map {
                it.accept(project, file)
            }.firstOrNull().let {
                it ?: FileEditorProvider.EP_FILE_EDITOR_PROVIDER.extensionList.firstOrNull {
                    it.javaClass.simpleName == "TextEditorProvider"
                }?.createEditor(project, file)
            }

            if (fileEditor != null) {
                return object : FileEditorPreviewSketch(project, file, fileEditor.javaClass.simpleName),
                    ExtensionLangSketch {
                    override fun getExtensionName(): String = "Preview"
                }
            }

            val language = CodeFence.findLanguageByExt(extName) ?: findLanguage("txt")
            return object : CodeHighlightSketch(project, content, language), ExtensionLangSketch {
                override fun getExtensionName(): String = extName
            }
        }
    }
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