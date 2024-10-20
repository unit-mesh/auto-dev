package cc.unitmesh.devti.custom.team

import cc.unitmesh.devti.settings.custom.teamPromptsSettings
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class TeamPromptsBuilder(private val project: Project) {
    private val baseDir get() = project.teamPromptsSettings.state.teamPromptsDir
    private val basePromptDir get() = project.guessProjectDir()?.findChild(baseDir)

    fun default(): List<TeamPromptAction> {
        val promptsDir = basePromptDir ?: return emptyList()
        val filterPrompts = promptsDir.children.filter { it.name.endsWith(".vm") }

        return buildPrompts(filterPrompts)
    }

    /**
     * Quick prompts are the prompts that are used for quick actions, which will load from the quick folder.
     * Format: "<baseDir>/quick/<quick-action-name>.vm",
     * For example: `prompts/quick/quick-action-name.vm`
     */
    fun quickPrompts(): List<TeamPromptAction> {
        val promptsDir = basePromptDir ?: return emptyList()
        val quickPromptDir = promptsDir.findChild("quick") ?: return emptyList()
        val quickPromptFiles = quickPromptDir.children.filter { it.name.endsWith(".vm") }

        return buildPrompts(quickPromptFiles)
    }

    /**
     * Flows are the prompts that are used for flow actions, which will load from the flows folder.
     * Format: "<baseDir>/flows/<flow-action-name>.devin",
     * For example: `prompts/flows/flow-action-name.devin`
     */
    fun flows(): List<VirtualFile> {
        val promptsDir = basePromptDir ?: return emptyList()
        val promptDir = promptsDir.findChild("flows") ?: return emptyList()
        return promptDir.children.filter { it.name.endsWith(".devin") }
    }

    private fun buildPrompts(prompts: List<VirtualFile>): List<TeamPromptAction> {
        return prompts.map {
            // a prompt should be named as <actionName>.vm, and we need to replace - with " "
            val promptName = it.nameWithoutExtension.replace("-", " ")

            val promptContent = runReadAction { it.inputStream.readBytes().toString(Charsets.UTF_8) }
            val actionPrompt = CustomActionPrompt.fromContent(promptContent)

            TeamPromptAction(promptName, actionPrompt)
        }
    }

    fun overrideTemplate(pathPrefix: String, filename: String): String? {
        val promptsDir = basePromptDir ?: return null
        val path = "$pathPrefix/$filename"

        val overrideFile = promptsDir.findFileByRelativePath(path) ?: return null
        return runReadAction { overrideFile.inputStream.readBytes().toString(Charsets.UTF_8) }
    }
}

data class TeamPromptAction(
    val actionName: String,
    val actionPrompt: CustomActionPrompt = CustomActionPrompt(),
)
