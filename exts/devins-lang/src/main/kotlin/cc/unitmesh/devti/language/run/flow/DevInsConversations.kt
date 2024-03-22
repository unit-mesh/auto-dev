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

    fun createConversation(input: String, scriptPath: String): DevInsConversation {
        val conversation = DevInsConversation(input, scriptPath, DevInsCompiledResult(), "", "")
        cachedConversations[scriptPath] = conversation
        return conversation
    }

    fun getConversation(scriptPath: String): DevInsConversation? {
        return cachedConversations[scriptPath]
    }

    fun updateCompiledResult(scriptPath: String, result: DevInsCompiledResult) {
        cachedConversations[scriptPath]?.let {
            cachedConversations[scriptPath] = it.copy(result = result)
        }
    }

    fun updateApiResponse(scriptPath: String, apiResponse: String) {
        cachedConversations[scriptPath]?.let {
            cachedConversations[scriptPath] = it.copy(apiResponse = apiResponse)
        }
    }

    fun updateIdeOutput(path: String, ideOutput: String) {
        cachedConversations[path]?.let {
            cachedConversations[path] = it.copy(ideOutput = ideOutput)
        }
    }
}


data class DevInsConversation(
    val input: String,
    val scriptPath: String,
    val result: DevInsCompiledResult,
    val apiResponse: String,
    val ideOutput: String
)