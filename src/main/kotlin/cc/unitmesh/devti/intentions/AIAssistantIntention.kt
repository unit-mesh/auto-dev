package cc.unitmesh.devti.intentions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.IntentionActionBean
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListSeparator
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.Nls
import javax.swing.Icon
import kotlin.jvm.internal.Intrinsics

class AIAssistantIntention : IntentionAction, Iconable {
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
        val intentions = Companion.getAiAssistantIntentions(project, editor, file)

        val title = AutoDevBundle.message("intentions.assistant.popup.title")
        val popupStep = CustomPopupStep(intentions, project, editor, file, title)
        val popup = JBPopupFactory.getInstance().createListPopup(popupStep)

        // TODO: after 2023.2 we can use this
        popup.showInBestPositionFor(editor)
    }

    companion object {
        val EP_NAME: ExtensionPointName<IntentionActionBean> =
            ExtensionPointName<IntentionActionBean>("cc.unitmesh.aiAssistantIntention")

        fun getAiAssistantIntentions(project: Project, editor: Editor, file: PsiFile): List<IntentionAction> {
            val intentions: MutableList<IntentionAction> = ArrayList()
            val extensionList = EP_NAME.extensionList

            for (bean in extensionList) {
                val intentionActionBean = bean as IntentionActionBean
                val intention = intentionActionBean.instance
                if (intention.isAvailable(project, editor, file)) {
                    intentions.add(intention)
                }
            }

            return intentions
        }
    }
}

class CustomPopupStep(
    val intentionAction: List<IntentionAction>,
    val project: Project,
    val editor: Editor,
    val psiFile: PsiFile,
    val popupTitle: @Nls String
) : BaseListPopupStep<IntentionAction>(popupTitle, intentionAction) {
    override fun getTextFor(value: IntentionAction): String = value.text

    override fun getSeparatorAbove(value: IntentionAction?): ListSeparator? =
        if (value != null && Intrinsics.areEqual(value, intentionAction)) ListSeparator() else null

    override fun onChosen(selectedValue: IntentionAction?, finalChoice: Boolean): PopupStep<*>? {
        val runnable = Runnable { selectedValue!!.invoke(project, editor, psiFile) }
        return doFinalStep(runnable)
    }
}
