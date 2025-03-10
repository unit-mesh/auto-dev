package cc.unitmesh.devti.sketch.ui

import com.intellij.openapi.project.Project

class MarkdownPreviewSketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean {
        return lang.lowercase() == "markdown"
    }

    override fun create(project: Project, content: String): ExtensionLangSketch = MarkdownPreviewHighlightSketch(project, content)
}
