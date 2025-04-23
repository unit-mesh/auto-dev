package cc.unitmesh.devti.language.run.runner

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.team.InteractionType
import cc.unitmesh.devti.language.ast.config.DevInActionLocation
import cc.unitmesh.devti.language.provider.LocationInteractionProvider
import cc.unitmesh.devti.language.run.flow.DevInsConversationService
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ShireDefaultLlmExecutor(
    override val context: ShireLlmExecutorContext,
    private val isLocalMode: Boolean,
) : ShireLlmExecutor(context) {
    override fun execute(postFunction: PostFunction) {
        ApplicationManager.getApplication().invokeLater({
            val console = context.console
            if (isLocalMode && context.hole == null) {
                console?.print(AutoDevBundle.message("shire.run.local.mode"), ConsoleViewContentType.SYSTEM_OUTPUT)
                context.processHandler.detachProcess()
                return@invokeLater
            }

            val interaction = context.hole?.interaction
            val interactionContext = LocationInteractionContext(
                location = context.hole?.actionLocation ?: DevInActionLocation.RUN_PANEL,
                interactionType = interaction ?: InteractionType.AppendCursorStream,
                editor = context.editor,
                project = context.myProject,
                prompt = context.prompt,
                console = console,
            )

            if (interaction != null) {
                if (context.hole!!.interaction == InteractionType.OnPaste) {
                    return@invokeLater
                }
                val interactionProvider = LocationInteractionProvider.provide(interactionContext)
                if (interactionProvider != null) {
                    interactionProvider.execute(interactionContext) { response, textRange ->
                        postFunction(response, textRange)
                        try {
                            context.processHandler.detachProcess()
                        } catch (e: Exception) {
                            console?.print(e.message ?: "Error", ConsoleViewContentType.ERROR_OUTPUT)
                        }
                    }

                    return@invokeLater
                }
            }

            AutoDevCoroutineScope.scope(context.myProject).launch {
                val llmResult = StringBuilder()
                runBlocking {
                    try {
                        LlmFactory.create(context.myProject)?.stream(context.prompt, "", false)
                            ?.cancelWithConsole(console)?.collect {
                            llmResult.append(it)
                            console?.print(it, ConsoleViewContentType.NORMAL_OUTPUT)
                        } ?: console?.print(
                            "DefaultLlm" + AutoDevBundle.message("devins.llm.notfound"),
                            ConsoleViewContentType.ERROR_OUTPUT
                        )
                    } catch (e: Exception) {
                        console?.print(e.message ?: "Error", ConsoleViewContentType.ERROR_OUTPUT)
                        context.processHandler.detachProcess()
                    }
                }

                console?.print(AutoDevBundle.message("devins.llm.done"), ConsoleViewContentType.SYSTEM_OUTPUT)

                val response = llmResult.toString()
                context.myProject.getService(DevInsConversationService::class.java)
                    .refreshLlmResponseCache(context.configuration.getScriptPath(), response)

                postFunction(response, null)
                context.processHandler.detachProcess()
            }
        }, ModalityState.nonModal())
    }
}

