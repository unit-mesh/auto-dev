package cc.unitmesh.mermaid

import com.intellij.lang.Language
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import com.intellij.openapi.vfs.VfsUtilCore
import javax.swing.JComponent
import javax.swing.JPanel

class MermaidSketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean {
        return lang == "mermaid" || lang == "mmd"
    }

    override fun create(project: Project, content: String): ExtensionLangSketch {
        val file = LightVirtualFile("mermaid.mermaid", content)
        return MermaidSketch(project, file)
    }
}

class MermaidSketch(project: Project, private val virtualFile: VirtualFile) : ExtensionLangSketch {
    private var mainPanel: JPanel

    init {
        val editor = getEditorProvider().createEditor(project, virtualFile) as TextEditorWithPreview
        mainPanel = panel {
            row {
                cell(editor.component).align(Align.FILL)
            }
        }.apply {
            border = JBUI.Borders.empty(0, 10)
        }
    }

    private fun getEditorProvider(): FileEditorProvider =
        FileEditorProvider.EP_FILE_EDITOR_PROVIDER.extensionList.firstOrNull {
            it.javaClass.simpleName == "MermaidEditorWithPreviewProvider"
        } ?: TextEditorProvider.getInstance()

    override fun getExtensionName(): String = "mermaid"

    override fun getViewText(): String = virtualFile.readText()

    override fun updateViewText(text: String) {}

    override fun getComponent(): JComponent = mainPanel

    override fun updateLanguage(language: Language?, originLanguage: String?) {}

    override fun dispose() {}
}

fun VirtualFile.readText(): String {
    return VfsUtilCore.loadText(this)
}
