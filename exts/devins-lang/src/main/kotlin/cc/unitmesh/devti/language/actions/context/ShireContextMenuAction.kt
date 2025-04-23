package cc.unitmesh.devti.language.actions.context

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.language.actions.DevInsRunFileAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import cc.unitmesh.devti.language.actions.base.validator.WhenConditionValidator
import cc.unitmesh.devti.language.startup.DynamicDevInsActionConfig

class ShireContextMenuAction(private val config: DynamicDevInsActionConfig) :
    DumbAwareAction(config.name, config.hole?.description, AutoDevIcons.AI_COPILOT) {

    init {
        templatePresentation.text = config.name.ifBlank { "Unknown" }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT


    override fun update(e: AnActionEvent) {
        //2024-07-13 10:32:57,277 [51307999] SEVERE - #c.i.o.a.i.Utils - Empty menu item text for ShireContextMenuAction@EditorPopup (com.phodal.shirelang.actions.context.ShireContextMenuAction). The default action text must be specified in plugin.xml or its class constructor [Plugin: com.phodal.shire]
        // com.intellij.diagnostic.PluginException: Empty menu item text for ShireContextMenuAction@EditorPopup (com.phodal.shirelang.actions.context.ShireContextMenuAction). The default action text must be specified in plugin.xml or its class constructor [Plugin: com.phodal.shire]
        try {
            val conditions = config.hole?.when_ ?: return
            val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

            WhenConditionValidator.isAvailable(conditions, psiFile).let {
                e.presentation.isEnabled = it
                e.presentation.isVisible = it

                e.presentation.text = config.hole.name
            }
        } catch (e: Exception) {
            logger<ShireContextMenuAction>().error("Error in ShireContextMenuAction", e)
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        DevInsRunFileAction.executeFile(
            project,
            config,
            DevInsRunFileAction.createRunConfig(e)
        )
    }
}