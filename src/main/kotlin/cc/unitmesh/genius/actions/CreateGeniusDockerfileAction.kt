package cc.unitmesh.genius.actions

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.template.DockerfileContext
import cc.unitmesh.devti.template.TemplateRender
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking


private const val DOCKERFILE = "Dockerfile"

class CreateGeniusDockerfileAction : AnAction(AutoDevBundle.message("action.new.genius.dockerfile")) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val root = project.guessProjectDir()!!
        val dockerfile = root.findFileByRelativePath(DOCKERFILE)
        if (dockerfile?.exists() == true) {
            // current Dockerfile to context
        }

        // first, we need to guess language
        val dockerContexts = BuildSystemProvider.guess(project);
        val templateRender = TemplateRender("genius/sre")
        templateRender.context = DockerContext.from(dockerContexts)

        val template = templateRender
            .getTemplate("create-dockerfile.vm")

        val msgs = templateRender.create(template)

        val task: Task.Backgroundable = DockerFileGenerateTask(project, msgs)
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }
}

data class DockerContext(
    val buildContext: String,
) {
    companion object {
        fun from(dockerContexts: List<DockerfileContext>): DockerContext {
            val string = dockerContexts.joinToString("\n") {
                val build = "- Build tool name: ${it.buildToolName}, Build tool version: ${it.buildToolVersion}\n"
                val language = "- Language name: ${it.languageName}, Language version: ${it.languageVersion}\n"
                val task = "- Build tool Task list: ${it.taskString}\n"
                build + language + task
            }
            return DockerContext(string)
        }
    }
}


class DockerFileGenerateTask(@JvmField val project: Project, val messages: List<LlmMsg.ChatMessage>) :
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
            projectRoot.createChildData(this, DOCKERFILE).setBinaryContent(result.toByteArray())
        });
    }
}
