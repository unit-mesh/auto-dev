package cc.unitmesh.devti.counit

import cc.unitmesh.devti.LLMCoroutineScope
import cc.unitmesh.devti.counit.dto.ExplainQuery
import cc.unitmesh.devti.counit.dto.QueryResponse
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

        val request = originRequest.removePrefix("/counit").trim()

        val response = coUnitPromptGenerator.findIntention(request)
            ?: throw Exception("CoUnit response is null, please check your CoUnit server address")

        ui.addMessage(response, true, response)

        // loading
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

            val searchTip = "search API by query and hypothetical document"
            llmProvider.appendLocalMessage(searchTip, ChatRole.User)
            ui.addMessage(searchTip, true, searchTip)

            val queryResult = coUnitPromptGenerator.queryTool(explain.query, explain.hypotheticalDocument)

            val related = buildDocAsContext(queryResult)
            llmProvider.appendLocalMessage(related, ChatRole.User)

            ApplicationManager.getApplication().invokeLater {
                ui.addMessage(related, true, related)
            }
        }
    }

    private fun buildDocAsContext(queryResult: Pair<QueryResponse?, QueryResponse?>): String {
        val sb = StringBuilder()
        val normalDoc = queryResult.first
        if (normalDoc != null && normalDoc.data.isNotEmpty()) {
            sb.append("here is related API to origin query's result: \n```markdown\n")
            sb.append(Json.encodeToString(normalDoc.data[0].displayText))
            sb.append("\n```\n")
        }

        val hypoDoc = queryResult.second
        if (hypoDoc != null && hypoDoc.data.isNotEmpty()) {
            sb.append("here is hypothetical document which related to origin query's result: \n```markdown\n")
            sb.append(Json.encodeToString(hypoDoc.data[0].displayText))
            sb.append("\n```\n")
        }

        val related = sb.toString()
        return related
    }

    private fun fix(result: String): String {
        // remove start and end ```json
        return result
            .removePrefix("```json")
            .removeSuffix("```")
    }
}

