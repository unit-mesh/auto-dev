package cc.unitmesh.go.provider

import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.sketch.ui.preview.FileEditorPreviewSketch
import com.goide.GoLanguage
import com.goide.playground.ui.GoPlaygroundFileEditorWithPreview
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import javax.swing.JComponent

class GoLangPlaygroundSketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean = lang == "go"

    override fun create(project: Project, content: String): ExtensionLangSketch {
        if (content.contains("package main")) {
            val file = LightVirtualFile("main.go", content)
            return GoLangPlaygroundSketch(project, content, file)
        }

        return object : CodeHighlightSketch(project, content, GoLanguage.INSTANCE), ExtensionLangSketch {
            override fun getExtensionName(): String = "GoHighlight"
        }
    }
}

class GoLangPlaygroundSketch(val myProject: Project, val content: String, val file: LightVirtualFile) :
    FileEditorPreviewSketch(myProject, file, "GoPlaygroundFileEditorWithPreview") {
    override fun getExtensionName(): String = "GoMain"

    val editorWithPreview = createEditorWithPreview(myProject, file) as? GoPlaygroundFileEditorWithPreview

    override val mainPanel: JComponent
        get() {
            return editorWithPreview?.component ?: editor.component
        }

    override fun getComponent(): JComponent {
        return editorWithPreview?.component ?: editor.component
    }

    override fun getViewText(): String {
        return editorWithPreview?.editor?.document?.text ?: editor.file.readText()
    }

    companion object {
        fun createEditorWithPreview(project: Project, file: VirtualFile): FileEditor {
            val textEditorProvider = TextEditorProvider.getInstance()
            val editorFactory = EditorFactory.getInstance()
            val editor = textEditorProvider.createEditor(project, file)

            return if (editor is TextEditor) {
                val viewer = editorFactory.createViewer(
                    editorFactory.createDocument(""),
                    project,
                    EditorKind.PREVIEW
                )
                val previewEditor = textEditorProvider.getTextEditor(viewer)

                Disposer.register(editor, Disposable {
                    EditorFactory.getInstance().releaseEditor(viewer)
                })

                GoPlaygroundFileEditorWithPreview(editor, previewEditor)
            } else {
                editor
            }
        }
    }
}
