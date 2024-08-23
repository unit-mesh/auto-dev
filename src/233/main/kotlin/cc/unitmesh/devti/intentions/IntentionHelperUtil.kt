package cc.unitmesh.devti.intentions

import cc.unitmesh.devti.custom.CustomActionBaseIntention
import cc.unitmesh.devti.custom.CustomDocumentationBaseIntention
import cc.unitmesh.devti.custom.TeamPromptBaseIntention
import cc.unitmesh.devti.custom.action.CustomPromptConfig
import cc.unitmesh.devti.custom.team.TeamPromptsBuilder
import cc.unitmesh.devti.intentions.action.base.ChatBaseIntention
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.IntentionActionBean
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

object IntentionHelperUtil {
    val EP_NAME: ExtensionPointName<IntentionActionBean> = ExtensionPointName("cc.unitmesh.autoDevIntention")
    fun getAiAssistantIntentions(project: Project, editor: Editor?, file: PsiFile): List<IntentionAction> {
        val extensionList = EP_NAME.extensionList

        val builtinIntentions = extensionList
            .asSequence()
            .map { it.instance.asIntention() }
            .filter { it.isAvailable(project, editor, file) }
            .toList()

        val promptConfig = CustomPromptConfig.load()
        val customActionIntentions: List<IntentionAction> = promptConfig.prompts.map {
            CustomActionBaseIntention.create(it)
        }

        val livingDocIntentions: List<IntentionAction> = promptConfig.documentations?.map {
            CustomDocumentationBaseIntention.create(it)
        } ?: emptyList()

        val teamPromptsIntentions: List<IntentionAction> = project.service<TeamPromptsBuilder>().default().map {
            TeamPromptBaseIntention.create(it, true)
        }

        val actionList = builtinIntentions + customActionIntentions + livingDocIntentions + teamPromptsIntentions
        return actionList.map { it as ChatBaseIntention }.sortedByDescending { it.priority() }
    }
}