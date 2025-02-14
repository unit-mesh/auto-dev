package cc.unitmesh.devti.custom.team

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.tasks.FileGenerateTask
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.sendToChatWindow
import cc.unitmesh.devti.intentions.action.task.BaseCompletionTask
import cc.unitmesh.devti.intentions.action.task.CodeCompletionRequest
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

/**
 * The `TeamPromptExecTask` class is a background task that executes a team prompt action in the context of a project.
 * It is responsible for handling different types of interactions based on the provided `intentionConfig` and `msgs`.
 *
 * @property project The project in which the team prompt action is executed.
 * @property msgs The list of chat messages associated with the team prompt action.
 * @property editor The editor in which the team prompt action is triggered.
 * @property intentionConfig The configuration of the team prompt action.
 * @property element The PSI element associated with the team prompt action.
 * @constructor Creates a `TeamPromptExecTask` with the specified project, chat messages, editor, intention configuration, and PSI element.
 */
class TeamPromptExecTask(
    @JvmField val project: Project,
    private val msgs: List<LlmMsg.ChatMessage>,
    val editor: Editor,
    private val intentionConfig: TeamPromptAction,
    val element: PsiElement?,
    private val targetFile: VirtualFile?,
    val taskName: String = AutoDevBundle.message("intentions.request.background.process.title")
) :
    Task.Backgroundable(project, taskName) {
    override fun run(indicator: ProgressIndicator) {
        val offset = runReadAction { editor.caretModel.offset }

        val userPrompt = msgs.filter { it.role == LlmMsg.ChatRole.User }.joinToString("\n") { it.content }
        val systemPrompt = msgs.filter { it.role == LlmMsg.ChatRole.System }.joinToString("\n") { it.content }

        when (intentionConfig.actionPrompt.interaction) {
            InteractionType.ChatPanel -> {
                sendToChatWindow(project, ChatActionType.CHAT) { panel, service ->
                    service.handleMsgsAndResponse(panel, msgs)
                }
            }

            InteractionType.AppendCursor,
            InteractionType.AppendCursorStream,
            -> {
                val msgString = systemPrompt + "\n" + userPrompt
                val request = runReadAction {
                    CodeCompletionRequest.create(editor, offset, element, null, msgString)
                } ?: return

                val task = object : BaseCompletionTask(request) {
                    override fun keepHistory(): Boolean = false
                    override fun promptText(): String = msgString
                }

                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
            }

            InteractionType.OutputFile -> {
                val fileName = intentionConfig.actionPrompt.other["fileName"] as String?

                val task = FileGenerateTask(project, msgs, fileName, taskName = taskName)
                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
            }

            InteractionType.ReplaceSelection -> {
                val msgString = systemPrompt + "\n" + userPrompt
                val request = runReadAction {
                    CodeCompletionRequest.create(editor, offset, element, null, msgString, isReplacement = true)
                } ?: return

                val task = object : BaseCompletionTask(request) {
                    override fun keepHistory(): Boolean = false
                    override fun promptText(): String = msgString
                }

                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
            }

            InteractionType.ReplaceCurrentFile -> {
                val fileName = targetFile?.path
                val task = FileGenerateTask(project, msgs, fileName, codeOnly = intentionConfig.actionPrompt.codeOnly, taskName = taskName)

                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
            }
        }
    }
}