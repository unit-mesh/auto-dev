package cc.unitmesh.devti.counit

import cc.unitmesh.devti.LLMCoroutineScope
import cc.unitmesh.devti.counit.dto.ExplainQuery
import cc.unitmesh.devti.counit.dto.QueryResult
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.settings.configurable.coUnitSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

const val CO_UNIT = "/counit"

@Service(Service.Level.PROJECT)
class CoUnitPreProcessor(val project: Project) {
    private val llmFactory = LlmFactory()
    private val logger = logger<CoUnitPreProcessor>()

    private val coUnitPromptGenerator = CoUnitPromptGenerator(project)
    private val json = Json { ignoreUnknownKeys = true }
    private val llmProvider = llmFactory.create(project)

    fun isCoUnit(input: String): Boolean {
        return project.coUnitSettings.enableCoUnit && input.startsWith(CO_UNIT)
    }

    fun handleChat(prompter: ContextPrompter, ui: ChatCodingPanel, context: ChatContext?) {
        val originRequest = prompter.requestPrompt()
        ui.addMessage(originRequest, true, originRequest)

        val request = originRequest.removePrefix(CO_UNIT).trim()

        val response = coUnitPromptGenerator.findIntention(request)
        if (response == null) {
            logger.error("can not find intention for request: $request")
            return
        }

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
                logger.error("parse result error: $e")
                return@launch
            }

            val searchTip = "search API by query and hypothetical document"
            llmProvider.appendLocalMessage(searchTip, ChatRole.User)
            ui.addMessage(searchTip, true, searchTip)

            val queryResult = coUnitPromptGenerator.semanticQuery(explain)

            val related = buildDocAsContext(queryResult)
            if (related.isEmpty()) {
                val noResultTip = "no related API found"
                llmProvider.appendLocalMessage(noResultTip, ChatRole.Assistant)
                ui.addMessage(noResultTip, false, noResultTip)
                return@launch
            }

            llmProvider.appendLocalMessage(related, ChatRole.User)

            ApplicationManager.getApplication().invokeLater {
                ui.addMessage(related, true, related)
            }
        }
    }

    private fun buildDocAsContext(queryResult: QueryResult): String {
        val sb = StringBuilder()
        val normalDoc = queryResult.englishQuery
        if (normalDoc.isNotEmpty()) {
            sb.append("here is related API to origin query's result: \n```markdown\n")
            sb.append(normalDoc[0].displayText)
            sb.append("\n```\n")
        }

        val nature = queryResult.naturalLangQuery
        if (nature.isNotEmpty()) {
            sb.append("here is natural language query's result: \n```markdown\n")
            sb.append(nature[0].displayText)
            sb.append("\n```\n")
        }

        val hyde = queryResult.hypotheticalDocument
        if (hyde.isNotEmpty()) {
            sb.append("here is hypothetical document's result: \n```markdown\n")
            sb.append(hyde[0].displayText)
            sb.append("\n```\n")
        }

        return sb.toString()
    }

    private fun fix(result: String): String {
        return result
            .removePrefix("```json")
            .removeSuffix("```")
    }
}

