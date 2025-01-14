package cc.unitmesh.devti.sketch.ui.patch

import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import com.intellij.openapi.project.Project

class DiffLangSketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean = lang == "diff" || lang == "patch"
    override fun create(project: Project, content: String): ExtensionLangSketch = DiffLangSketch(project, content)
}
