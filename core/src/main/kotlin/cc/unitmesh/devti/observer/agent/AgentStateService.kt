package cc.unitmesh.devti.observer.agent

import cc.unitmesh.devti.agent.tool.AgentTool
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.llms.custom.Message
import cc.unitmesh.devti.llms.tokenizer.TokenizerFactory
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
import cc.unitmesh.devti.observer.plan.PlanUpdateListener
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.util.parser.MarkdownCodeHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.patch.FilePatch
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.changes.shelf.ShelvedChange
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class AgentStateService(val project: Project) {
    val maxToken = AutoDevSettingsState.maxTokenLength
    var state: AgentState = AgentState()
    var tokenizer = lazy {
        TokenizerFactory.createTokenizer()
    }

    fun addTools(tools: List<BuiltinCommand>) {
        state.usedTools = tools.map {
            AgentTool(it.commandName, it.description, "")
        }

        logger<AgentStateService>().info("Called agent tools:\n ${state.usedTools.joinToString("\n")}")
    }

    fun addToChange(path: Path, change: FilePatch) {
        val shelvedChange = createShelvedChange(project, path, change)
        if (shelvedChange != null) {
            state.changes.add(shelvedChange.change)

            ApplicationManager.getApplication().messageBus
                .syncPublisher(PlanUpdateListener.TOPIC)
                .onUpdateChange(state.changes)
        }
    }

    fun createShelvedChange(project: Project, patchPath: Path, patch: FilePatch): ShelvedChange? {
        val beforeName: String? = patch.beforeName
        val afterName: String? = patch.afterName
        if (beforeName == null || afterName == null) {
            logger<AgentStateService>().warn("Failed to parse the file patch: [$patchPath]:$patch")
            return null
        }

        val status = if (patch.isNewFile) {
            FileStatus.ADDED
        } else if (patch.isDeletedFile) {
            FileStatus.DELETED
        } else {
            FileStatus.MODIFIED
        }

        return ShelvedChange.create(project, patchPath, beforeName, afterName, status)
    }

    fun buildOriginIntention(): String? {
        val intention = state.messages
            .firstOrNull { it.role.lowercase() == "user" }
            ?.content

        if (intention != null) {
            state.originIntention = intention
        }

        return intention
    }

    fun getAllMessages(): List<Message> {
        return state.messages
    }

    /**
     * Call some LLM to compress it or use some other method to compress the history
     */
    fun processMessages(messages: List<Message>): List<Message> {
        val countLength = tokenizer.value.count(messages.joinToString("\n") { it.content })
        if (countLength < maxToken) {
            state.messages = messages
            return messages
        }

        state.messages = messages.map {
            it.copy(content = MarkdownCodeHelper.removeAllMarkdownCode(it.content))
        }

        return state.messages
    }

    fun updatePlan(items: MutableList<AgentTaskEntry>) {
        this.state.plan = items
        ApplicationManager.getApplication().messageBus
            .syncPublisher(PlanUpdateListener.TOPIC)
            .onPlanUpdate(items)
    }

    fun updatePlan(content: String) {
        val planItems = MarkdownPlanParser.parse(content)
        updatePlan(planItems.toMutableList())
    }

    fun resetState() {
        state = AgentState()
        ApplicationManager.getApplication().messageBus
            .syncPublisher(PlanUpdateListener.TOPIC)
            .onPlanUpdate(mutableListOf())
    }

    fun getPlan(): MutableList<AgentTaskEntry> {
        return state.plan
    }
}