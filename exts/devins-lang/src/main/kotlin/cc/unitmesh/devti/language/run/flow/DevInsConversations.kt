package cc.unitmesh.devti.language.run.flow

import cc.unitmesh.devti.language.compiler.DevInsCompiledResult
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class DevInsConversationService(val project: Project) {
    /**
     * The cached conversations
     */
    private val cachedConversations: MutableMap<String, DevInsConversation> = mutableMapOf()

    fun createConversation(scriptPath: String, result: DevInsCompiledResult): DevInsConversation {
        val conversation = DevInsConversation(scriptPath, result, "", "")
        cachedConversations[scriptPath] = conversation
        return conversation
    }

    fun getConversation(scriptPath: String): DevInsConversation? {
        return cachedConversations[scriptPath]
    }

    fun updateLlmResponse(scriptPath: String, llmResponse: String) {
        cachedConversations[scriptPath]?.let {
            cachedConversations[scriptPath] = it.copy(llmResponse = llmResponse)
        }
    }

    fun updateIdeOutput(path: String, ideOutput: String) {
        cachedConversations[path]?.let {
            cachedConversations[path] = it.copy(ideOutput = ideOutput)
        }
    }

    fun tryReRun(scriptPath: String) {
        if (cachedConversations.isEmpty()) {
            return
        }

        val conversation = cachedConversations[scriptPath] ?: return
        if (conversation.hadReRun) {
            return
        }

        conversation.hadReRun = true

        // call llm again to re-run
    }
}


data class DevInsConversation(
    val scriptPath: String,
    val result: DevInsCompiledResult,
    val llmResponse: String,
    val ideOutput: String,
    val messages: MutableList<cc.unitmesh.devti.llms.custom.Message> = mutableListOf(),
    var hadReRun: Boolean = false
) {
    // update messages when has Error or Warning
}