package cc.unitmesh.genius.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.tasks.FileGenerateTask
import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.template.DockerfileContext
import cc.unitmesh.devti.template.TemplateRender
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator


private const val DOCKERFILE = "Dockerfile"

class CreateGeniusDockerfileAction : AnAction(AutoDevBundle.message("action.new.genius.dockerfile")) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // first, we need to guess language
        val dockerContexts = BuildSystemProvider.guess(project);
        val templateRender = TemplateRender("genius/sre")
        templateRender.context = DockerContext.from(dockerContexts)

        val template = templateRender
            .getTemplate("create-dockerfile.vm")

        val msgs = templateRender.create(template)

        val task: Task.Backgroundable = FileGenerateTask(project, msgs, DOCKERFILE)
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


