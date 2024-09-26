package cc.unitmesh.devti.actions.chat.base

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiWhiteSpace
import com.intellij.temporary.getElementToAction

/**
 * This abstract class can check for the psi-element
 * when updating the presentation of the action in the user interface.
 *
 * For example, when the psi-element of an action is [PsiWhiteSpace],
 * an unexpected prompt is generated and should not be processed.
 * To avoid such prompts, the action should be disabled.
 *
 * @author lk
 */
abstract class ChatCheckForUpdateAction : ChatBaseAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val project = e.getData(CommonDataKeys.PROJECT)
        e.presentation.isEnabled = getElementToAction(project, editor)?.text?.isNotBlank() ?: false
    }
}