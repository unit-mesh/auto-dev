package cc.unitmesh.devti.actions.groups

import cc.unitmesh.devti.settings.LanguageChangedCallback.presentationText
import com.intellij.openapi.actionSystem.ActionGroupUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware

/**
 * Add the right-click actions to the dynamic group.
 *
 * This group hides itself on the editorPopupMenu
 * when each child are disabled or invisible.
 *
 * @author lk
 */
class AutoChatDynamicActionGroup : DefaultActionGroup(), DumbAware {

    init {
        presentationText("autodev.chat", templatePresentation.also { it.isHideGroupIfEmpty = true }, 1)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = !ActionGroupUtil.isGroupEmpty(this, e)
    }
}

