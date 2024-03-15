package cc.unitmesh.devti.language.actions

import cc.unitmesh.devti.AutoDevNotifications
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import org.jetbrains.annotations.NonNls

class DevInRunFileAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        if (e.presentation.text.isNullOrBlank()) {
            e.presentation.text = "Run DevIn file: ${file.name}"
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val file =
            e.getData(CommonDataKeys.PSI_FILE) ?: return
        val virtualFile = file.virtualFile ?: return


        val project = file.project
        val context = ConfigurationContext.getFromContext(e.dataContext, e.place)

        AutoDevNotifications.notify(project, "Run file action")
    }

    companion object {
        val ID: @NonNls String = "runDevInFileAction"
    }

}
