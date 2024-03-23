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

    /**
     * Updates the LLM response for a given script path in the cached conversations.
     * If the script path exists in the cached conversations, the LLM response is updated with the provided value.
     *
     * @param scriptPath The script path for which the LLM response needs to be updated.
     * @param llmResponse The new LLM response to be updated for the given script path.
     */
    fun updateLlmResponse(scriptPath: String, llmResponse: String) {
        cachedConversations[scriptPath]?.let {
            cachedConversations[scriptPath] = it.copy(llmResponse = llmResponse)
        }
    }

    /**
     * Updates the IDE output for a conversation at the specified path.
     *
     * @param path The path of the conversation to update.
     * @param ideOutput The new IDE output to set for the conversation.
     */
    fun updateIdeOutput(path: String, ideOutput: String) {
        cachedConversations[path]?.let {
            cachedConversations[path] = it.copy(ideOutput = ideOutput)
        }
    }

    /**
     * Function to try re-running a conversation script.
     *
     * @param scriptPath The path of the script to re-run
     */
    fun tryFixWithLlm(scriptPath: String) {
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