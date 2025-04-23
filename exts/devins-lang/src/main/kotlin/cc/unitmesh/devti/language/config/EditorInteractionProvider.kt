package cc.unitmesh.devti.language.config

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.custom.tasks.FileGenerateTask
import cc.unitmesh.devti.custom.team.InteractionType
import cc.unitmesh.devti.diff.DiffStreamHandler
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory.AutoDevToolUtil
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory.Companion.createNormalChatWindow
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.gui.chat.NormalChatCodingPanel
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.message.ChatContext
import cc.unitmesh.devti.gui.chat.view.MessageView
import cc.unitmesh.devti.intentions.action.task.CodeCompletionRequest
import cc.unitmesh.devti.language.console.addCancelCallback
import cc.unitmesh.devti.language.provider.LocationInteractionProvider
import cc.unitmesh.devti.language.run.runner.LocationInteractionContext
import cc.unitmesh.devti.language.run.runner.PostFunction
import cc.unitmesh.devti.language.run.runner.cancelWithConsole
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.llms.cancelHandler
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.impl.ContentManagerImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import kotlin.invoke

class EditorInteractionProvider : LocationInteractionProvider {
    override fun isApplicable(context: LocationInteractionContext): Boolean {
        return true
    }

    override fun execute(context: LocationInteractionContext, postExecute: PostFunction) {
        val targetFile = context.editor?.virtualFile

        when (context.interactionType) {
            InteractionType.AppendCursor,
            InteractionType.AppendCursorStream,
                -> {
                val task = createTask(
                    context,
                    context.prompt,
                    isReplacement = false,
                    postExecute = postExecute,
                    false
                )?.cancelWithConsole(context.console)

                if (task == null) {
                    AutoDevNotifications.error(context.project, "Failed to create code completion task.")
                    postExecute.invoke("", null)
                    return
                }

                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
            }

            InteractionType.OutputFile -> {
                val fileName = targetFile?.name
                val task = ShireFileGenerateTask(
                    context.project,
                    context.prompt,
                    fileName,
                    postExecute = postExecute
                ).cancelWithConsole(context.console)
                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
            }

            InteractionType.ReplaceSelection -> {
                val task =
                    createTask(context, context.prompt, true, postExecute, false)?.cancelWithConsole(context.console)

                if (task == null) {
                    AutoDevNotifications.error(context.project, "Failed to create code completion task.")
                    postExecute.invoke("", null)
                    return
                }

                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
            }

            InteractionType.ReplaceCurrentFile -> {
                val fileName = targetFile?.name
                val task = ShireFileGenerateTask(
                    context.project,
                    context.prompt,
                    fileName,
                    postExecute = postExecute
                ).cancelWithConsole(context.console)

                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
            }

            InteractionType.InsertBeforeSelection -> {
                val task =
                    createTask(context, context.prompt, false, postExecute, isInsertBefore = true)?.cancelWithConsole(
                        context.console
                    )

                if (task == null) {
                    AutoDevNotifications.error(context.project, "Failed to create code completion task.")
                    postExecute.invoke("", null)
                    return
                }

                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
            }

            InteractionType.RunPanel -> {
                val flow: Flow<String>? = LlmFactory.create(context.project)?.stream(context.prompt, "", false)
                AutoDevCoroutineScope.scope(context.project).launch {
                    val suggestion = StringBuilder()

                    flow?.cancelWithConsole(context.console)?.cancellable()?.collect { char ->
                        suggestion.append(char)

                        invokeLater {
                            context.console?.print(char, ConsoleViewContentType.NORMAL_OUTPUT)
                        }
                    }

                    postExecute.invoke(suggestion.toString(), null)
                }
            }

            InteractionType.ChatPanel,
            InteractionType.RightPanel -> {
                val actionType = ChatActionType.CODE_COMPLETE
                val chatCodingService = ChatCodingService(actionType, context.project)
                val toolWindowManager =
                    ToolWindowManager.getInstance(context.project).getToolWindow(AutoDevToolUtil.ID)!!

                val contentPanel = AutoDevToolWindowFactory.labelNormalChat(toolWindowManager, chatCodingService)

                toolWindowManager.activate {
                    val flow: Flow<String> = LlmFactory.create(context.project).stream(context.prompt, "", false)
                    AutoDevCoroutineScope.scope(context.project).launch {
                        val suggestion = StringBuilder()
                        contentPanel.addMessage(context.prompt, isMe = true)

                        contentPanel.updateMessage(flow)
                        postExecute.invoke(suggestion.toString(), null)
                    }
                }
            }

            InteractionType.OnPaste -> {
                /**
                 *  already handle in [com.phodal.shirelang.actions.copyPaste.ShireCopyPastePreProcessor]
                 */
            }

            InteractionType.StreamDiff -> {
                if (context.editor == null) {
                    AutoDevNotifications.error(context.project, "Editor is null, please open a file to continue.")
                    return
                }

                val code = context.editor.document.text
                val diffStreamHandler = DiffStreamHandler(
                    context.project,
                    editor = context.editor,
                    0,
                    code.lines().size,
                    onClose = {
                    },
                    onFinish = {
                        postExecute.invoke(it, null)
                        AutoDevNotifications.warn(context.project, "Patch Applied")
                    })

                diffStreamHandler.streamDiffLinesToEditor(code, context.prompt)
            }
        }
    }

    private fun createTask(
        context: LocationInteractionContext,
        userPrompt: String,
        isReplacement: Boolean,
        postExecute: PostFunction,
        isInsertBefore: Boolean,
    ): ChatCompletionTask? {
        if (context.editor == null) {
            AutoDevNotifications.error(context.project, "Editor is null, please open a file to continue.")
            return null
        }

        val editor = context.editor

        val offset = if (isInsertBefore) {
            editor.selectionModel.selectionStart
        } else {
            editor.caretModel.offset
        }

        val request = runReadAction {
            ShireCodeCompletionRequest.create(
                editor,
                offset,
                userPrompt = userPrompt,
                postExecute = postExecute,
                isReplacement = isReplacement,
            )
        } ?: return null

        val task = ChatCompletionTask(request)
        return task
    }
}
