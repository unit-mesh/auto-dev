package cc.unitmesh.genius.actions

import cc.unitmesh.devti.AutoDevBundle
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

private const val DOCKERFILE = "Dockerfile"

class CreateGeniusDockerfileAction : AnAction(AutoDevBundle.message("action.new.genius.dockerfile")) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val root = project.guessProjectDir()!!
        val dockerfile = root.findFileByRelativePath(DOCKERFILE)
        if (dockerfile?.exists() == true) {
            // current Dockerfile to context
        }

        // first we need to guess language
        val contexts = BuildSystemProvider.guess(project);
        val result = ""

//        val stream =
//            LlmProviderFactory().connector(project).stream("TODO", "")
//        runBlocking {
//            stream.cancellable().collect {
//                result += it
//            }
//        }

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
