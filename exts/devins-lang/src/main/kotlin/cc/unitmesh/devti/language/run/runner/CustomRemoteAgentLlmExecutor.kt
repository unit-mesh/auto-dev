package cc.unitmesh.devti.language.run.runner

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.agent.custom.CustomAgentExecutor
import cc.unitmesh.devti.agent.custom.model.CustomAgentConfig
import cc.unitmesh.devti.language.run.flow.DevInsConversationService
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CustomRemoteAgentLlmExecutor(
    override val context: ShireLlmExecutorContext,
    private val agent: CustomAgentConfig,
) : ShireLlmExecutor(context) {
    override fun execute(postFunction: PostFunction) {
        ApplicationManager.getApplication().invokeLater {
            val stringFlow: Flow<String>? = CustomAgentExecutor(project = context.myProject)
                .execute(context.prompt, agent, StringBuilder())

            val console = context.console
            if (stringFlow == null) {
                console?.print(
                    "CustomRemoteAgent:" + AutoDevBundle.message("devins.llm.notfound"),
                    ConsoleViewContentType.ERROR_OUTPUT
                )
                context.processHandler.detachProcess()
                postFunction(null, null)
                return@invokeLater
            }

            AutoDevCoroutineScope.scope(context.myProject).launch {
                val llmResult = StringBuilder()
                runBlocking {
                    stringFlow.cancelWithConsole(console).collect {
                        llmResult.append(it)
                        console?.print(it, ConsoleViewContentType.NORMAL_OUTPUT)
                    }
                }

                console?.print("\nDone!", ConsoleViewContentType.SYSTEM_OUTPUT)
                val llmResponse = llmResult.toString()
                context.myProject.getService(DevInsConversationService::class.java)
                    .refreshLlmResponseCache(context.configuration.getScriptPath(), llmResponse)

                postFunction(llmResponse, null)
                context.processHandler.detachProcess()
            }
        }
    }
}