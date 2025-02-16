package cc.unitmesh.devti.sketch.ui.openapi

import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.FileEditorSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.util.parser.CodeFence.Companion.findLanguage
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import javax.swing.JComponent


private val editorWithPreviews: List<FileEditorProvider> =
    FileEditorProvider.EP_FILE_EDITOR_PROVIDER.extensionList.filter {
        it.javaClass.simpleName.contains("Preview")
    }

class OpenAPISketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String) = lang == "yaml" || lang == "yml"

    override fun create(
        project: Project,
        content: String
    ): ExtensionLangSketch {
        val isValidOpenAPI = content.contains("openapi:") && content.contains("info:")
        if (!isValidOpenAPI) {
            val language = findLanguage("yaml")
            return object : CodeHighlightSketch(project, content, language), ExtensionLangSketch {
                override fun getExtensionName(): String = "Yaml"
            }
        }

        val file = LightVirtualFile(createFileNameWithTime(), content)
        val fileEditor = editorWithPreviews.map {
            it.accept(project, file)
        }.firstOrNull().let {
            it ?: FileEditorProvider.EP_FILE_EDITOR_PROVIDER.extensionList.firstOrNull {
                it.javaClass.simpleName == "TextEditorProvider"
            }?.createEditor(project, file)
        }

        if (fileEditor != null) {
            return object : FileEditorSketch(project, file, fileEditor.javaClass.simpleName), ExtensionLangSketch {
                override fun getExtensionName(): String = "OpenAPI"
            }
        }

        return OpenAPISketch(project, content)
    }
}

class OpenAPISketch(val myProject: Project, private val content: String) : FileEditorSketch(
    myProject,
    LightVirtualFile(createFileNameWithTime(), content),
    "SwaggerEditorWithPreview"
) {
    override var mainPanel: JComponent = editor.component

    override fun getExtensionName(): String = "OpenAPI"
}

private fun createFileNameWithTime(): String = "openapi-${System.currentTimeMillis()}.yaml"