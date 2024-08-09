package cc.unitmesh.devti.actions

import cc.unitmesh.devti.settings.AutoDevSettingsConfigurable
import cc.unitmesh.devti.settings.LanguageChangedCallback.presentationText
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil

class EditSettingsAction : AnAction() {
    init{
        presentationText("settings.autodev.others.editSettings", templatePresentation)
    }
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(project, AutoDevSettingsConfigurable::class.java)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
