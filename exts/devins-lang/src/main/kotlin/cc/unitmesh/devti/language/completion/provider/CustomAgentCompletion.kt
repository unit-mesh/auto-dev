package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.agent.custom.model.CustomAgentConfig
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory.AutoDevToolUtil
import cc.unitmesh.devti.gui.chat.NormalChatCodingPanel
import cc.unitmesh.devti.provider.DevInsAgentToolCollector
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.ProcessingContext

class CustomAgentCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val configs: List<CustomAgentConfig> = CustomAgentConfig.loadFromProject(parameters.originalFile.project)
        configs.forEach { config ->
            result.addElement(
                LookupElementBuilder.create(config.name)
                    .withIcon(AutoDevIcons.CLOUD_AGENT)
                    .withInsertHandler { context, _ ->
                        context.document.insertString(context.tailOffset, " ")
                        context.editor.caretModel.moveCaretRelatively(1, 0, false, true, false)

                        val toolWindow =
                            ToolWindowManager.getInstance(context.project).getToolWindow(AutoDevToolUtil.ID)
                        toolWindow?.contentManager?.contents?.map { it.component }?.forEach {
                            if (it is NormalChatCodingPanel) {
                                it.selectAgent(config)
                            }
                        }
                    }
                    .withTypeText(config.description, true))
        }

        val project = parameters.originalFile.project
        val devInsAgent = DevInsAgentToolCollector.all(project).map {
            CustomAgentConfig(name = it.name, description = it.description, isFromDevIns = true)
        }

        devInsAgent.forEach { config ->
            result.addElement(
                LookupElementBuilder.create(config.name)
                    .withIcon(AutoDevIcons.LOCAL_AGENT)
                    .withInsertHandler { context, _ ->
                        context.document.insertString(context.tailOffset, " ")
                        context.editor.caretModel.moveCaretRelatively(1, 0, false, true, false)

                        val toolWindow =
                            ToolWindowManager.getInstance(context.project).getToolWindow(AutoDevToolUtil.ID)
                        toolWindow?.contentManager?.contents?.map { it.component }?.forEach {
                            if (it is NormalChatCodingPanel) {
                                it.selectAgent(config)
                            }
                        }
                    }
                    .withTypeText(config.description, true))
        }
    }
}
