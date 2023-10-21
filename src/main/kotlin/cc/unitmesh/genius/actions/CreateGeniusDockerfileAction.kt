package cc.unitmesh.genius.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.llms.LlmProviderFactory
import cc.unitmesh.devti.provider.BuildSystemProvider
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
        val stream =
            LlmProviderFactory().connector(project).stream("TODO", "")

        var result = ""

        runBlocking {
            stream.cancellable().collect {
                result += it
            }
        }

        val root = project.guessProjectDir()!!
        val dockerfile = root.findFileByRelativePath(DOCKERFILE)
        if (dockerfile?.exists() == true) {
            // if Dockerfile exit, send to chat
        }

        // first we need to guess language
        val contexts = BuildSystemProvider.guess(project);

        val task: Task.Backgroundable = DockerFileGenerateTask(project, result)
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }
}


class DockerFileGenerateTask(@JvmField val project: Project, fileContent: String) :
    Task.Backgroundable(project, AutoDevBundle.message("intentions.request.background.process.title")) {
    val projectRoot = project.guessProjectDir()!!
    override fun run(indicator: ProgressIndicator) {
        WriteCommandAction.runWriteCommandAction(project, "Living Document", "cc.unitmesh.livingDoc", {
            // write Dockerfile to project root
        });
    }
}
