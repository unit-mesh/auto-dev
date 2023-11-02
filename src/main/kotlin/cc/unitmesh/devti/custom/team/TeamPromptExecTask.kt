package cc.unitmesh.devti.custom.team

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.tasks.FileGenerateTask
import cc.unitmesh.devti.gui.sendToChatPanel
import cc.unitmesh.devti.intentions.action.task.BaseCompletionTask
import cc.unitmesh.devti.intentions.action.task.CodeCompletionRequest
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiElement

class TeamPromptExecTask(
    @JvmField val project: Project,
    val msgs: List<LlmMsg.ChatMessage>,
    val editor: Editor,
    val intentionConfig: TeamPromptAction,
    val element: PsiElement?,
) :
    Task.Backgroundable(project, AutoDevBundle.message("intentions.request.background.process.title")) {
    override fun run(indicator: ProgressIndicator) {
        val offset = runReadAction { editor.caretModel.offset }

        val userPrompt = msgs.filter { it.role == LlmMsg.ChatRole.User }.joinToString("\n") { it.content }
        val systemPrompt = msgs.filter { it.role == LlmMsg.ChatRole.System }.joinToString("\n") { it.content }

        when (intentionConfig.actionPrompt.interaction) {
            InteractionType.ChatPanel -> {
                sendToChatPanel(project) { panel, service ->
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
                val fileName = intentionConfig.actionPrompt.other.getOrDefault("fileName", "output.txt") as String
                val filename = project.guessProjectDir()!!.toNioPath().resolve(fileName).toFile()
                if (!filename.exists()) {
                    filename.createNewFile()
                }

                val task = FileGenerateTask(project, msgs, filename)
                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
            }
        }
    }

}