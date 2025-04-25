package cc.unitmesh.devti.language.actions.intention.ui

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.ListSeparator
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiFile

class CustomPopupStep(
    private val intentionAction: List<IntentionAction>,
    private val project: Project,
    private val editor: Editor,
    private val psiFile: PsiFile,
    private val popupTitle: String,
) : BaseListPopupStep<IntentionAction>(popupTitle, intentionAction) {
    override fun getTextFor(value: IntentionAction): String = value.text

    override fun getSeparatorAbove(value: IntentionAction?): ListSeparator? =
        if (value != null && value == intentionAction) ListSeparator() else null

    override fun onChosen(selectedValue: IntentionAction?, finalChoice: Boolean): PopupStep<*>? {
        return doFinalStep {
            selectedValue?.invoke(project, editor, psiFile)
        }
    }
}