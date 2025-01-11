package cc.unitmesh.devti.sketch.patch

import cc.unitmesh.devti.sketch.ExtensionLangSketch
import cc.unitmesh.devti.sketch.LanguageSketchProvider
import com.intellij.openapi.project.Project

class DiffLangSketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean = lang == "diff" || lang == "patch"
    override fun create(project: Project, content: String): ExtensionLangSketch = DiffLangSketch(project, content)
}
