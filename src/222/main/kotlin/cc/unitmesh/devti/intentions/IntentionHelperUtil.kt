package cc.unitmesh.devti.intentions

import cc.unitmesh.devti.custom.CustomActionIntention
import cc.unitmesh.devti.custom.CustomDocumentationIntention
import cc.unitmesh.devti.custom.TeamPromptIntention
import cc.unitmesh.devti.custom.action.CustomPromptConfig
import cc.unitmesh.devti.custom.team.TeamPromptsBuilder
import cc.unitmesh.devti.intentions.action.base.AbstractChatIntention
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.IntentionActionBean
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

object IntentionHelperUtil {
    val EP_NAME: ExtensionPointName<IntentionActionBean> = ExtensionPointName("cc.unitmesh.autoDevIntention")
    fun getAiAssistantIntentions(project: Project, editor: Editor, file: PsiFile): List<IntentionAction> {
        val extensionList = EP_NAME.extensionList

        val builtinIntentions = extensionList
            .asSequence()
            .map { it.instance }
            .filter { it.isAvailable(project, editor, file) }
            .toList()

        val promptConfig = CustomPromptConfig.load()
        val customActionIntentions: List<IntentionAction> = promptConfig.prompts.map {
            CustomActionIntention.create(it)
        }

        val livingDocIntentions: List<IntentionAction> = promptConfig.documentations?.map {
            CustomDocumentationIntention.create(it)
        } ?: emptyList()

        val teamPromptsIntentions: List<IntentionAction> = project.service<TeamPromptsBuilder>().default().map {
            TeamPromptIntention.create(it, true)
        }

        val actionList = builtinIntentions + customActionIntentions + livingDocIntentions + teamPromptsIntentions
        return actionList
            .map { it as AbstractChatIntention }
            .sortedByDescending(AbstractChatIntention::priority)
    }
}
