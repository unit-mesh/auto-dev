package cc.unitmesh.devti.language.run.flow

import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.sendToChatWindow
import cc.unitmesh.devti.language.DevInBundle
import cc.unitmesh.devti.language.compiler.DevInsCompiledResult
import cc.unitmesh.devti.language.run.runner.ShireConsoleView
import cc.unitmesh.devti.language.run.runner.cancelWithConsole
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.TextContextPrompter
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking

@Service(Service.Level.PROJECT)
class DevInsConversationService(val project: Project) {
    /**
     * The cached conversations
     */
    private val cachedConversations: MutableMap<String, DevInsProcessContext> = mutableMapOf()

    fun createConversation(scriptPath: String, result: DevInsCompiledResult): DevInsProcessContext {
        val conversation = DevInsProcessContext(scriptPath, result, "", "")
        cachedConversations[scriptPath] = conversation
        return conversation
    }

    fun getConversation(scriptPath: String): DevInsProcessContext? {
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
     * Updates the IDE output for a conversation at the specified path.
     *
     * @param path The path of the conversation to update.
     * @param ideOutput The new IDE output to set for the conversation.
     */
    fun refreshIdeOutput(path: String, ideOutput: String) {
        cachedConversations[path]?.let {
            cachedConversations[path] = it.copy(ideOutput = ideOutput)
        }
    }

    /**
     * Updates the LLM response for a given script path in the cached conversations.
     * If the script path exists in the cached conversations, the LLM response is updated with the provided value.
     *
     * @param scriptPath The script path for which the LLM response needs to be updated.
     * @param llmResponse The new LLM response to be updated for the given script path.
     */
    fun refreshLlmResponseCache(scriptPath: String, llmResponse: String) {
        cachedConversations[scriptPath]?.let {
            cachedConversations[scriptPath] = it.copy(llmResponse = llmResponse)
        }
    }

    /**
     * Function to try re-running a conversation script.
     *
     * @param scriptPath The path of the script to re-run
     */
    fun retryScriptExecution(scriptPath: String, consoleView: ShireConsoleView?) {
        if (cachedConversations.isEmpty()) return
        val conversation = cachedConversations[scriptPath] ?: return
        if (conversation.hadReRun) return
        conversation.hadReRun = true

        val prompt = StringBuilder()
        val compiledResult = conversation.compiledResult
        if (compiledResult.isLocalCommand) {
            val message =
                DevInBundle.message("devin.prompt.fix.command", compiledResult.input, compiledResult.output)
            prompt.append(message)
        }

        prompt.append(DevInBundle.message("devin.prompt.fix.run-result", conversation.ideOutput))

        val finalPrompt = prompt.toString()
        if (consoleView != null) {
            runBlocking {
                try {
                    LlmFactory.create(project)
                        ?.stream(finalPrompt, "", true)
                        ?.cancelWithConsole(consoleView)
                        ?.collect {
                            consoleView.print(it, ConsoleViewContentType.NORMAL_OUTPUT)
                        }
                } catch (e: Exception) {
                    consoleView.print(e.message ?: "Error", ConsoleViewContentType.ERROR_OUTPUT)
                }
            }
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

        val prompt = StringBuilder()

        // todo: refactor to DevIn template file
        if (conversation.compiledResult.isLocalCommand) {
            prompt.append(
                """
                You are a top software developer in the world, which can help me to fix the issue.
                When I use shell-like language and compile the script, I got an error, can you help me to fix it?
                
                Origin script:
                ```devin
                ${conversation.compiledResult.input}
                ```
                
                Script with result:
                ####
                ${conversation.compiledResult.output}
                ####
                """.trimIndent()
            )
        }

        prompt.append(
            """
            Here is the run result, can you help me to fix it?
            Run result:
            ####
            ${conversation.ideOutput}
            ####
            """.trimIndent()
        )

        val finalPrompt = prompt.toString()
        sendToChatWindow(project, ChatActionType.CHAT) { panel, service ->
            service.handlePromptAndResponse(panel, TextContextPrompter(finalPrompt), null, true)
        }
    }

    fun getLlmResponse(scriptPath: String): String {
        return cachedConversations[scriptPath]?.llmResponse ?: ""
    }
}
