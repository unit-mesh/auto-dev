package cc.unitmesh.genius.actions

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.tasks.FileGenerateTask
import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.genius.actions.context.DevOpsContext
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.guessProjectDir
import kotlin.io.path.createDirectories


class GenerateGitHubActionsAction : AnAction(AutoDevBundle.message("action.new.genius.cicd.github")) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // first, we need to guess language
        val githubActions = BuildSystemProvider.guess(project);
        val templateRender = TemplateRender("genius/cicd")
        templateRender.context = DevOpsContext.from(githubActions)
        val template = templateRender.getTemplate("generate-github-action.vm")

        val dir = project.guessProjectDir()!!.toNioPath().resolve(".github").resolve("workflows")
            .createDirectories()

        val filename = dir.resolve("ci.yml").toFile()
        if (!filename.exists()) {
            filename.createNewFile()
        }

        val msgs = templateRender.buildMsgs(template)

        val task: Task.Backgroundable = FileGenerateTask(project, msgs, filename)
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }
}


