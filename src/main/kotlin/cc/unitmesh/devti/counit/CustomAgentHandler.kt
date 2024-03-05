package cc.unitmesh.devti.counit

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class CustomAgentHandler(val project: Project) {
    fun executeQuery(input: String, selectedAgent: Any): String? {
        return null
    }

    fun semanticQuery(query: String): String? {
        return null
    }
}