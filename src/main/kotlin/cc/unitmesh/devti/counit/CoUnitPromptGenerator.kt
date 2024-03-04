package cc.unitmesh.devti.counit

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class CoUnitPromptGenerator(val project: Project) {
    fun findIntention(input: String): String? {
        return null
    }

    fun semanticQuery(query: String): String? {
        return null
    }
}