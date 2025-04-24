package cc.unitmesh.devti.language.actions.intention

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile
import javax.swing.Icon
import cc.unitmesh.devti.language.actions.intention.ui.CustomPopupStep
import cc.unitmesh.devti.language.ast.config.ShireActionLocation
import cc.unitmesh.devti.language.startup.DynamicShireActionService

class AutoDevIntentionHelper : IntentionAction, Iconable {
    override fun startInWriteAction(): Boolean = true
    override fun getText(): String = AutoDevBundle.message("devins.intention")
    override fun getFamilyName(): String = AutoDevBundle.message("devins.intention")
    override fun getIcon(flags: Int): Icon = AutoDevIcons.AI_COPILOT

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (file == null) return false

        val instance = InjectedLanguageManager.getInstance(project)
        if (instance.getTopLevelFile(file)?.virtualFile == null) return false

        return getAiAssistantIntentions(file, null).isNotEmpty()
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val intentions = getAiAssistantIntentions(file, null)
        if (intentions.isEmpty()) return

        val title = AutoDevBundle.message("intentions.assistant.popup.title")
        val popupStep = CustomPopupStep(intentions, project, editor, file, title)
        try {
            val popup = JBPopupFactory.getInstance().createListPopup(popupStep)
            popup.showInBestPositionFor(editor)
        } catch (e: Exception) {
            logger<AutoDevIntentionHelper>().warn("Failed to show popup", e)
        }
    }

    companion object {
        fun getAiAssistantIntentions(file: PsiFile, event: AnActionEvent?): List<IntentionAction> {
            val project = event?.project ?: return emptyList()
            val shireActionConfigs = DynamicShireActionService.getInstance(project).getActions(ShireActionLocation.INTENTION_MENU)

            return shireActionConfigs.map { actionConfig ->
                AutoDevIntentionAction(actionConfig.hole, file, event)
            }
        }
    }
}
