package cc.unitmesh.devti.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import cc.unitmesh.devti.DevtiBundle

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {

    var OpenAIToken: String? = null
    var githubToken: String? = null

    init {
        thisLogger().info(DevtiBundle.message("projectService", project.name))
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    fun getRandomNumber() = (1..100).random()
}
