package cc.unitmesh.devti.custom.test

import cc.unitmesh.devti.settings.custom.teamPromptsSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir

@Service(Service.Level.PROJECT)
class TemplatedTestPrompt(private val project: Project) {
    val settings = project.teamPromptsSettings

    fun lookup(fileName: String): String? {
        val path = settings.state.teamPromptsDir
        val promptsDir = project.guessProjectDir()?.findChild(path) ?: return null
        val templateFile = promptsDir.findChild(fileName) ?: return null

        val templateCode = templateFile.children.firstOrNull { it.name.endsWith(".vm") } ?: return null
        return templateCode.inputStream.readBytes().toString(Charsets.UTF_8)
    }
}