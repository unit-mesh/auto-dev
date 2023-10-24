package cc.unitmesh.devti.custom.tasks

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.llms.LlmFactory
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

class FileGenerateTask(@JvmField val project: Project, val messages: List<LlmMsg.ChatMessage>, val fileName: String) :
    Task.Backgroundable(project, AutoDevBundle.message("intentions.request.background.process.title")) {
    private val projectRoot = project.guessProjectDir()!!

    override fun run(indicator: ProgressIndicator) {
        val requestPrompt = messages.filter { it.role == LlmMsg.ChatRole.User }.joinToString("\n") { it.content }
        val systemPrompt = messages.filter { it.role == LlmMsg.ChatRole.System }.joinToString("\n") { it.content }

        val stream =
            LlmFactory().create(project).stream(requestPrompt, systemPrompt)

        var result = ""
        runBlocking {
            stream.cancellable().collect {
                result += it
            }
        }

        WriteCommandAction.runWriteCommandAction(project, "GenerateDockerfile", "cc.unitmesh.genius", {
            projectRoot.createChildData(this, fileName).setBinaryContent(result.toByteArray())
        });
    }
}