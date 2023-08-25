package cc.unitmesh.devti.counit

import cc.unitmesh.devti.LLMCoroutineScope
import cc.unitmesh.devti.counit.dto.ExplainQuery
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.llms.LlmProviderFactory
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.settings.configurable.coUnitSettings
import com.intellij.openapi.application.ApplicationManager
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

    val llmProvider = llmProviderFactory.connector(project)


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
            llmProvider.appendLocalMessage(response, ChatRole.User)

            val intentionFlow = llmProvider.stream(response, "")
            val result = ui.updateMessage(intentionFlow)

            llmProvider.appendLocalMessage(result, ChatRole.Assistant)

            val explain = try {
                val fixedResult = fix(result)
                val explain: ExplainQuery = json.decodeFromString(fixedResult)
                explain
            } catch (e: Exception) {
                throw Exception("parse result error: $e")
            }

            val searchTip = "search by query and hyde doc"

            llmProvider.appendLocalMessage(searchTip, ChatRole.User)
            ui.addMessage(searchTip, true, searchTip)

            val queryResult = coUnitPromptGenerator.queryTool(explain.query, explain.hypotheticalDocument)

            val sb = StringBuilder()
            if (queryResult.first != null) {
                sb.append("query result: \n```json\n")
                sb.append(Json.encodeToString(queryResult.first))
                sb.append("\n```\n")
            }

            if (queryResult.second != null) {
                sb.append("hyde doc result: \n```json\n")
                sb.append(Json.encodeToString(queryResult.second))
                sb.append("\n```\n")
            }

            val related = sb.toString()
            llmProvider.appendLocalMessage(related, ChatRole.User)

            ApplicationManager.getApplication().invokeLater {
                ui.addMessage(related, true, related)
            }
        }
    }

    private fun fix(result: String): String {
        // remove start and end ```json
        return result
            .removePrefix("```json")
            .removeSuffix("```")
    }
}

