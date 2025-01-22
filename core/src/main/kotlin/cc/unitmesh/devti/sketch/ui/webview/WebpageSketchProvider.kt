package cc.unitmesh.devti.sketch.ui.webview

import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.code.HtmlLangSketch
import com.intellij.openapi.project.Project

class WebpageSketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean {
        return lang.lowercase() == "html" || lang.lowercase() == "htm"
    }

    override fun create(project: Project, content: String): ExtensionLangSketch {
        if (content.startsWith("<!DOCTYPE html>") || content.startsWith("<html>")) {
            return WebpageLangSketch(project, content)
        }

        return HtmlLangSketch(project, content)
    }
}

