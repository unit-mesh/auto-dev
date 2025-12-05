package cc.unitmesh.devti.language.actions.external

import cc.unitmesh.devti.language.actions.DevInsRunFileAction
import cc.unitmesh.devti.devins.ShireActionLocation
import cc.unitmesh.devti.devins.VariableActionEventDataHolder
import cc.unitmesh.devti.language.startup.DynamicShireActionService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class AutoDevSonarLintAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    private fun shireActionConfigs(project: Project) =
        DynamicShireActionService.getInstance(project).getActions(ShireActionLocation.EXT_SONARQUBE_MENU)

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val isOnlyOneConfig = shireActionConfigs(project).size == 1

        val hobbitHole = shireActionConfigs(project).firstOrNull()?.hole
        e.presentation.isVisible = isOnlyOneConfig
        e.presentation.isEnabled = hobbitHole != null && hobbitHole.enabled
        if (hobbitHole != null) {
            e.presentation.text = hobbitHole.name ?: "<Placeholder>"
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        VariableActionEventDataHolder.putData(VariableActionEventDataHolder(e.dataContext))

        val config = shireActionConfigs(project).firstOrNull() ?: return
        DevInsRunFileAction.executeFile(project, config, null)
    }
}
