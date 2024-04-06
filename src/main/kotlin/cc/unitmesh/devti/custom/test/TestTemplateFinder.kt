package cc.unitmesh.devti.custom.test

import cc.unitmesh.devti.settings.custom.teamPromptsSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir

@Service(Service.Level.PROJECT)
class TestTemplateFinder(private val project: Project) {
    val settings = project.teamPromptsSettings

    /**
     * Looks up a template file by its name and returns its content as a string.
     *
     * @param templateFileName the name of the template file to look up
     * @return the content of the template file as a string, or null if the file is not found
     *
     * 1. Find the prompts directory
     */
    fun lookup(templateFileName: String): String? {
        val path = settings.state.teamPromptsDir
        val promptsDir = project.guessProjectDir()?.findChild(path) ?: return null
        val templateFile = promptsDir.findChild("templates") ?: return null

        val templateCode = templateFile.children.firstOrNull { it.name == templateFileName }
        if (templateCode != null) {
            return templateCode.inputStream.readBytes().toString(Charsets.UTF_8)
        }

        // second lookup same name with .vm
        val vmTemplate = templateFile.children.firstOrNull { it.name == "$templateFileName.vm" }
        if (vmTemplate != null) {
            return vmTemplate.inputStream.readBytes().toString(Charsets.UTF_8)
        }

        // final search end with filename
        val endSuffix = templateFile.children.firstOrNull { it.name.endsWith(templateFileName) }
        if (endSuffix != null) {
            return endSuffix.inputStream.readBytes().toString(Charsets.UTF_8)
        }

        return null
    }
}