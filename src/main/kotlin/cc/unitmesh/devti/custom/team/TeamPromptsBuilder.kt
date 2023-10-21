package cc.unitmesh.devti.custom.team

import cc.unitmesh.devti.settings.custom.teamPromptsSettings
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir

@Service(Service.Level.PROJECT)
class TeamPromptsBuilder(private val project: Project) {
    val settings = project.teamPromptsSettings
    fun build(): List<TeamPromptAction> {
        val path = settings.state.teamPromptsDir
        val promptsDir = project.guessProjectDir()?.findChild(path) ?: return emptyList()

        return promptsDir.children.filter { it.name.endsWith(".vm") }.map {
            // a prompt should be named as <actionName>.vm, and we need to replace - with " "
            val promptName = it.nameWithoutExtension.replace("-", " ")
            // load content of the prompt file
            val promptContent = runReadAction { it.inputStream.readBytes().toString(Charsets.UTF_8) }
            val actionPrompt = TeamActionPrompt.fromContent(promptContent)
            TeamPromptAction(promptName, actionPrompt)
        }
    }
}

data class TeamPromptAction(
    val actionName: String,
    val actionPrompt: TeamActionPrompt = TeamActionPrompt(),
)
