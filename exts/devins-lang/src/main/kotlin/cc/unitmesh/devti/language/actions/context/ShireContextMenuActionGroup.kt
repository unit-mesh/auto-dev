package cc.unitmesh.devti.language.actions.context

import com.intellij.openapi.actionSystem.*
import cc.unitmesh.devti.language.actions.base.validator.WhenConditionValidator
import cc.unitmesh.devti.language.ast.config.ShireActionLocation
import cc.unitmesh.devti.language.startup.DynamicShireActionService

class ShireContextMenuActionGroup : ActionGroup() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        e.presentation.isPopupGroup = DynamicShireActionService.getInstance(project).getAllActions().size > 1
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val project = e?.project ?: return emptyArray()
        val actionService = DynamicShireActionService.getInstance(project)

        return actionService.getActions(ShireActionLocation.CONTEXT_MENU).mapNotNull { actionConfig ->
            if (actionConfig.hole == null) return@mapNotNull null
            if (!actionConfig.hole.enabled) return@mapNotNull null

            actionConfig.hole.when_?.let {
                val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return@mapNotNull null
                if (!WhenConditionValidator.isAvailable(it, psiFile)) return@mapNotNull null
            }

            val menuAction = ShireContextMenuAction(actionConfig)
            if (actionConfig.hole.shortcut != null) {
                actionService.bindShortcutToAction(menuAction, actionConfig.hole.shortcut)
            }

            menuAction
        }.distinctBy { it.templatePresentation.text }.toTypedArray()
    }
}

