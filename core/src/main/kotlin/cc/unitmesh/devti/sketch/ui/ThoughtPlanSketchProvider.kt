package cc.unitmesh.devti.sketch.ui

import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import com.intellij.openapi.project.Project

class ThoughtPlanSketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean = lang == "plan"

    override fun create(project: Project, content: String): ExtensionLangSketch {
        return object : CodeHighlightSketch(project, content, null), ExtensionLangSketch {
            override fun getExtensionName(): String = "ThoughtPlan"
        }
    }
}
