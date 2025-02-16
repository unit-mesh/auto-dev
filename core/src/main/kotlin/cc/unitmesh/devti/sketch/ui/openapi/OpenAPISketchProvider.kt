package cc.unitmesh.devti.sketch.ui.openapi

import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.FileEditorSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.util.parser.CodeFence.Companion.findLanguage
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import javax.swing.JComponent

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