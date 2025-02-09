package cc.unitmesh.devti.sketch.ui.openapi

import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.util.parser.CodeFence.Companion.findLanguage
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
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

class OpenAPISketch(private val project: Project, private val content: String) : ExtensionLangSketch {
    override fun getExtensionName(): String {
        TODO("Not yet implemented")
    }

    override fun getViewText(): String {
        TODO("Not yet implemented")
    }

    override fun updateViewText(text: String, complete: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getComponent(): JComponent {
        TODO("Not yet implemented")
    }

    override fun updateLanguage(language: Language?, originLanguage: String?) {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }
}