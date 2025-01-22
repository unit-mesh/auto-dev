package cc.unitmesh.devti.sketch.ui.webview

import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.code.HtmlLangSketch
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class WebpageSketch : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean {
        return lang == "html" || lang == "htm"
    }

    override fun create(
        project: Project,
        content: String
    ): ExtensionLangSketch {
        if (content.startsWith("<!DOCTYPE html>") || content.startsWith("<html>")) {
            return WebpageLangSketch(project, content)
        }

        return HtmlLangSketch(project, content)
    }
}

class WebpageLangSketch(val project: Project, val text: String) : ExtensionLangSketch {
    override fun getExtensionName(): String {
        return "Webpage"
    }

    override fun getViewText(): String {
        TODO("Not yet implemented")
    }

    override fun updateViewText(text: String) {
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

