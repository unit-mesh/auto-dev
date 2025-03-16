package cc.unitmesh.devti.sketch.ui

import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.sketch.ui.plan.PlanSketch
import com.intellij.openapi.project.Project

class ThoughtPlanSketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean = lang == "plan"

    override fun create(project: Project, content: String): ExtensionLangSketch {
        val planItems = MarkdownPlanParser.parse(content)
        if (planItems.isNotEmpty()) {
            return PlanSketch(project, content, planItems.toMutableList())
        }

        return object : CodeHighlightSketch(project, content, null), ExtensionLangSketch {
            override fun getExtensionName(): String = "ThoughtPlan"
        }
    }
}

