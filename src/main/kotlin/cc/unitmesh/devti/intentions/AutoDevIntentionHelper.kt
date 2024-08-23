package cc.unitmesh.devti.intentions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.intentions.ui.CustomPopupStep
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile
import javax.swing.Icon

class AutoDevIntentionHelper : IntentionAction, Iconable {
    override fun startInWriteAction(): Boolean = false
    override fun getText(): String = AutoDevBundle.message("intentions.assistant.name")
    override fun getFamilyName(): String = AutoDevBundle.message("intentions.assistant.name")
    override fun getIcon(flags: Int): Icon = AutoDevIcons.AI_COPILOT
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (file == null) return false

        val instance = InjectedLanguageManager.getInstance(project)
        return instance.getTopLevelFile(file)?.virtualFile != null
                && IntentionHelperUtil.getAiAssistantIntentions(project, editor, file).isNotEmpty()
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val intentions = IntentionHelperUtil.getAiAssistantIntentions(project, editor, file)

        val title = AutoDevBundle.message("intentions.assistant.popup.title")
        val popupStep = CustomPopupStep(intentions, project, editor, file, title)
        val popup = JBPopupFactory.getInstance().createListPopup(popupStep)

        popup.showInBestPositionFor(editor)
    }

}
