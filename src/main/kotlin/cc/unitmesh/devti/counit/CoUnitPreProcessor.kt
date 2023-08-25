package cc.unitmesh.devti.counit

import cc.unitmesh.devti.LLMCoroutineScope
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.llms.LlmProviderFactory
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.settings.configurable.coUnitSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class CoUnitPreProcessor(val project: Project) {
    private val llmProviderFactory = LlmProviderFactory()

    fun isCoUnit(input: String): Boolean {
        return project.coUnitSettings.enableCoUnit && input.startsWith("/counit")
    }

    fun handleChat(prompter: ContextPrompter, ui: ChatCodingPanel, context: ChatContext?) {
        val originRequest = prompter.requestPrompt()
        ui.addMessage(originRequest, true, originRequest)

        // originRequest is starsWith /counit, should remove it
        val request = originRequest.removePrefix("/counit").trim()

        val response = CoUnitPromptGenerator(project).findIntention(request)
            ?: throw Exception("CoUnit response is null, please check your CoUnit server address")

        ui.addMessage(response, true, response)

        ApplicationManager.getApplication().executeOnPooledThread {
            val response = llmProviderFactory.connector(project).stream(response, "")
            LLMCoroutineScope.scope(project).launch {
                ui.updateMessage(response)
            }
        }
    }
}

