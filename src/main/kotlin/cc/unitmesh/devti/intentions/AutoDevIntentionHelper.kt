package cc.unitmesh.devti.intentions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.custom.CustomDocumentationIntention
import cc.unitmesh.devti.custom.CustomIntention
import cc.unitmesh.devti.intentions.ui.CustomPopupStep
import cc.unitmesh.devti.custom.CustomPromptConfig
import cc.unitmesh.devti.custom.TeamPromptIntention
import cc.unitmesh.devti.custom.team.TeamPromptsBuilder
import cc.unitmesh.devti.intentions.action.base.AbstractChatIntention
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.IntentionActionBean
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import javax.swing.Icon

class AutoDevIntentionHelper : IntentionAction, Iconable {
    override fun startInWriteAction(): Boolean = false
    override fun getText(): String = AutoDevBundle.message("intentions.assistant.name")
    override fun getFamilyName(): String = AutoDevBundle.message("intentions.assistant.name")
    override fun getIcon(flags: Int): Icon = AutoDevIcons.AI_COPILOT
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (file == null) return false

        val topLevelFile = InjectedLanguageManager.getInstance(project).getTopLevelFile((file as PsiElement?)!!)
        return topLevelFile?.virtualFile != null
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val intentions = getAiAssistantIntentions(project, editor, file)

        val title = AutoDevBundle.message("intentions.assistant.popup.title")
        val popupStep = CustomPopupStep(intentions, project, editor, file, title)
        val popup = JBPopupFactory.getInstance().createListPopup(popupStep)

        // TODO: after 2023.2 we can use this
        popup.showInBestPositionFor(editor)
    }

    companion object {
        val EP_NAME: ExtensionPointName<IntentionActionBean> =
            ExtensionPointName<IntentionActionBean>("cc.unitmesh.autoDevIntention")

        fun getAiAssistantIntentions(project: Project, editor: Editor, file: PsiFile): List<IntentionAction> {
            val extensionList = EP_NAME.extensionList

            val builtinIntentions = extensionList
                .asSequence()
                .map { (it as IntentionActionBean).instance }
                .filter { it.isAvailable(project, editor, file) }
                .toList()

            val promptConfig = CustomPromptConfig.load()
            val customIntentions: List<IntentionAction> = promptConfig.prompts.map {
                CustomIntention.create(it)
            }

            val livingDocIntentions: List<IntentionAction> = promptConfig.documentations?.map {
                CustomDocumentationIntention.create(it)
            } ?: emptyList()

            val teamPromptsIntentions: List<IntentionAction> = project.service<TeamPromptsBuilder>().build().map {
                TeamPromptIntention.create(it)
            }

            val actionList = builtinIntentions + customIntentions + livingDocIntentions + teamPromptsIntentions
            return actionList.map { it as AbstractChatIntention }.sortedByDescending { it.priority() }
        }
    }
}
