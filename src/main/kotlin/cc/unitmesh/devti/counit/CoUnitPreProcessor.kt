package cc.unitmesh.devti.counit

import cc.unitmesh.devti.LLMCoroutineScope
import cc.unitmesh.devti.counit.dto.ExplainQuery
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.llms.LlmProviderFactory
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.settings.configurable.coUnitSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Service(Service.Level.PROJECT)
class CoUnitPreProcessor(val project: Project) {
    private val llmProviderFactory = LlmProviderFactory()

    fun isCoUnit(input: String): Boolean {
        return project.coUnitSettings.enableCoUnit && input.startsWith("/counit")
    }

    private val coUnitPromptGenerator = CoUnitPromptGenerator(project)

    private val json = Json { ignoreUnknownKeys = true }

    fun handleChat(prompter: ContextPrompter, ui: ChatCodingPanel, context: ChatContext?) {
        val originRequest = prompter.requestPrompt()
        ui.addMessage(originRequest, true, originRequest)

        // originRequest is starsWith /counit, should remove it
        val request = originRequest.removePrefix("/counit").trim()

        val response = coUnitPromptGenerator.findIntention(request)
            ?: throw Exception("CoUnit response is null, please check your CoUnit server address")

        ui.addMessage(response, true, response)
        // add for laoding
        ui.addMessage("start to identify intention", false, "start to identify intention")

        LLMCoroutineScope.scope(project).launch {
            val intentionFlow = llmProviderFactory.connector(project).stream(response, "")
            val result = ui.updateMessage(intentionFlow)

            try {
                val explain: ExplainQuery = json.decodeFromString(result)
                ui.addMessage("search by query and hyde doc", true, "search by query and hyde doc")
                val queryResult = coUnitPromptGenerator.queryTool(explain.query, explain.hypotheticalDocument)

                val sb = StringBuilder()
                if (queryResult.first != null) {
                    sb.append("query result: \n```json\n")
                    sb.append(Json.encodeToString(queryResult.first))
                    sb.append("\n\n")
                }

                if (queryResult.second != null) {
                    sb.append("hyde doc result: \n```json\n")
                    sb.append(Json.encodeToString(queryResult.second))
                    sb.append("\n\n")
                }

                val related = sb.toString()
                ui.addMessage(related, true, related)
            } catch (e: Exception) {
                throw Exception("parse result error: $e")
            }
        }
    }
}

